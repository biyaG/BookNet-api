package it.unipi.booknetapi.repository.author;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Repository
public class AuthorRepository implements AuthorRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(AuthorRepository.class);

    private final MongoClient mongoClient;
    private final MongoCollection<Author> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;

    public AuthorRepository(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
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

        logger.debug("Inserting author: {}", author);

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
                logger.error("Error during transaction: {}", e.getMessage());
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
                                    parameters(
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

        logger.debug("Inserting many author: {}", authors.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                this.mongoCollection.insertMany(mongoSession, authors);

                saveAuthorsToNeo4j(authors);

                mongoSession.commitTransaction();

                logger.info("Many authors inserted successfully: {}", authors.size());

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
                                        parameters("authors", neo4jBatch)
                                );
                                return null;
                            }
                    );
                }
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

        // TODO: to complete (put in transactional and call neo4j)

        return false;
    }

    private void updateBooksInNeo4j(String idAuthor, List<BookEmbed> books) {
        Objects.requireNonNull(idAuthor);
        Objects.requireNonNull(books);

        // TODO: to complete
    }

    /**
     * @param idAuthor
     * @param book
     * @return
     */
    @Override
    public boolean addBook(String idAuthor, BookEmbed book) {
        return false;
    }

    /**
     * @param idAuthor
     * @param idBook
     * @return
     */
    @Override
    public boolean removeBook(String idAuthor, String idBook) {
        return false;
    }

    /**
     * @param idAuthor author's id
     * @return true if the author was deleted, false otherwise
     */
    @Override
    public boolean delete(String idAuthor) {
        Objects.requireNonNull(idAuthor);

        logger.debug("Deleting author: {}", idAuthor);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection.deleteOne(mongoSession, Filters.eq("_id", new ObjectId(idAuthor)));

                if(deleteResult.getDeletedCount() > 0) {
                    mongoSession.commitTransaction();
                    deleteAuthorFromNeo4j(idAuthor);
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during transaction: {}", e.getMessage());
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
                                    parameters("id", idAuthor)
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
    public boolean deleteAll(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

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
                logger.error("Error during transaction: {}", e.getMessage());
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
                                    parameters("ids", ids)
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

        logger.debug("Retrieving author: {}", idAuthor);

        Author author = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idAuthor)))
                .first();

        if (author != null) {
            logger.debug("Author successfuly retrieving");
        } else {
            logger.debug("Author not found");
        }

        return author != null ? Optional.of(author) : Optional.empty();
    }

    /**
     * @return list of all authors with pagination
     */
    @Override
    public PageResult<Author> findAll(int page, int size) {
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
     * @param idAuthors author's ids
     * @return list of authors with the given ids, or empty if not found
     */
    @Override
    public List<Author> findAll(List<String> idAuthors) {
        Objects.requireNonNull(idAuthors);

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

        if(idAuthors.isEmpty()) return List.of();

        return this.mongoCollection
                .find(Filters.in("_id", idAuthors))
                .into(new ArrayList<>());
    }
}
