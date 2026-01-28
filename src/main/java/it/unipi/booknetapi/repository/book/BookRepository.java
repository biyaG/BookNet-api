package it.unipi.booknetapi.repository.book;


import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.ReviewSummary;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.ExternalId;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.utils.LanguageUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class BookRepository implements BookRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(BookRepository.class);

    private final Integer batchSize;

    private final MongoCollection<Book> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final CacheService cacheService;
    private final MeterRegistry registry;
    private final MongoClient mongoClient;

    private static final String CACHE_PREFIX = "book:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public BookRepository(
            AppConfig appConfig,
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            CacheService cacheService,
            MeterRegistry registry
    ) {
        this.batchSize = appConfig.getBatchSize() != null ? appConfig.getBatchSize() : 50;
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("books", Book.class);
        this.neo4jManager = neo4jManager;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    private boolean handleUpdateResult(UpdateResult result, String idBook) {
        if (result.getModifiedCount() > 0) {
            deleteCache(idBook);
            return true;
        }
        return false;
    }

    private static String generateCacheKey(String idBook) {
        return CACHE_PREFIX + idBook;
    }

    private void cacheBook(Book book) {
        this.cacheService.save(generateCacheKey(book.getId().toHexString()), book, CACHE_TTL);
    }

    private void cacheBook(List<Book> books) {
        books.forEach(this::cacheBook);
    }

    private void cacheBookInThread(List<Book> books) {
        Thread thread = new Thread(() -> cacheBook(books));
        thread.start();
    }

    private void deleteCache(String idBook) {
        this.cacheService.delete(generateCacheKey(idBook));
    }

    private void deleteCache(List<String> idBooks) {
        idBooks.forEach(this::deleteCache);
    }

    private void deleteCacheInThread(List<ObjectId> idBooks) {
        Thread thread = new Thread(() -> idBooks.forEach(idBook -> deleteCache(idBook.toHexString())));
        thread.start();
    }

    @Override
    public Book save(Book book) {
        Objects.requireNonNull(book);
        logger.debug("Saving book: {}", book);

        if(book.getRatingReview() == null) book.setRatingReview(new ReviewSummary(0f, 0));

        try(com.mongodb.client.ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();

            try{
                boolean success;
                if(book.getId() == null){
                    InsertOneResult insertOneResult = this.mongoCollection.insertOne(book); //Upsert Logic (Update or Insert):
                    success = insertOneResult.wasAcknowledged();
                    if(success){
                        book.setId(Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId().getValue());
                    }
                } else{
                    UpdateResult updateResult = this.mongoCollection.replaceOne(
                            Filters.eq("_id", book.getId()), book
                    );
                    success = updateResult.getModifiedCount() > 0;
                }
                if(success){
                    saveBookToNeo4j(book);
                    session.commitTransaction();

                    this.cacheBook(book);
                    logger.info("Book inserted successfully: {}", book);
                    return book;
                }
            } catch(Exception e){
                session.abortTransaction();
                logger.error("Error during inserting book in neo4j: {}",e.getMessage());
                return null;
            }
        }
        return null;
    }

    public Book insertWithThread(Book book) {
        Objects.requireNonNull(book);

        InsertOneResult insertOneResult = this.mongoCollection.insertOne(book);
        if(insertOneResult.wasAcknowledged()){
            Thread thread = new Thread(() -> saveBookToNeo4j(book));
            thread.start();
        }
        return insertOneResult.wasAcknowledged() ? book : null;
    }

    private void saveBookToNeo4j(Book book) {
        Objects.requireNonNull(book);

        this.registry.timer("neo4j.ops", "query", "save_book").record(() -> {
            try(Session session = this.neo4jManager.getDriver().session()){
                String cypher = "CREATE (b:Book {mid: $bookId, title : $title, ratingAvg: $ratingAvg})";
                session.executeWrite(tx -> {
                    tx.run(
                            cypher,
                            Values.parameters(
                                    "bookId", book.getId().toHexString(),
                                    "title", book.getTitle(),
                                    "ratingAvg", book.getRatingReview()//getRatingReview actually gives you the document so think about this latter
                            )
                    );
                    return null;
                });
            }
        });
    }


    @Override
    public List<Book> saveAll(List<Book> books){
        Objects.requireNonNull(books);

        if(books.isEmpty()) return List.of();

        logger.debug("Inserting many books: {}", books.size());

        try(ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                this.mongoCollection.insertMany(session, books);
                saveBooksToNeo4j(books);
                session.commitTransaction();
                this.cacheBookInThread(books);

                logger.info("Books saved successfully : {}",books.size());
                return books;
            }catch (Exception e){
                session.abortTransaction();
                logger.error("Error occurred while inserting in Neo4j: {}",e.getMessage());
            }
        }
        return List.of();
    }

    @Override
    public List<Book> importBooks(List<BookGoodReads> importedBooks) {
        Objects.requireNonNull(importedBooks);


        logger.debug("[REPOSITORY] [BOOK] [IMPORT] Importing {} books from GoodReads", importedBooks.size());

        List<Book> result = new ArrayList<>(importedBooks.size());

        for(int i=0; i < importedBooks.size(); i += this.batchSize) {
            int endIndex = Math.min(i + this.batchSize, importedBooks.size());

            List<BookGoodReads> booksBatch = importedBooks.subList(i, endIndex);

            List<ObjectId> ids = bulkUpSet(booksBatch);

            List<Map<String, Object>> neo4jBatch = new ArrayList<>();

            List<Book> books = this.mongoCollection.find(Filters.in("_id", ids))
                    .projection(Projections.include("_id", "title", "isbn", "isbn13", "externalId"))
                    .into(new ArrayList<>());

            books.forEach(book -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", book.getId().toHexString());
                map.put("title", book.getTitle());
                map.put("isbn", book.getIsbn());
                map.put("isbn13", book.getIsbn13());
                neo4jBatch.add(map);
            });

            if (!neo4jBatch.isEmpty()) {
                bulkUpdateBooksInNeo4j(neo4jBatch);
            }

            result.addAll(books);
        }

        return result;
    }

    @Override
    public void importBooksAuthors(Map<ObjectId, List<AuthorEmbed>> bookAuthors) {
        Objects.requireNonNull(bookAuthors);

        if (bookAuthors.isEmpty()) return;

        logger.debug("[IMPORT] [BOOK-AUTHORS] Processing {} books", bookAuthors.size());

        // Convert Map entries to a List to enable indexed batching
        List<Map.Entry<ObjectId, List<AuthorEmbed>>> entries = new ArrayList<>(bookAuthors.entrySet());

        for (int i = 0; i < entries.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entries.size());
            List<Map.Entry<ObjectId, List<AuthorEmbed>>> batch = entries.subList(i, endIndex);

            List<WriteModel<Book>> mongoBulkOps = new ArrayList<>();

            Map<String, List<String>> neo4jMap = new HashMap<>();

            for (Map.Entry<ObjectId, List<AuthorEmbed>> entry : batch) {
                ObjectId bookId = entry.getKey();
                List<AuthorEmbed> authors = entry.getValue();

                mongoBulkOps.add(new UpdateOneModel<>(
                        Filters.eq("_id", bookId),
                        Updates.set("authors", authors)
                ));

                List<String> authorIds = authors.stream()
                        .map(a -> a.getId().toHexString())
                        .toList();

                neo4jMap.put(bookId.toHexString(), authorIds);
            }

            if (!mongoBulkOps.isEmpty()) {
                this.mongoCollection.bulkWrite(mongoBulkOps, new BulkWriteOptions().ordered(false));
            }

            if (!neo4jMap.isEmpty()) {
                importBooksAuthorsInNeo4j(neo4jMap);
            }
        }

    }

    private void importBooksAuthorsInNeo4j(Map<String, List<String>> bookAuthors) {
        if (bookAuthors.isEmpty()) return;

        List<Map<String, Object>> neo4jBatch = bookAuthors.entrySet().stream()
                .map(entry -> Map.of(
                        "bid", entry.getKey(),
                        "aids", entry.getValue().stream().sorted().toList()
                ))
                .toList();

        // Logic:
        // A. Match the Book (assumed to exist).
        // B. Delete OLD relationships (Pruning).
        // C. Create NEW relationships for every author in the list.
        //    We MERGE the Author node to ensure it exists (creating a shell node if missing).
        String query = """
                UNWIND $batch AS row
                MATCH (b:Book {mid: row.bid})

                // Remove obsolete relationships
                // Find existing authors connected to this book
                OPTIONAL MATCH (b)-[r:WRITTEN_BY]->(oldA:Author)
                // Only delete if the author ID is NOT in our new list
                WHERE NOT oldA.mid IN row.aids
                DELETE r

                // Add new relationships
                WITH b, row
                UNWIND row.aids AS authorId
                MERGE (a:Author {mid: authorId})
                // MERGE handles the check: it only creates the rel if it doesn't exist
                MERGE (b)-[:WRITTEN_BY]->(a)
            """;

        try (Session session = this.neo4jManager.getDriver().session()) {
            session.executeWrite(tx -> {
                tx.run(query, Values.parameters("batch", neo4jBatch));
                return null;
            });
        }

    }

    private List<ObjectId> bulkUpSet(List<BookGoodReads> booksGoodReads) {
        logger.debug("[REPOSITORY] [BOOK] [IMPORT] [MONGODB] Importing {} books from GoodReads", booksGoodReads.size());

        List<WriteModel<Book>> writes = new ArrayList<>();

        for(BookGoodReads bookGoodRead : booksGoodReads){
            String goodReadsId = bookGoodRead.getBookId();
            ExternalId externalId = ExternalId.builder().goodReads(goodReadsId).build();

            List<Bson> updates = new ArrayList<>();
            updates.add(Updates.set("externalId.goodReads", bookGoodRead.getBookId()));
            // updates.add(Updates.setOnInsert("externalId", externalId));

            updates.add(Updates.setOnInsert("isbn13", bookGoodRead.getIsbn13()));
            updates.add(Updates.setOnInsert("title", bookGoodRead.getTitle()));
            updates.add(Updates.setOnInsert("subtitle", bookGoodRead.getTitleWithoutSeries()));
            updates.add(Updates.setOnInsert("description", bookGoodRead.getDescription()));

            parseAndSet(updates, "publicationYear", bookGoodRead.getPublicationYear());
            parseAndSet(updates, "publicationMonth", bookGoodRead.getPublicationMonth());
            parseAndSet(updates, "publicationDay", bookGoodRead.getPublicationDay());

            if(bookGoodRead.getCountryCode() != null && !bookGoodRead.getCountryCode().isBlank()) {
                Optional<String> optS = LanguageUtils.getLanguageFromCountry(bookGoodRead.getCountryCode());
                updates.add(
                        Updates.addToSet("languages", optS.orElse(bookGoodRead.getCountryCode()))
                );
            }
            if(bookGoodRead.getLanguageCode() != null && !bookGoodRead.getLanguageCode().isBlank()) {
                updates.add(
                        Updates.addToSet("languages", bookGoodRead.getLanguageCode())
                );
                if(bookGoodRead.getLanguageCode().length() > 2) {
                    updates.add(
                            Updates.addToSet("languages", bookGoodRead.getLanguageCode().substring(0, 2))
                    );
                }
            }

            addIfPresent(updates, "images", bookGoodRead.getImageUrl());
            addIfPresent(updates, "previews", bookGoodRead.getUrl());
            addIfPresent(updates, "publishers", bookGoodRead.getPublisher());

            float rating = parseSafeFloat(bookGoodRead.getAverageRating());
            int count = parseSafeInt(bookGoodRead.getRatingCount());
            updates.add(Updates.setOnInsert("ratingReview.rating", rating));
            updates.add(Updates.setOnInsert("ratingReview.count", count));

            writes.add(new UpdateOneModel<>(
                    Filters.eq("isbn", bookGoodRead.getIsbn()),

                    Updates.combine(updates),

                    new UpdateOptions().upsert(true)
            ));
        }

        if (!writes.isEmpty()) {
            BulkWriteResult result = this.mongoCollection
                    .bulkWrite(writes, new BulkWriteOptions().ordered(false));

            List<ObjectId> objectIds = result.getUpserts()
                    .stream()
                    .map(upsert -> upsert.getId().asObjectId().getValue())
                    .toList();
            logger.debug("[REPOSITORY] [Book] [IMPORT] [MONGODB] writes {} books", objectIds.size());

            return objectIds;
        } else {
            logger.debug("[REPOSITORY] [Book] [IMPORT] [MONGODB] writes is empty");
        }

        return List.of();
    }

    private void parseAndSet(List<Bson> updates, String field, String value) {
        if (value != null && !value.isBlank()) {
            try {
                updates.add(Updates.set(field, Integer.parseInt(value)));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void addIfPresent(List<Bson> updates, String field, String value) {
        if (value != null && !value.isBlank()) {
            updates.add(Updates.addToSet(field, value));
        }
    }

    private float parseSafeFloat(String val) {
        if (val == null) return 0f;
        try { return Float.parseFloat(val); } catch (NumberFormatException e) { return 0f; }
    }

    private int parseSafeInt(String val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    private void bulkUpdateBooksInNeo4j(List<Map<String, Object>> booksBatch) {
        if (booksBatch.isEmpty()) return;

        logger.debug("[REPOSITORY] [BOOK] [IMPORT] [NEO4J] Importing {} books", booksBatch.size());

        // Cypher Logic:
        // 1. UNWIND: Turns the list of maps into individual rows
        // 2. MERGE: Finds a Book by its MongoDB ID ('mid'). Creates it if missing.
        // 3. SET: Updates title and ISBNs.
        //    Note: If 'row.isbn' is null, the property 'b.isbn' will be removed/set to null in Neo4j.
        String query = """
        UNWIND $batch AS row
        MERGE (b:Book {mid: row.id})
        SET
            b.title = row.title,
            b.isbn = row.isbn,
            b.isbn13 = row.isbn13
        """;

        try (Session session = this.neo4jManager.getDriver().session()) {
            session.executeWrite(tx -> {
                tx.run(query, Values.parameters("batch", booksBatch));
                return null;
            });
        }
    }

    private void saveBooksToNeo4j(List<Book> books){
        List<Map<String, Object>> neo4jBatch = new ArrayList<>();
        for(Book book : books){
            neo4jBatch.add(Map.of("bookId", book.getId().toHexString(),"title", book.getTitle(), "ratingAvg", book.getRatingReview()));
        }

        if(!neo4jBatch.isEmpty()){
            this.registry.timer("neo4j.ops", "query", "save_reader").record(() -> {
                try(Session session = this.neo4jManager.getDriver().session()){
                    session.executeWrite(
                            tx -> {
                                tx.run(
                                        "UNWIND $books as book CREATE (b:Book {mid:book.bookId, title: book.title, ratingAvg: book.ratingAvg})",
                                        Values.parameters("books", neo4jBatch)
                                );
                                return null;
                            }
                    );
                }
            });
        }

    }

    private Book BookWrittenByAuthorRelationship(Book book){
        Objects.requireNonNull(book);

        this.registry.timer("neo4j.ops", "query", "BookWrittenByAuthorRelationship").record(() -> {
//            String cypher = ""
        });

        return Objects.requireNonNull(book); ///
    }

    @Override
    public boolean deleteBook(String idBook) {
        Objects.requireNonNull(idBook);

        try(ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                DeleteResult deleteResult = this.mongoCollection.deleteOne(Filters.eq("_id", new ObjectId(idBook)));
                if(deleteResult.getDeletedCount() > 0){
                    deleteBookFromNeo4J(idBook);
                    session.commitTransaction();
                    this.deleteCache(idBook);
                    return true;
                }
            }catch(Exception e){
                session.abortTransaction();
            }
        }
        return false;
    }

    private void deleteBookFromNeo4J(String idBook) {
        Objects.requireNonNull(idBook);

        this.registry.timer("neo4j.ops", "query", "delete_book").record(()->{
            try(Session session = this.neo4jManager.getDriver().session()){
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH(b:Book {mid: $bookId}) DETACH DELETE r", //CHANGE
                                    Values.parameters("bookId", idBook)
                            );
                            return null;
                        }
                );
            }
        });
    }

    @Override
    public boolean addReview(Review review) {
        Objects.requireNonNull(review);
        Objects.requireNonNull(review.getBookId());
        Objects.requireNonNull(review.getUser());
        Objects.requireNonNull(review.getUser().getId());

        Book book = this.mongoCollection.find(Filters.eq("_id", review.getBookId())).first();
        if(book == null) return false;
        ReviewSummary oldSummary = book.getRatingReview();

        int newCount = oldSummary.getCount() + 1;
        float newAvg = ((oldSummary.getRating() * oldSummary.getCount()) + review.getRating()) / newCount;

        ReviewSummary updatedSummary = new ReviewSummary(newAvg, newCount);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", review.getBookId()),
                Updates.combine(
                        Updates.set("ratingReview", updatedSummary),
                        Updates.push("reviews", review.getBookId())
                )
        );

        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean removeReview(String idBook, String idUser, String idReview) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idReview);

       UpdateResult updateResult = this.mongoCollection.updateOne(
               Filters.eq("_id", new ObjectId(idBook)),
               Updates.pull("reviews", Filters.eq("_id", new ObjectId(idReview)))
        );

        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateImage(String idBook, String newImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("images", newImageUrl)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean updatePreview(String idBook, String newPreviewImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newPreviewImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("preview", newPreviewImageUrl)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean deletePreview(String idBook, String deletePreviewImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(deletePreviewImageUrl);

        UpdateResult updateResult = mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.pull("preview", deletePreviewImageUrl)
        );

        return handleUpdateResult(updateResult, idBook);
    }

    /*@Override
    public boolean updateSimilarBooks(String idBook, BookEmbed book) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(book);

        if(!ObjectId.isValid(idBook)) return false;

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.set("similar_books", book)
        );
        return handleUpdateResult(updateResult, idBook);
    }*/

    @Override
    public boolean updateSimilarBooks(String idBook, List <BookEmbed>  books) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(books);

        if(!ObjectId.isValid(idBook)) return false;
        logger.debug("[REPOSITORY] [BOOK] [SET BOOKS] book: {}, books size: {}", idBook, books.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                UpdateResult result = this.mongoCollection
                        .updateOne(
                                Filters.eq("_id", new ObjectId(idBook)),
                                Updates.set("similar_books", books)
                        );

                if (result.getModifiedCount() > 0) {
                    updateSimilarBooksInNeo4J(idBook, books);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [BOOK] [SET BOOKS] Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    private void updateSimilarBooksInNeo4J(String idBook, List<BookEmbed> books) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(books);

        if(!ObjectId.isValid(idBook)) return ;

        List<Map<String, String>> bookListParams = books.stream()
                .map(g -> Map.of(
                        "id", g.getId().toHexString(),
                        "title", g.getTitle()
                ))
                .toList();

        String cypher = """
            MATCH (b:Book {mid: $bookId})
            
            // 1. Prune: Identify which IDs we want to keep
            WITH b, [s IN $books | s.id] AS targetIds
            
            // Delete relationships to books NOT in the target list
            OPTIONAL MATCH (b)-[r:SIMILAR_TO]->(other:Book)
            WHERE NOT other.mid IN targetIds
            DELETE r
            
            // 2. Merge: Add new relationships
            // We use WITH b to pass the node after the DELETE operation
            WITH b
            UNWIND $books AS sData
                MERGE (s:Book {mid: sData.id})
                ON CREATE SET s.title = sData.title
                MERGE (b)-[:SIMILAR_TO]->(s)
            """;

        this.registry.timer("neo4j.ops", "query", "similar_books").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    Values.parameters(
                                            "bookId", idBook,
                                            "books", bookListParams
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param mapBooks
     * @return
     */
    @Override
    public boolean updateSimilarBooks(Map<String, List<BookEmbed>> mapBooks) {
        if (mapBooks == null || mapBooks.isEmpty()) return false;

        logger.debug("[REPOSITORY] [BOOK] [SET BOOKS] Processing {} books", mapBooks.size());

        // Convert Map to List for indexed batching
        List<Map.Entry<String, List<BookEmbed>>> entries = new ArrayList<>(mapBooks.entrySet());
        boolean allBatchesSuccess = true;

        // Process in Batches
        for (int i = 0; i < entries.size(); i += this.batchSize) {
            int end = Math.min(i + this.batchSize, entries.size());
            List<Map.Entry<String, List<BookEmbed>>> batchEntries = entries.subList(i, end);

            // Convert batch back to a Map or List for processing
            Map<String, List<BookEmbed>> currentBatch = new HashMap<>();
            for (Map.Entry<String, List<BookEmbed>> entry : batchEntries) {
                currentBatch.put(entry.getKey(), entry.getValue());
            }

            // Process this specific batch in its own transaction context
            boolean batchSuccess = bulkUpSet(currentBatch);
            if (!batchSuccess) {
                allBatchesSuccess = false;
                // Optional: break; if you want to stop immediately on first error
            }
        }

        return allBatchesSuccess;
    }

    private boolean bulkUpSet(Map<String, List<BookEmbed>> batch) {
        logger.debug("[REPOSITORY] [BOOK] [IMPORT] [BOOK SIMILARITY] Importing {} books from GoodReads", batch.size());


        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                List<WriteModel<Book>> writes = new ArrayList<>();

                for (Map.Entry<String, List<BookEmbed>> entry : batch.entrySet()) {
                    String bookId = entry.getKey();
                    if (ObjectId.isValid(bookId)) {
                        writes.add(new UpdateOneModel<>(
                                Filters.eq("_id", new ObjectId(bookId)),
                                Updates.set("similar_books", entry.getValue())
                        ));
                    }
                }

                if (writes.isEmpty()) return true;

                BulkWriteResult result = this.mongoCollection.bulkWrite(mongoSession, writes);

                if (result.getMatchedCount() > 0) {
                    bulkUpdateSimilarBooksInNeo4J(batch);

                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                    return false;
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [BOOK] [SET BOOKS] Error during batch transaction: {}", e.getMessage());
                return false;
            }
        }
    }

    private void bulkUpdateSimilarBooksInNeo4J(Map<String, List<BookEmbed>> mapBooks) {
        if (mapBooks.isEmpty()) return;

        List<Map<String, Object>> neo4jBatch = mapBooks.entrySet().stream()
                .map(entry -> {
                    List<Map<String, String>> simList = entry.getValue().stream()
                            .map(g -> Map.of(
                                    "id", g.getId().toHexString(),
                                    "title", g.getTitle()
                            ))
                            .toList();

                    return Map.<String, Object>of(
                            "bid", entry.getKey(),
                            "sim", simList
                    );
                })
                .toList();

        String cypher = """
            UNWIND $batch AS row
            MATCH (b:Book {mid: row.bid})
            WITH b, row, [s IN row.sim | s.id] AS targetIds
            
            OPTIONAL MATCH (b)-[r:SIMILAR_TO]->(other:Book)
            WHERE NOT other.mid IN targetIds
            DELETE r
            
            WITH b, row
            UNWIND row.sim AS sData
                MERGE (s:Book {mid: sData.id})
                ON CREATE SET s.title = sData.title
                MERGE (b)-[:SIMILAR_TO]->(s)
            """;

        this.registry.timer("neo4j.ops", "query", "bulk_similar_books").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    tx.run(cypher, Values.parameters("batch", neo4jBatch));
                    return null;
                });
            }
        });
    }

    @Override
    public boolean updateGenres(String idBook, List<GenreEmbed> genres) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genres);

        if(!ObjectId.isValid(idBook)) return false;

        logger.debug("[REPOSITORY] [BOOK] [SET GENRES] book: {}, genre size: {}", idBook, genres.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                UpdateResult result = this.mongoCollection
                        .updateOne(
                                Filters.eq("_id", new ObjectId(idBook)),
                                Updates.set("genres", genres)
                        );

                if(result.getModifiedCount() > 0) {
                    updateGenresInNeo4J(idBook, genres);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] BOOK] [SET GENRES] Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    private void updateGenresInNeo4J(String idBook, List<GenreEmbed> genres) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genres);

        if(!ObjectId.isValid(idBook)) return ;

        List<Map<String, String>> genreListParams = genres.stream()
                .map(g -> Map.of(
                        "id", g.getId().toHexString(),
                        "name", g.getName()
                ))
                .toList();

        String cypher = """
            MATCH (b:Book {mid: $bookId})
            
            WITH b
            OPTIONAL MATCH (b)-[r:IN_GENRE]->(:Genre)
            DELETE r
            
            WITH b
            UNWIND $genres AS gData
            MERGE (g:Genre {mid: gData.id})
            ON CREATE SET g.name = gData.name
            MERGE (b)-[:IN_GENRE]->(g)
            """;

        this.registry.timer("neo4j.ops", "query", "update_book").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    Values.parameters(
                                            "bookId", idBook,
                                            "genres", genres
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }


    @Override
    public boolean addGenre(String idBook, GenreEmbed genre) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genre);

        if(!ObjectId.isValid(idBook)) return false;

        logger.debug("[REPOSITORY] [BOOK] [ADD GENRE] author: {}, book: {}", idBook, genre.getId());

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("genres", genre)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean removeGenre(String idBook, GenreEmbed genre) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genre);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.pull("_id ", Filters.eq("_id", genre.getId()))
        );

        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean deleteAllBooks(List<ObjectId> idBooks){
        Objects.requireNonNull(idBooks);

        try (ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                DeleteResult deleteResult = this.mongoCollection.deleteMany(
                        Filters.in("_id",idBooks) //Because ID books is an objectId

                        // Filters.in("_id",idBooks.stream().map(ObjectId::new).toList())

                );
                if(deleteResult.getDeletedCount() > 0){
                    deleteBookBatchFromNeo4j(idBooks);
                    session.commitTransaction();
                    this.deleteCacheInThread(idBooks);
                    return true;
                }
            }catch(Exception e){
                session.abortTransaction();
                return false;
            }
        }
        return false;
    }

    private void deleteBookBatchFromNeo4j(List<ObjectId> idBooks){
        this.registry.timer("neo4j.ops", "query", "delete_books").record(() -> {
            try(Session session = this.neo4jManager.getDriver().session()){
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (b: Book) WHERE b.mid IN $bookIds DETACH DELETE b", /// why bookIds
                                    Values.parameters("bookIds",idBooks.stream().map(ObjectId::toHexString).toList())
                            );
                            return null;
                        }
                );
            }
        });
    }

    @Override
    public Optional<Book> findById(String idBook) {
        Objects.requireNonNull(idBook);

        Book cachedBook = this.cacheService.get(generateCacheKey(idBook), Book.class);
        if(cachedBook != null){
            return Optional.of(cachedBook);
        }
        Book book = this.mongoCollection.find(Filters.eq("_id", new ObjectId(idBook))).first();
        if (book != null){
           this.cacheBook(book);
           return Optional.of(book);
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<Book>> findByTitle(String title) {
        Objects.requireNonNull(title);

        List<Book> books = this.mongoCollection
                .find(Filters.eq("title", title))
                .into(new ArrayList<>());

        if(!books.isEmpty()){
            books.forEach(this::cacheBook);
            return Optional.of(books);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> searchByTitle(String title) {
        Objects.requireNonNull(title);
        if(title.isBlank()) return List.of();

        /*
        // Create a regex that matches any of the titles, ignoring case
        // Pattern: "^(Title1|Title2|Title3)$" with CASE_INSENSITIVE flag
        String regexPattern = "^(" + String.join("|", titles).replace("?", "\\?") + ")$";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);


        return this.mongoCollection
                .find(Filters.regex("title", pattern))
                .into(new ArrayList<>());
        */
        return List.of();
    }

    @Override
    public List<Book> findByTitle(List<String> titles) {
        Objects.requireNonNull(titles);

        if(titles.isEmpty()) return List.of();

        logger.debug("[REPOSITORY] [BOOK] [FIND] [BY TITLE] titles: {}", titles);

        List<Book> books = this.mongoCollection
                .find(Filters.in("title", titles))
                .into(new ArrayList<>());

        return books;
    }

    @Override
    public List<Book> findByGoodReadsExternIds(List<String> externBookIds) {
        Objects.requireNonNull(externBookIds);

        if(externBookIds.isEmpty()) return List.of();

        logger.debug("[REPOSITORY] [BOOK] [FIND] [BY EXTERN IDS] ids: {}", externBookIds);

        List<Book> books = this.mongoCollection
                .find(Filters.in("externalId.goodReads", externBookIds))
                .into(new ArrayList<>());

        return books;
    }

    @Override
    public PageResult<Book> findAll(int page, int size) {
        logger.debug("[REPOSITORY] [BOOK] [FIND] [ALL] page: {}, size: {}", page, size);

        int skip = page * size;

        List<Book> books = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments();
        // cacheBookInThread(books); ////
        return new PageResult<>(books, total, page, size);
    }

    @Override
    public PageResult<Book> search(String title, int page, int size) {
        Objects.requireNonNull(title);

        logger.debug("[REPOSITORY] [BOOK] [SEARCH] title: {}, page: {}, size: {}", title, page, size);

        int skip = page * size;

        List<Book> books = this.mongoCollection
                .find(
                        Filters.regex("title", "^" + title + "$", "i")
                ).skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(
                        Filters.regex("title", "^" + title + "$", "i")
                );

        return new PageResult<>(books, total, page, size);
    }

}
