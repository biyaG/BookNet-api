package it.unipi.booknetapi.repository.user;

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
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.shared.lib.database.*;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class UserRepository implements UserRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final MongoClient mongoClient;
    private final MongoCollection<User> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;



    public UserRepository(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("users", User.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }



    /**
     * @param user the user to insert
     * @return the inserted user
     */
    @Override
    public User insert(User user) {
        Objects.requireNonNull(user);

        logger.debug("Inserting user: {}", user);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                boolean success;
                if(user.getId() == null) {
                    InsertOneResult insertOneResult = this.mongoCollection
                            .insertOne(mongoSession, user);
                    success = insertOneResult.getInsertedId() != null;
                    if(success) {
                        user.setId(Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId().getValue());
                    }
                } else {
                    UpdateResult updateResult = this.mongoCollection
                            .replaceOne(
                                    mongoSession,
                                    Filters.eq("_id", user.getId()),
                                    user
                            );
                    success = updateResult.getModifiedCount() > 0;
                }

                if(success) {
                    if(user instanceof Reader) {
                        saveReaderToNeo4j((Reader) user);
                    }

                    mongoSession.commitTransaction();

                    logger.info("User inserted successfully: {}", user);
                    return user;
                } else {
                    mongoSession.abortTransaction();
                    logger.error("Error during insert in mongodb");
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during insert in no4j: {}", e.getMessage());
            }
        }

        return null;
    }


    public User insertWithThread(User user) {
        Objects.requireNonNull(user);

        InsertOneResult insertOneResult = this.mongoCollection.insertOne(user);
        if(insertOneResult.getInsertedId() != null && user instanceof Reader) {
            Thread thread = new Thread(() -> saveReaderToNeo4j((Reader) user));
            thread.start();
        }

        return insertOneResult.wasAcknowledged() ? user : null;
    }

    private void saveReaderToNeo4j(Reader reader) {
        Objects.requireNonNull(reader);

        this.registry.timer("neo4j.ops", "query", "save_reader").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                String cypher = "CREATE (r:Reader {mid: $userId, name: $userName})";
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    Values.parameters(
                                            "userId", reader.getId().toHexString(),
                                            "userName", reader.getName()
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param users users to insert
     * @return the inserted users
     */
    @Override
    public List<User> insert(List<User> users) {
        Objects.requireNonNull(users);
        if(users.isEmpty()) return List.of();

        logger.debug("Inserting many user: {}", users.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                this.mongoCollection.insertMany(mongoSession, users);

                saveReadersToNeo4j(users);

                mongoSession.commitTransaction();

                logger.info("Many User inserted successfully: {}", users.size());

                return users;
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during insert in no4j: {}", e.getMessage());
            }
        }

        return List.of();
    }

    private void saveReadersToNeo4j(List<User> users) {
        List<Map<String, Object>> noe4jBatch = new ArrayList<>();

        for(User user : users) {
            if(user instanceof Reader) {
                noe4jBatch.add(Map.of("userId", user.getId().toHexString(), "userName", user.getName()));
            }
        }

        if(!noe4jBatch.isEmpty()) {
            this.registry.timer("neo4j.ops", "query", "save_reader").record(() -> {
                try (Session session = this.neo4jManager.getDriver().session()) {
                    session.executeWrite(
                            tx -> {
                                tx.run(
                                        "UNWIND $users as user CREATE (r:Reader {mid: user.userId, name: user.userName})",
                                        Values.parameters("users", noe4jBatch)
                                );
                                return null;
                            }
                    );
                }
            });
        }
    }

    /**
     * @param idUser user's id
     * @param newName new name
     * @return true if the name was updated successfully, false otherwise
     */
    @Override
    public boolean updateName(String idUser, String newName) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(newName);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                UpdateResult updateResult = this.mongoCollection
                        .updateOne(
                                mongoSession,
                                Filters.eq("_id", new ObjectId(idUser)),
                                Updates.set("name", newName)
                        );

                if(updateResult.getModifiedCount() > 0) {
                    this.updateReaderNameInNeo4j(idUser, newName);
                    mongoSession.commitTransaction();

                    return true;
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return false;
    }

    private void updateReaderNameInNeo4j(String idUser, String newName) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(newName);

        this.registry.timer("neo4j.ops", "query", "update_reader").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "MATCH (r:Reader {mid: $userId}) SET r.name = $userName",
                                    Values.parameters("userId", idUser, "userName", newName)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idUser user's id
     * @param newRole new role
     * @return true if the role was updated successfully, false otherwise
     */
    @Override
    public boolean updateRole(String idUser, Role newRole) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(newRole);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idUser)),
                        Updates.set("role", newRole)
                );

        return updateResult.getModifiedCount() > 0;
    }


    /**
     * @param idUser user's id
     * @param newPassword new password
     * @return true if the password was updated successfully, false otherwise
     */
    @Override
    public boolean updatePassword(String idUser, String newPassword) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(newPassword);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idUser)),
                        Updates.set("password", newPassword)
                );

        return updateResult.getModifiedCount() > 0;
    }

    /**
     * @param idUser user's id
     * @param newImageUrl user's new image url
     * @return true if the image url was updated successfully, false otherwise
     */
    @Override
    public boolean updateImage(String idUser, String newImageUrl) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(newImageUrl);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idUser)),
                        Updates.set("imageUrl", newImageUrl)
                );

        return updateResult.getModifiedCount() > 0;
    }

    /**
     * @param idUser user's id
     * @param preference new preference
     * @return true if the preference was updated successfully, false otherwise
     */
    @Override
    public boolean updatePreference(String idUser, UserPreference preference) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(preference);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idUser)),
                        Updates.set("preference", preference)
                );

        if(updateResult.getModifiedCount() > 0) {
            updateNeo4jPreferenceThread(idUser, preference);
            return true;
        }

        return false;
    }

    private void updateNeo4jPreferenceThread(String idUser, UserPreference preference) {
        Thread thread = new Thread(() -> updateNeo4jPreference(idUser, preference));
        thread.start();
    }

    private void updateNeo4jPreference(String idUser, UserPreference preference) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(preference);

        List<Map<String, Object>> genreList = preference.getGenres() == null ? Collections.emptyList() :
                preference.getGenres().stream()
                        .filter(g -> g.getId() != null) // Safety check
                        .map(g -> Map.<String, Object>of("id", g.getId(), "name", g.getName()))
                        .toList();

        List<Map<String, Object>> authorList = preference.getAuthors() == null ? Collections.emptyList() :
                preference.getAuthors().stream()
                        .filter(a -> a.getId() != null)
                        .map(a -> Map.<String, Object>of("id", a.getId(), "name", a.getName()))
                        .toList();

        this.registry.timer("neo4j.ops", "query", "update_reader").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {

                session.executeWrite(tx -> {
                    String cypher = """
                    MATCH (r:Reader {mid: $userId})
                    
                    // Step A: Clear existing relationships (Full Overwrite Strategy)
                    // We use OPTIONAL MATCH to avoid failing if no relationships exist yet
                    OPTIONAL MATCH (r)-[r1:INTERESTED_IN]->(:Genre)
                    OPTIONAL MATCH (r)-[r2:FOLLOWS]->(:Author)
                    DELETE r1, r2
                   
                    // Step B: Rebuild Genre Connections
                    // We use FOREACH because UNWIND fails the whole query if the list is empty
                    WITH r
                    FOREACH (gData IN $genres |
                        MERGE (g:Genre {mid: gData.id})
                        ON CREATE SET g.name = gData.name
                        MERGE (r)-[:INTERESTED_IN]->(g)
                    )
                   
                    // Step C: Rebuild Author Connections
                    FOREACH (aData IN $authors |
                        MERGE (a:Author {mid: aData.id})
                        ON CREATE SET a.name = aData.name
                        MERGE (r)-[:FOLLOWS]->(a)
                    )
                    """;

                    tx.run(
                            cypher,
                            Values.parameters(
                                "userId", idUser,
                                "genres", genreList,
                                "authors", authorList
                            )
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param idUser user's id
     * @param books list of books to add to the shelf
     * @return true if the shelf was updated successfully, false otherwise
     */
    @Override
    public boolean updateShelf(String idUser, List<BookEmbed> books) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(books);

        List<UserBookShelf> shelf = books.stream()
                .filter(b -> b.getId() != null)
                .map(b -> new UserBookShelf(b, BookShelfStatus.ADDED, new Date()))
                .toList();

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                UpdateResult updateResult = this.mongoCollection
                        .updateOne(
                                Filters.eq("_id", new ObjectId(idUser)),
                                Updates.set("shelf", shelf)
                        );

                if(updateResult.getModifiedCount() > 0) {
                    updateShelfNeo4j(idUser, shelf);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return false;
    }

    private void updateShelfNeo4j(String idUser, List<UserBookShelf> shelf) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(shelf);

        List<Map<String, Object>> batch = shelf.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookId", item.getBook().getId().toHexString());
                    map.put("status", item.getStatus());
                    // No need for 'bookTitle' or other fields
                    return map;
                })
                .toList();

        this.registry.timer("neo4j.ops", "query", "update_shelf").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    String cypher = """
                        // 1. Find the User
                        MATCH (r:Reader {mid: $userId})
                    
                        // 2. Iterate through the list of books
                        UNWIND $batch AS row
                    
                        // 3. STRICT MATCH: Find the book by its Mongo ID
                        // If the book does not exist in Neo4j, this line fails for that row,
                        // and the rest of the operations (MERGE relationship) are SKIPPED for that specific book.
                        MATCH (b:Book {mid: row.bookId})
                    
                        // 4. If we survived the MATCH above, create/update the relationship
                        MERGE (r)-[rel:ADDED_TO_SHELF]->(b)
                    
                        // 5. Update Relationship Properties
                        SET rel.ts = datetime(),
                            rel.status = row.status
                    """;
                    tx.run(
                            cypher,
                            Values.parameters(
                                "userId", idUser,
                                    "batch", batch
                            )
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param idUser user's id
     * @param book book to add to the shelf
     * @return true if the book was added successfully, false otherwise
     */
    @Override
    public boolean addBookInShelf(String idUser, BookEmbed book) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(book);

        if(!ObjectId.isValid(idUser)) return false;
        if(book.getId() == null) return false;

        UserBookShelf bookShelf = new UserBookShelf(book, BookShelfStatus.ADDED, new Date());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                UpdateResult updateResult = this.mongoCollection
                        .updateOne(
                                Filters.eq("_id", new ObjectId(idUser)),
                                Updates.push("shelf", bookShelf)
                        );

                if(updateResult.getModifiedCount() > 0) {
                    addBookInShelfNeo4j(idUser, book);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }



        return false;
    }

    private void addBookInShelfNeo4j(String idUser, BookEmbed book) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(book);

        this.registry.timer("neo4j.ops", "query", "add_to_shelf").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {

                session.executeWrite(tx -> {
                    String cypher = """
                    MATCH (r:Reader {mid: $userId})
                    
                    // 1. Ensure the Book exists in the Graph
                    // We merge on 'mid' (Mongo ID) to avoid duplicates
                    MERGE (b:Book {mid: $bookId})
                    
                    // 2. If the book is new to Neo4j, initialize its properties
                    ON CREATE SET
                        b.id = randomUUID(),  // Generate a Neo4j-specific UUID
                        b.title = $bookTitle
                    
                    // 3. Create the Shelf Relationship
                    MERGE (r)-[rel:ADDED_TO_SHELF]->(b)
                    
                    // 4. Update Relationship Properties
                    SET rel.ts = datetime(),
                        // Default to 'WANT_TO_READ' if status is missing,
                        // otherwise keep the existing status
                        rel.status = COALESCE(rel.status, 'ADDED')
                    """;

                    tx.run(
                            cypher,
                            Values.parameters(
                                "userId", idUser,
                                "bookId", book.getId().toHexString(),
                                "bookTitle", book.getTitle()
                            )
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param idUser user's id
     * @param idBook book's id
     * @return true if the book was removed successfully, false otherwise
     */
    @Override
    public boolean removeBookFromShelf(String idUser, String idBook) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idBook);
        if(!ObjectId.isValid(idUser) || !ObjectId.isValid(idBook)) return false;

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idUser)),
                Updates.pull("shelf", Filters.eq("id", new ObjectId(idBook)))
        );

        if(updateResult.getModifiedCount() > 0) {
            Runnable task = () -> this.removeBookFromShelfNeo4j(idUser, idBook);
            Thread thread = new Thread(task);
            thread.start();
        }

        return false;
    }

    private void removeBookFromShelfNeo4j(String idUser, String idBook) {
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idBook);

        this.registry.timer("neo4j.ops", "query", "remove_from_shelf").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    tx.run(
                            "MATCH (r:Reader {mid: $userId})-[rel:ADDED_TO_SHELF]->(b:Book {mid: $bookId}) DELETE rel",
                            Values.parameters("userId", idUser, "bookId", idBook)
                    );
                    return null;
                });
            }
        });
    }


    /**
     * @param review review
     * @return true if the review was added successfully, false otherwise
     */
    @Override
    public boolean addReview(Review review) {
        Objects.requireNonNull(review);
        Objects.requireNonNull(review.getBookId());
        Objects.requireNonNull(review.getUser());
        Objects.requireNonNull(review.getUser().getId());

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", review.getUser().getId()),
                Updates.push("reviews", review.getId())
        );

        if(updateResult.getModifiedCount() > 0) {
            Runnable task = () -> this.addReviewInNeo4j(
                    review.getUser().getId().toHexString(),
                    review.getBookId().toHexString(),
                    review.getRating(),
                    review.getDateAdded()
            );
            Thread thread = new Thread(task);
            thread.start();
        }

        return updateResult.getModifiedCount() > 0;
    }

    private void addReviewInNeo4j(String idUser, String idBook, Float rating, Date date) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(rating);
        Objects.requireNonNull(date);

        this.registry.timer("neo4j.ops", "query", "add_review").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    tx.run(
                            "MATCH (r:Reader {mid: $userId})-[rel:RATER]->(b:Book {mid: $bookId}) SET rel.rating = $rating, rel.ts = $date",
                            Values.parameters("userId", idUser, "bookId", idBook, "rating", rating, "date", date)
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param idUser user's id
     * @param idReview review's id
     * @return true if the review was deleted successfully, false otherwise
     */
    @Override
    public boolean deleteReview(String idUser, String idBook, String idReview) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idReview);

        if(!ObjectId.isValid(idUser) || !ObjectId.isValid(idBook) || !ObjectId.isValid(idReview)) return false;

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.and(
                    Filters.eq("_id", new ObjectId(idUser)),
                    Filters.elemMatch("reviews", Filters.eq("$id", new ObjectId(idReview)))
                ),
                Updates.pull("reviews", Filters.eq("$id", new ObjectId(idReview)))
        );

        if(updateResult.getModifiedCount() > 0) {
            Runnable task = () -> this.deleteReviewInNeo4j(idUser, idBook);
            Thread thread = new Thread(task);
            thread.start();
        }

        return updateResult.getModifiedCount() > 0;
    }

    private void deleteReviewInNeo4j(String idUser, String idBook) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);

        this.registry.timer("neo4j.ops", "query", "delete_review").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(tx -> {
                    tx.run(
                            "MATCH (r:Reader {mid: $userId})-[rel:RATER]->(b:Book {mid: $bookId}) DELETE rel",
                            Values.parameters("userId", idUser, "bookId", idBook)
                    );
                    return null;
                });
            }
        });
    }

    /**
     * @param idUser user's id
     */
    @Override
    public boolean delete(String idUser) {
        Objects.requireNonNull(idUser);

        if(!ObjectId.isValid(idUser)) return false;

        try(ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection
                        .deleteOne(Filters.eq("_id", new ObjectId(idUser)));
                if(deleteResult.getDeletedCount() > 0) {
                    deleteUserFromNeo4j(idUser);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return false;
    }

    private void deleteUserFromNeo4j(String idUser) {
        Objects.requireNonNull(idUser);

        this.registry.timer("neo4j.ops", "query", "delete_user").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (r:Reader {mid: $userId}) DETACH DELETE r",
                                    Values.parameters("userId", idUser)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idUsers user's ids
     * @return true if the users were deleted successfully, false otherwise
     */
    @Override
    public boolean deleteAll(List<String> idUsers) {
        Objects.requireNonNull(idUsers);

        List<String> ids = idUsers.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        if(ids.isEmpty()) return true;

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection
                        .deleteMany(
                                Filters.in("_id", ids.stream().map(ObjectId::new).toList())
                        );
                if(deleteResult.getDeletedCount() > 0) {
                    deleteUsersBatchFromNeo4j(ids);
                    mongoSession.commitTransaction();
                    return true;
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                return false;
            }
        }

        return false;
    }

    private void deleteUsersBatchFromNeo4j(List<String> idUsers) {
        Objects.requireNonNull(idUsers);

        List<String> ids = idUsers.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        this.registry.timer("neo4j.ops", "query", "delete_user").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (r:Reader) WHERE r.mid IN $userIds DETACH DELETE r",
                                    Values.parameters("userIds", ids)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idUser user's id
     * @return a user associated with the given id or empty if not found
     */
    @Override
    public Optional<User> findById(String idUser) {
        Objects.requireNonNull(idUser);

        if(!ObjectId.isValid(idUser)) return Optional.empty();

        User user = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idUser)))
                .first();

        return user != null ? Optional.of(user) : Optional.empty();
    }

    /**
     * @param username user's username
     * @return a user associated with the given username or empty if not found
     */
    @Override
    public Optional<User> findByUsername(String username) {
        Objects.requireNonNull(username);

        User user = this.mongoCollection
                .find(Filters.eq("username", username))
                .first();

        return user != null ? Optional.of(user) : Optional.empty();
    }

    /**
     * @return list of all users with pagination
     */
    @Override
    public PageResult<User> findAll(int page, int size) {
        int skip = page * size;

        List<User> users = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments();

        return new PageResult<>(users, total, page, size);
    }
}
