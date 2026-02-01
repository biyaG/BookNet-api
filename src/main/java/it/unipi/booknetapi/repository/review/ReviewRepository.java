package it.unipi.booknetapi.repository.review;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.BookShelfStatus;
import it.unipi.booknetapi.model.user.ReviewerRead;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.Source;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Repository
public class ReviewRepository implements ReviewRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(ReviewRepository.class);

    private final Integer batchSize;

    private final MongoClient mongoClient;
    private final MongoCollection<Review> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;


    public ReviewRepository(
            AppConfig appConfig,
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.batchSize = appConfig.getBatchSize() != null ? appConfig.getBatchSize() : 100;

        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("reviews", Review.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }


    /**
     * @param review the review to insert
     * @return the inserted review
     */
    @Override
    public Review insert(Review review) {
        Objects.requireNonNull(review);

        logger.debug("[REPOSITORY] [REVIEW] [INSERT] Inserting review: {}", review);

        if (review.getBookId() == null) {
            logger.warn("[REPOSITORY] [REVIEW] [INSERT] Review book id is null, cannot insert review: {}", review);
            return null;
        }

        if (review.getUser() == null || review.getUser().getId() == null) {
            logger.warn("[REPOSITORY] [REVIEW] [INSERT] Review user id is null, cannot insert review: {}", review);
            return null;
        }

        if (review.getRating() == null) {
            logger.warn("[REPOSITORY] [REVIEW] [INSERT] Review rating and comment is null, cannot insert review: {}", review);
            return null;
        }

        if(review.getDateAdded() == null || review.getDateAdded().after(new Date())) {
            review.setDateAdded(new Date());
        }

        try (ClientSession mongoSession = this.mongoClient.startSession()) {

            mongoSession.startTransaction();

            try {
                InsertOneResult insertOneResult = this.mongoCollection.insertOne(mongoSession, review);
                if(insertOneResult.getInsertedId() != null) {
                    saveReviewToNeo4j(review);

                    mongoSession.commitTransaction();

                    logger.info("[REPOSITORY] [REVIEW] [INSERT] Review inserted successfully: {}", review);
                    return review;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [REVIEW] [INSERT] Error during review insertion: {}", e.getMessage());
                return null;
            }

        }

        return null;
    }

    /**
     * Insert a review relationship (:Reader)-[RATER {rating, ts}]->(:Book)
     *
     * @param review the review to insert
     * */
    private void saveReviewToNeo4j(Review review) {
        Objects.requireNonNull(review);

        // Convert Date to ZonedDateTime for Neo4j
        ZonedDateTime timestamp = review.getDateAdded() != null
                ? review.getDateAdded().toInstant().atZone(ZoneId.systemDefault())
                : ZonedDateTime.now();

        this.registry.timer("neo4j.ops", "query", "save_review").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {

                session.executeWrite(tx -> {
                    String cypher = """
                    // Try to find the Reader AND the Book
                    MATCH (r:Reader {mid: $userId})
                    MATCH (b:Book {mid: $bookId})
                    
                    // Create or Update the RATED relationship
                    MERGE (r)-[rel:RATED]->(b)
                    
                    // Set properties
                    SET rel.rating = $rating,
                        rel.ts = $ts
                    """;

                    tx.run(cypher,
                            parameters(
                                    "userId", review.getUser().getId().toHexString(),
                                    "bookId", review.getBookId().toHexString(),
                                    "rating", review.getRating(),
                                    "ts", timestamp
                            )
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param reviews the reviews to insert
     * @return the inserted reviews
     */
    @Override
    public List<Review> insert(List<Review> reviews) {
        Objects.requireNonNull(reviews);

        reviews = reviews.stream()
                .filter(
                        r -> r.getBookId() != null
                                && r.getUser() != null && r.getUser().getId() != null
                                && (r.getRating() != null || r.getComment() != null)
                ).collect(Collectors.toList());

        if (reviews.isEmpty()) {
            return List.of();
        }

        logger.debug("[REPOSITORY] [REVIEW] [INSERT MANY] Inserting reviews: {}", reviews.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                InsertManyResult insertManyResult = this.mongoCollection.insertMany(mongoSession, reviews);

                insertManyResult.getInsertedIds();
                if(!insertManyResult.getInsertedIds().isEmpty()) {
                    saveReviewToNeo4j(reviews);

                    mongoSession.commitTransaction();

                    return reviews;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [REVIEW] [INSERT MANY] Error during review insertion: {}", e.getMessage());
            }
        }

        return List.of();
    }

    /**
     * @param reviews the reviews to insert
     * @return the inserted reviews
     */
    @Override
    public List<Review> insertFromGoodReads(List<Review> reviews) {
        Objects.requireNonNull(reviews);

        reviews = reviews.stream()
                .filter(
                        r -> r.getBookId() != null
                                && r.getUser() != null && r.getUser().getId() != null
                                && ((r.getRating() != null && r.getRating() > 0) || r.getComment() != null)
                ).collect(Collectors.toList());

        if (reviews.isEmpty()) {
            return List.of();
        }

        logger.debug("[REPOSITORY] [REVIEW] [INSERT MANY] [FROM GOODREADS] Inserting reviews: {}", reviews.size());

        List<Review> result = new ArrayList<>(reviews.size());

        for (int i=0; i<reviews.size(); i+=this.batchSize) {
            int endIndex = Math.min(i + batchSize, reviews.size());

            List<Review> reviewBatch = reviews.subList(i, endIndex);

            List<ObjectId> ids = bulkUpsetGoodReads(reviewBatch);

            List<Review> reviewsSaved = this.mongoCollection.find(Filters.in("_id", ids))
                    .into(new ArrayList<>());

            if(!reviewsSaved.isEmpty()) {
                saveReviewToNeo4j(reviewsSaved);

                result.addAll(reviewsSaved);
            }
        }

        return result;
    }

    private List<ObjectId> bulkUpsetGoodReads(List<Review> reviews) {
        logger.debug("[REPOSITORY] [REVIEW] [IMPORT] [MONGODB] Importing {} reviews from GoodReads", reviews.size());

        List<WriteModel<Review>> writes = new ArrayList<>();

        for(Review review : reviews) {
            String goodReadsId = review.getExternalId() != null ? review.getExternalId().getGoodReads() : null;

            if(goodReadsId != null) {
                writes.add(new UpdateOneModel<>(
                        Filters.eq("externalId.goodReads", goodReadsId),

                        Updates.combine(
                                Updates.set("bookId", review.getBookId()),
                                Updates.set("user", review.getUser()),

                                Updates.set("rating", review.getRating()),
                                Updates.set("comment", review.getComment()),
                                Updates.set("dateAdded", review.getDateAdded()),
                                Updates.set("dateUpdated", review.getDateUpdated()),

                                Updates.setOnInsert("source", Source.GOOD_READS),
                                Updates.setOnInsert("externalId", review.getExternalId())
                        ),

                        new UpdateOptions().upsert(true)
                ));
            }
        }

        if (!writes.isEmpty()) {
            BulkWriteResult result = this.mongoCollection
                    .bulkWrite(writes, new BulkWriteOptions().ordered(false));

            List<BulkWriteUpsert> inserts = result.getUpserts();

            List<ObjectId> objectIds = inserts.stream()
                    .map(insert -> insert.getId().asObjectId().getValue())
                    .toList();

            logger.debug("[REPOSITORY] [REVIEW] [IMPORT] [MONGODB] writes {} reviews", objectIds.size());

            return objectIds;
        } else {
            logger.debug("[REPOSITORY] [REVIEW] [IMPORT] [MONGODB] writes is empty");
        }

        return List.of();
    }

    private void saveReviewToNeo4j(List<Review> reviews) {
        Objects.requireNonNull(reviews);

        if (reviews.isEmpty()) {
            return;
        }

        logger.debug("[REPOSITORY] [REVIEW] [IMPORT] [NEO4J] Importing {} reviews from GoodReads", reviews.size());

        List<Map<String, Object>> batchData = reviews.stream()
                .filter(r -> r.getUser() != null && r.getUser().getId() != null && r.getBookId() != null)
                .filter(r -> r.getRating() != null)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", r.getUser().getId().toHexString());

                    map.put("userName", r.getUser().getName() != null ? r.getUser().getName() : "");

                    map.put("bookId", r.getBookId().toHexString());
                    map.put("rating", r.getRating());

                    if (r.getDateAdded() != null) {
                        map.put("ts", r.getDateAdded().toInstant().atZone(ZoneId.systemDefault()));
                    } else {
                        map.put("ts", ZonedDateTime.now());
                    }
                    return map;
                })
                .toList();

        if (batchData.isEmpty()) return;

        this.registry.timer("neo4j.ops", "query", "save_reviews_batch").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {

                session.executeWrite(tx -> {
                    String cypher = """
                        UNWIND $batch AS item
                        
                        // 2. Ensure Reader exists and update Name
                        MERGE (r:Reader {mid: item.userId})
                        ON CREATE SET r.name = item.userName
                        
                        // FIX: You must bridge the Write (SET) and Read (MATCH) with WITH
                        // We pass 'r' (the reader) and 'item' (data for book/rating) forward
                        WITH r, item
                        
                        // 3. Match the Book
                        MATCH (b:Book {mid: item.bookId})
                        
                        // 4. Create/Update Relationship
                        MERGE (r)-[rel:RATED]->(b)
                        SET rel.rating = item.rating,
                            rel.ts = item.ts
                        """;

                    tx.run(cypher, Map.of("batch", batchData));
                    return null;
                });
            }
        });
    }

    /**
     * @param reads
     */
    @Override
    public void importGoodReadsReviewsRead(List<ReviewerRead> reads) {

        if (reads == null || reads.isEmpty()) return;

        logger.debug("[REPOSITORY] [REVIEW] [INSERT RELATIONSHIPS] [FROM GOODREADS] Inserting reads: {}", reads.size());

        // Prepare Batch: Calculate Status and Timestamp in Java
        List<Map<String, Object>> batch = reads.stream()
                .filter(r -> r.getUserId() != null && r.getBookId() != null)
                .map(r -> {
                    // Logic: isRead null/false -> READING, true -> FINISHED
                    boolean isFinished = Boolean.TRUE.equals(r.getIsRead());
                    String status = isFinished ? BookShelfStatus.FINISHED.name() : BookShelfStatus.READING.name();

                    // Logic: Use readAt if finished, otherwise startedAt. Fallback to now.
                    Date date = isFinished ? r.getReadAt() : r.getStartedAt();
                    long ts = (date != null) ? date.getTime() : System.currentTimeMillis();

                    return Map.<String, Object>of(
                            "uid", r.getUserId().toHexString(),
                            "bid", r.getBookId().toHexString(),
                            "status", status,
                            "ts", ts
                    );
                })
                .toList();

        String cypher = """
            UNWIND $batch AS row
            
            // Ensure Nodes Exist (Shell Node Pattern)
            MERGE (r:Reader {mid: row.uid})
            MERGE (b:Book {mid: row.bid})
            
            // Create/Update Relationship
            MERGE (r)-[rel:ADDED_TO_SHELF]->(b)
            SET
                rel.status = row.status,
                rel.ts = row.ts
            """;

        for (int i=0; i<batch.size(); i+=this.batchSize) {
            int endIndex = Math.min(i + batchSize, batch.size());

            List<Map<String, Object>> batchData = batch.subList(i, endIndex);

            this.registry.timer("neo4j.ops", "query", "import_reads").record(() -> {
                try (Session session = this.neo4jManager.getDriver().session()) {
                    session.executeWrite(tx -> {
                        tx.run(cypher, Values.parameters("batch", batchData));
                        return null;
                    });
                }
            });
        }

    }

    /**
     * @param idReview review's id
     * @param rating rating to set
     * @param comment comment to set
     * @return true if the review was updated successfully, false otherwise
     */
    @Override
    public boolean updateReview(String idReview, Integer rating, String comment) {
        Objects.requireNonNull(idReview);

        Optional<Review> optReview = this.findById(idReview);

        if(optReview.isPresent()) {
            Review review = optReview.get();

            try (ClientSession mongoSession = this.mongoClient.startSession()) {
                mongoSession.startTransaction();

                try {
                    List<Bson> updateOperations = new ArrayList<>();
                    if (rating != null) {
                        updateOperations.add(Updates.set("rating", rating));
                    }
                    if (comment != null) {
                        updateOperations.add(Updates.set("comment", comment));
                    }
                    updateOperations.add(Updates.set("dateUpdated", new Date()));

                    UpdateResult updateResult = this.mongoCollection
                            .updateOne(
                                    mongoSession,
                                    Filters.eq("_id", new ObjectId(idReview)),
                                    Updates.combine(updateOperations)
                            );

                    if(updateResult.getModifiedCount() > 0) {
                        if(rating != null) {
                            updateReviewInNeo4j(review.getUser().getId().toHexString(), review.getBookId().toHexString(), rating);
                        }

                        mongoSession.commitTransaction();
                        return true;
                    }
                } catch (Exception e) {
                    mongoSession.abortTransaction();
                }
            }
        }

        return false;
    }

    private void updateReviewInNeo4j(String idReader, String idBook, Integer rating) {
        Objects.requireNonNull(idReader);
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(rating);

        // Cypher query explanation:
        // 1. MATCH finds the existing User and Book nodes (prevents creating empty nodes if IDs are wrong).
        // 2. MERGE creates the relationship 'REVIEWED' if it's missing, or matches it if it exists.
        // 3. SET updates the rating property regardless of whether the relationship was just created or already existed.
        String cypher = """
            MATCH (r:Reader {mid: $idReader})
            MATCH (b:Book {mid: $idBook})
            MERGE (r)-[rel:RATED]->(b)
            SET rel.rating = $rating
            """;

        this.registry.timer("neo4j.ops", "query", "update_review").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    parameters(
                                            "idReader", idReader,
                                            "idBook", idBook,
                                            "rating", rating
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idReview review's id
     */
    @Override
    public boolean delete(String idReview) {
        Objects.requireNonNull(idReview);

        Optional<Review> optReview = this.findById(idReview);

        logger.debug("Deleting review: {}", idReview);

        if(optReview.isPresent()) {
            Review review = optReview.get();

            try (ClientSession mongoSession = this.mongoClient.startSession()) {
                mongoSession.startTransaction();

                try {
                    DeleteResult deleteResult = this.mongoCollection.deleteOne(mongoSession, Filters.eq("_id", new ObjectId(idReview)));

                    if(deleteResult.getDeletedCount() > 0) {
                        deleteReviewInNeo4j(review.getUser().getId().toHexString(), review.getBookId().toHexString());

                        mongoSession.commitTransaction();
                        return true;
                    } else {
                        mongoSession.abortTransaction();
                    }
                } catch (Exception e) {
                    mongoSession.abortTransaction();
                    logger.error("Error during review deletion: {}", e.getMessage());
                }
            }
        }

        return false;
    }

    private void deleteReviewInNeo4j(String idReader, String idBook) {
        Objects.requireNonNull(idReader);
        Objects.requireNonNull(idBook);

        // Cypher query explanation:
        // 1. MATCH finds the path from the specific Reader to the specific Book.
        // 2. DELETE rel removes ONLY the relationship. The Reader and Book nodes remain.
        String query = """
        MATCH (:Reader {mid: $idReader})-[rel:RATED]->(:Book {mid: $idBook})
        DELETE rel
        """;

        this.registry.timer("neo4j.ops", "query", "update_review").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    query,
                                    parameters(
                                            "idReader", idReader,
                                            "idBook", idBook
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idReviews review ids to delete
     * @return true if all reviews were deleted successfully, false otherwise
     */
    @Override
    public boolean delete(List<String> idReviews) {
        Objects.requireNonNull(idReviews);

        List<Review> reviews = this.findAll(idReviews);

        if(reviews.isEmpty()) return true;

        logger.debug("Deleting review: {}", reviews.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                List<Map<String, String>> idReadersBooks = reviews.stream()
                        .map(r -> Map.of("idReader", r.getUser().getId().toHexString(), "idBook", r.getBookId().toHexString()))
                        .toList();

                List<ObjectId> idReviewsObjIds = reviews.stream().map(Review::getId).toList();

                DeleteResult deleteResult = this.mongoCollection.deleteMany(mongoSession, Filters.in("_id", idReviewsObjIds));

                if(deleteResult.getDeletedCount() == idReviews.size()) {
                    deleteReviewsInNeo4j(idReadersBooks);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during review deletion: {}", e.getMessage());
            }
        }

        return false;
    }

    private void deleteReviewsInNeo4j(List<Map<String, String>> idReadersBooks) {
        // Map => (idReader, id reader), (idBook, id book)

        // Cypher query explanation:
        // 1. UNWIND iterates through the list provided in the $batch parameter.
        // 2. MATCH finds the specific relationship for the current row (pair of IDs).
        // 3. DELETE removes that specific relationship.
        String cypher = """
        UNWIND $batch AS row
        MATCH (:Reader {id: row.idReader})-[rel:REVIEWED]->(:Book {id: row.idBook})
        DELETE rel
        """;

        try (Session session = this.neo4jManager.getDriver().session()) {
            session.executeWrite(
                    tx -> {
                        tx.run(
                                cypher,
                                parameters("batch", idReadersBooks)
                        );
                        return null;
                    }
            );
        }
    }

    /**
     * @param idReview review id
     * @return review
     */
    @Override
    public Optional<Review> findById(String idReview) {
        Objects.requireNonNull(idReview);

        logger.debug("Find review: {}", idReview);

        Review review = this.mongoCollection.find(
                Filters.eq("_id", new ObjectId(idReview))
        ).first();

        return review != null ? Optional.of(review) : Optional.empty();
    }

    /**
     * @param idReviews review ids
     * @return list of reviews
     */
    @Override
    public List<Review> findAll(List<String> idReviews) {
        Objects.requireNonNull(idReviews);

        List<ObjectId> ids = idReviews.stream()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        if(ids.isEmpty()) return List.of();

        logger.debug("Find reviews: {}", ids);

        return this.mongoCollection
                .find(Filters.in("_id", ids))
                .into(new ArrayList<>());
    }

    /**
     * @param idBook book id
     * @param page page number
     * @param size size of the page
     * @return list of reviews
     */
    @Override
    public PageResult<Review> findByBook(String idBook, int page, int size) {
        Objects.requireNonNull(idBook);

        int skip = page * size;

        List<Review> reviews = this.mongoCollection
                .find(Filters.eq("bookId", new ObjectId(idBook)))
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(Filters.eq("bookId", new ObjectId(idBook)));

        return new PageResult<>(reviews, total, page, size);
    }

    /**
     * @param idReader reader id
     * @param page page number
     * @param size size of the page
     * @return list of reviews
     */
    @Override
    public PageResult<Review> findByReader(String idReader, int page, int size) {
        Objects.requireNonNull(idReader);

        int skip = page * size;

        List<Review> reviews = this.mongoCollection
                .find(Filters.eq("user.id", new ObjectId(idReader)))
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(Filters.eq("user.id", new ObjectId(idReader)));

        return new PageResult<>(reviews, total, page, size);
    }

    /**
     * @param page number of the page
     * @param size size of the page
     * @return list of reviews
     */
    @Override
    public PageResult<Review> findAll(int page, int size) {
        int skip = page * size;

        List<Review> reviews = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments();

        return new PageResult<>(reviews, total, page, size);
    }

    /**
     *
     */
    @Override
    public void migrate() {
        logger.debug("[REPOSITORY] [REVIEW] [MIGRATE]");

        /*long total = this.mongoCollection
                .countDocuments();

        int totalPages = (int) Math.ceil((double) total / this.batchSize);

        for(int page = 0; page < totalPages; page++) {
            int skip = page * this.batchSize;
            List<Review> reviews = this.mongoCollection
                    .find()
                    .skip(skip)
                    .limit(this.batchSize)
                    .into(new ArrayList<>());

            saveReviewToNeo4j(reviews);
        }*/


        int offset = 0;

        // Start with a "zero" ObjectId (minimum value)
        ObjectId lastId = new ObjectId("000000000000000000000000");

        while (true) {
            logger.debug("[REPOSITORY] [REVIEW] [MIGRATE] [MONGODB TO NEO4J] Migrated batch: {} to {}", offset, offset + batchSize);

            // Find the next batch of books where _id > lastId
            List<Review> reviews = this.mongoCollection
                    .find(Filters.gt("_id", lastId)) // Use Index instead of Scan
                    .sort(Sorts.ascending("_id"))    // Ensure order
                    .limit(this.batchSize)
                    .into(new ArrayList<>());

            if (reviews.isEmpty()) break; // Finished

            // Update the cursor to the last processed ID
            lastId = reviews.getLast().getId();

            // Process migration
            saveReviewToNeo4j(reviews);

            offset += this.batchSize;
        }

    }
}
