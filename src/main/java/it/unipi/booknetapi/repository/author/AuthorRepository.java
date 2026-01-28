package it.unipi.booknetapi.repository.author;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.ExternalId;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AuthorRepository implements AuthorRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(AuthorRepository.class);

    private final Integer batchSize;

    private final MongoClient mongoClient;
    private final MongoCollection<Author> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;

    private static final String CACHE_PREFIX = "author:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public AuthorRepository(
            AppConfig appConfig,
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.batchSize = appConfig.getBatchSize() != null ? appConfig.getBatchSize() : 100;
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("authors", Author.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }


    /**
     * @param author the author to insert
     * @return the inserted author
     */
    @Override
    public Author insert(Author author) {
        Objects.requireNonNull(author);

        logger.debug("[REPOSITORY] [AUTHOR] [INSERT] author: {}", author);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                InsertOneResult insertOneResult = this.mongoCollection.insertOne(mongoSession, author);

                if(insertOneResult.getInsertedId() != null) {
                    mongoSession.commitTransaction();
                    saveAuthorToNeo4j(author);
                    return author;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [AUTHOR] [INSERT] Error during transaction: {}", e.getMessage());
            }
        }

        return null;
    }

    private void saveAuthorToNeo4j(Author author) {
        Objects.requireNonNull(author);

        this.registry.timer("neo4j.ops", "query", "save_author").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                String cypher = "CREATE (a:Author {mid: $id, name: $name})";
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    Values.parameters(
                                            "id", author.getId(),
                                            "name", author.getName()
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param authors authors to insert
     * @return the inserted authors
     */
    @Override
    public List<Author> insert(List<Author> authors) {
        Objects.requireNonNull(authors);
        if(authors.isEmpty()) return List.of();

        logger.debug("[REPOSITORY] [AUTHOR] [INSERT MANY] author size: {}", authors.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                this.mongoCollection.insertMany(mongoSession, authors);

                saveAuthorsToNeo4j(authors);

                mongoSession.commitTransaction();

                logger.debug("[REPOSITORY] [AUTHOR] [INSERT] Many authors inserted successfully: {}", authors.size());

                return authors;
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return List.of();
    }

    private void saveAuthorsToNeo4j(List<Author> authors) {
        List<Map<String, Object>> neo4jBatch = new ArrayList<>();

        authors.forEach(author -> {
            neo4jBatch.add(Map.of("id", author.getId(), "name", author.getName()));
        });

        if(!neo4jBatch.isEmpty()) {
            this.registry.timer("neo4j.ops", "query", "save_authors").record(() -> {
                try (Session session = this.neo4jManager.getDriver().session()) {
                    session.executeWrite(
                            tx -> {
                                tx.run(
                                        "UNWIND $authors as author CREATE (a:Author {mid: author.id, name: author.name})",
                                        Values.parameters("authors", neo4jBatch)
                                );
                                return null;
                            }
                    );
                }
            });
        }
    }

    /**
     * @param importedAuthors the authors to import
     */
    @Override
    public List<Author> importAuthors(List<AuthorGoodReads> importedAuthors) {
        logger.debug("[REPOSITORY] [AUTHOR] [IMPORT] Importing {} authors from GoodReads", importedAuthors.size());

        List<Author> result = new ArrayList<>(importedAuthors.size());

        for(int i=0; i < importedAuthors.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, importedAuthors.size());

            List<AuthorGoodReads> importedAuthorsBatch =  importedAuthors.subList(i, endIndex);

            List<ObjectId> ids = bulkUpset(importedAuthorsBatch);

            List<Map<String, Object>> neo4jBatch = new ArrayList<>();

            List<Author> authors = this.mongoCollection.find(Filters.in("_id", ids))
                    .projection(Projections.include("_id", "name")) // Optimize: fetch only needed fields
                    .into(new ArrayList<>());

            authors.forEach(
                    author -> {
                        // Map MongoDB Author POJO to Neo4j Map
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", author.getId().toHexString()); // The crucial MongoDB ID
                        map.put("name", author.getName());
                        neo4jBatch.add(map);
                    }
            );

            if (!neo4jBatch.isEmpty()) {
                bulkUpdateAuthorsInNeo4j(neo4jBatch);
            }

            result.addAll(authors);
        }

        return result;
    }

    private List<ObjectId> bulkUpset(List<AuthorGoodReads> authorsGoodReads) {
        logger.debug("[REPOSITORY] [AUTHOR] [IMPORT] [MONGODB] Importing {} authors from GoodReads", authorsGoodReads.size());

        List<WriteModel<Author>> writes = new ArrayList<>();

        for (AuthorGoodReads source : authorsGoodReads) {

            String goodReadsId = source.getAuthorId();
            ExternalId externalId = ExternalId.builder().goodReads(goodReadsId).build();

            // Even though we use POJOs, we use 'Updates.set' to avoid overwriting the 'authors' list
            writes.add(new UpdateOneModel<>(
                    // Filter: Find author by external Kaggle ID
                    Filters.eq("externalId.goodReads", goodReadsId),

                    // Update: Combine multiple operations
                    Updates.combine(
                            // ALWAYS update these fields (e.g. fix name typos)
                            Updates.set("name", source.getName()),

                            // ON INSERT only:
                            Updates.setOnInsert("externalId", externalId),
                            Updates.setOnInsert("description", null),
                            Updates.setOnInsert("imageUrl", null),
                            Updates.setOnInsert("books", List.of())
                    ),

                    // Create if it doesn't exist
                    new UpdateOptions().upsert(true)
            ));
        }

        if (!writes.isEmpty()) {
            BulkWriteResult result = this.mongoCollection
                    .bulkWrite(writes, new BulkWriteOptions().ordered(false));

            List<BulkWriteUpsert> inserts = result.getUpserts();

            List<ObjectId> objectIds = new ArrayList<>();
            for (BulkWriteUpsert insert : inserts) {
                objectIds.add(insert.getId().asObjectId().getValue());
            }
            logger.debug("[REPOSITORY] [AUTHOR] [IMPORT] [MONGODB] writes {} authors", objectIds.size());

            return objectIds;
        } else {
            logger.debug("[REPOSITORY] [AUTHOR] [IMPORT] [MONGODB] writes is empty");
        }

        return List.of();
    }

    // In your Neo4j Service or Repository
    private void bulkUpdateAuthorsInNeo4j(List<Map<String, Object>> authorsBatch) {
        logger.debug("[REPOSITORY] [AUTHOR] [IMPORT] [NEO4J] Importing {} authors", authorsBatch.size());

        // Cypher:
        // 1. UNWIND expands the list into rows.
        // 2. MERGE finds the author by ID or creates them.
        // 3. SET updates the name (in case it changed in MongoDB).
        String query = """
        UNWIND $batch AS row
        MERGE (a:Author {mid: row.id})
        SET a.name = row.name
        """;

        try (Session session = this.neo4jManager.getDriver().session()) {
            session.executeWrite(tx -> {
                tx.run(query, Values.parameters("batch", authorsBatch));
                return null;
            });
        }
    }

    /**
     * @param idAuthor author's id
     * @param newDescription new description for the author
     * @return true if the description was updated, false otherwise
     */
    @Override
    public boolean updateDescription(String idAuthor, String newDescription) {
        Objects.requireNonNull(idAuthor);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idAuthor)),
                        Updates.set("description", newDescription)
                );

        return updateResult.getModifiedCount() > 0;
    }

    /**
     * @param idAuthor author's id
     * @param newImageUrl new image url for the author
     * @return true if the image url was updated, false otherwise
     */
    @Override
    public boolean updateImage(String idAuthor, String newImageUrl) {
        Objects.requireNonNull(idAuthor);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idAuthor)),
                        Updates.set("imageUrl", newImageUrl)
                );

        return updateResult.getModifiedCount() > 0;
    }

    /**
     * @param idAuthor author's id
     * @param books books to update for the author
     * @return true if the books were updated, false otherwise
     */
    @Override
    public boolean updateBooks(String idAuthor, List<BookEmbed> books) {
        Objects.requireNonNull(idAuthor);
        Objects.requireNonNull(books);

        if(!ObjectId.isValid(idAuthor)) return false;

        logger.debug("[REPOSITORY] [AUTHOR] [SET BOOKS] author: {}, book size: {}", idAuthor, books.size());

        UpdateResult result = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idAuthor)),
                        Updates.set("books", books)
                );

        return result.getModifiedCount() > 0;
    }

    /**
     * @param idAuthor author's id
     * @param book book to add to the author's list of books
     * @return true if the book was added, false otherwise
     */
    @Override
    public boolean addBook(String idAuthor, BookEmbed book) {
        Objects.requireNonNull(idAuthor);
        Objects.requireNonNull(book);

        if(!ObjectId.isValid(idAuthor)) return false;

        logger.debug("[REPOSITORY] [AUTHOR] [ADD BOOK] author: {}, book: {}", idAuthor, book.getId());

        UpdateResult result = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idAuthor)),
                        Updates.push("books", book)
                );

        return result.getModifiedCount() > 0;
    }

    /**
     * @param idAuthor author's id
     * @param idBook book's id to remove from the author's list of books
     * @return true if the book was removed, false otherwise
     */
    @Override
    public boolean removeBook(String idAuthor, String idBook) {
        Objects.requireNonNull(idAuthor);
        Objects.requireNonNull(idBook);

        if(!ObjectId.isValid(idAuthor)) return false;
        if(!ObjectId.isValid(idBook)) return false;

        logger.debug("[REPOSITORY] [AUTHOR] [REMOVE BOOK] author: {}, book: {}", idAuthor, idBook);

        UpdateResult result = mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idAuthor)),
                Updates.pull("books", new Document("id", new ObjectId(idBook)))
        );

        return result.getModifiedCount() > 0;
    }

    /**
     * @param idAuthor author's id
     * @return true if the author was deleted, false otherwise
     */
    @Override
    public boolean delete(String idAuthor) {
        Objects.requireNonNull(idAuthor);

        logger.debug("[REPOSITORY] [AUTHOR] [DELETE] [BY ID] id: {}", idAuthor);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection.deleteOne(mongoSession, Filters.eq("_id", new ObjectId(idAuthor)));

                if(deleteResult.getDeletedCount() > 0) {
                    deleteAuthorFromNeo4j(idAuthor);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [AUTHOR] [DELETE] [BY ID] Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    private void deleteAuthorFromNeo4j(String idAuthor) {
        Objects.requireNonNull(idAuthor);

        this.registry.timer("neo4j.ops", "query", "delete_author").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (a:Author {mid: $id}) DETACH DELETE a",
                                    Values.parameters("id", idAuthor)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idAuthors author's ids
     * @return true if all authors were deleted, false otherwise
     */
    @Override
    public boolean delete(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

        logger.debug("[REPOSITORY] [AUTHOR] [DELETE] [BY IDS] authors ids: {}", idAuthors);

        List<String> ids = idAuthors.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        if(ids.isEmpty()) return true;

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection.deleteMany(mongoSession, Filters.in("_id", ids.stream().map(ObjectId::new).toList()));

                if(deleteResult.getDeletedCount() > 0) {
                    deleteAuthorsFromNeo4j(ids);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("[REPOSITORY] [AUTHOR] [DELETE] [BY IDS] Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    private void deleteAuthorsFromNeo4j(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

        List<String> ids = idAuthors.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        this.registry.timer("neo4j.ops", "query", "delete_authors").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (a:Author) WHERE a.mid IN $ids DETACH DELETE a",
                                    Values.parameters("ids", ids)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idAuthor author's id
     * @return the author with the given id, or empty if not found
     */
    @Override
    public Optional<Author> findById(String idAuthor) {
        Objects.requireNonNull(idAuthor);

        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [BY ID] id: {}", idAuthor);

        Author author = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idAuthor)))
                .first();

        return author != null ? Optional.of(author) : Optional.empty();
    }

    @Override
    public List<BookEmbed> findBooksByAuthor(String authorId) {
        Objects.requireNonNull(authorId);

        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [BOOK] [BY AUTHOR] [ID] id: {}", authorId);

        Author author = mongoCollection
                .find(Filters.eq("_id",  new ObjectId(authorId)))
                .first();

        if (author == null) {
            return Collections.emptyList();
        }
        return author.getBooks();
    }

    @Override
    public List<Author> findAllById(List<String> authorIds) {

        if (authorIds == null || authorIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<ObjectId> objectIds = authorIds.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        if(objectIds.isEmpty()) return new ArrayList<>();

        logger.debug("[REPOSITORY] [AUTHOR] [FIND BY IDS] author ids size: {}", objectIds.size());

        return mongoCollection
                .find(Filters.in("_id", objectIds))
                .into(new ArrayList<>());
    }

    /**
     * @return list of all authors with pagination
     */
    @Override
    public PageResult<Author> findAll(int page, int size) {
        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [ALL] page: {}, size: {}", page, size);

        int skip = page * size;

        List<Author> authors = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments();

        return new PageResult<>(authors, total, page, size);
    }

    /**
     * @param name name of author to search
     * @param page page number
     * @param size size of page
     * @return list of authors with pagination
     */
    @Override
    public PageResult<Author> search(String name, int page, int size) {
        logger.debug("[REPOSITORY] [AUTHOR] [SEARCH] page: {}, size: {}", page, size);

        int skip = page * size;

        List<Author> authors = this.mongoCollection
                .find(
                        Filters.regex("name", "^" + name + "$", "i")
                )
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(
                        Filters.regex("name", "^" + name + "$", "i")
                );

        return new PageResult<>(authors, total, page, size);
    }

    /**
     * @param idAuthors author's ids
     * @return list of authors with the given ids, or empty if not found
     */
    @Override
    public List<Author> findAll(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [MANY] authors: {}", idAuthors);

        List<ObjectId> ids = idAuthors.stream()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        return this.find(ids);
    }

    /**
     * @param idAuthors author's ids
     * @return list of authors with the given ids, or empty if not found
     */
    @Override
    public List<Author> find(List<ObjectId> idAuthors) {
        Objects.requireNonNull(idAuthors);

        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [MANY] authors ids size: {}", idAuthors.size());

        if(idAuthors.isEmpty()) return List.of();

        return this.mongoCollection
                .find(Filters.in("_id", idAuthors))
                .into(new ArrayList<>());
    }


    /**
     * @param idAuthors extern goodreads author's ids
     * @return list of authors with the given ids, or empty if not found
     */
    @Override
    public List<Author> findByExternGoodReadIds(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

        logger.debug("[REPOSITORY] [AUTHOR] [FIND] [MANY] [BY EXTERN GOOD READ IDS] authors ids size: {}", idAuthors.size());

        if(idAuthors.isEmpty()) return List.of();

        return this.mongoCollection
                .find(Filters.in("externalId.goodReads", idAuthors))
                .into(new ArrayList<>());
    }

    /**
     * Migrate authors from mongodb to neo4j
     */
    @Override
    public void migrateAuthors() {
        logger.debug("[REPOSITORY] [AUTHOR] [MIGRATE] [MONGODB TO NEO4J]");

        // int batchSize = 100;
        int offset = 0;

        long totalDocuments = this.mongoCollection.countDocuments();

        logger.debug("[REPOSITORY] [AUTHOR] [MIGRATE] [MONGODB TO NEO4J] Starting migration of {} authors...", totalDocuments);

        while (offset < totalDocuments) {
            List<Author> authors = new ArrayList<>();
            try (MongoCursor<Author> cursor = this.mongoCollection.find()
                    .skip(offset)
                    .limit(this.batchSize)
                    .iterator()
            ) {
                while (cursor.hasNext()) {
                    authors.add(cursor.next());
                }
            }

            if(authors.isEmpty()) break;

            List<Map<String, Object>> neo4jBatch = new ArrayList<>();
            for(Author author : authors) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", author.getId().toHexString());
                map.put("name", author.getName());
                neo4jBatch.add(map);
            }

            String query = """
            UNWIND $batch AS row
            MERGE (a:Author {mid: row.id})
            ON CREATE SET
                a.name = row.name
            """;

            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    tx.run(query, Values.parameters("batch", neo4jBatch));
                    return null;
                });
            }

            logger.debug("[REPOSITORY] [AUTHOR] [MIGRATE] [MONGODB TO NEO4J] Migrated batch: {} to {}", offset, offset + batchSize);

            offset += this.batchSize;
        }

        logger.debug("[REPOSITORY] [AUTHOR] [MIGRATE] [MONGODB TO NEO4J] Migration complete.");
    }
}
