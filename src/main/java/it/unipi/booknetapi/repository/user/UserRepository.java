package it.unipi.booknetapi.repository.user;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.lib.database.*;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

@Repository
public class UserRepository implements UserRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final MongoManager mongoManager;
    private final MongoCollection<User> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "user:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public UserRepository(MongoManager mongoManager, Neo4jManager neo4jManager, CacheService cacheService) {
        // We get a collection specifically typed for 'User'
        this.mongoManager = mongoManager;
        this.mongoCollection = mongoManager.getDatabase().getCollection("users", User.class);
        this.neo4jManager = neo4jManager;
        this.cacheService = cacheService;
    }

    private static String generateCacheKey(String idUser) {
        return CACHE_PREFIX + idUser;
    }

    private void cacheUser(User user) {
        this.cacheService.save(generateCacheKey(user.getId().toHexString()), user, CACHE_TTL);
    }

    private void cacheUser(List<User> users) {
        users.forEach(this::cacheUser);
    }

    private void cacheUserInThread(List<User> users) {
        Thread thread = new Thread(() -> cacheUser(users));
        thread.start();
    }

    private void deleteCache(String idUser) {
        this.cacheService.delete(generateCacheKey(idUser));
    }

    private void deleteCache(List<String> idUsers) {
        idUsers.forEach(this::deleteCache);
    }

    private void deleteCacheInThread(List<String> idUsers) {
        Thread thread = new Thread(() -> deleteCache(idUsers));
        thread.start();
    }

    /**
     * @param user the user to insert
     * @return the inserted user
     */
    @Override
    public User insert(User user) {
        Objects.requireNonNull(user);

        logger.debug("Inserting user: {}", user);

        try (ClientSession mongoSession = this.mongoManager.getMongoClient().startSession()) {
            mongoSession.startTransaction();

            try {
                boolean success;
                if(user.getId() == null) {
                    InsertOneResult insertOneResult = this.mongoCollection
                            .insertOne(user);
                    success = insertOneResult.wasAcknowledged();
                    if(success) {
                        user.setId(Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId().getValue());
                    }
                } else {
                    UpdateResult updateResult = this.mongoCollection
                            .replaceOne(
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

                    this.cacheUser(user);
                    logger.info("User inserted successfully: {}", user);
                    return user;
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during insert in no4j: {}", e.getMessage());
                return null;
            }
        }

        return null;
    }


    public User insertWithThread(User user) {
        Objects.requireNonNull(user);

        InsertOneResult insertOneResult = this.mongoCollection.insertOne(user);
        if(insertOneResult.wasAcknowledged() && user instanceof Reader) {
            Thread thread = new Thread(() -> saveReaderToNeo4j((Reader) user));
            thread.start();
        }

        return insertOneResult.wasAcknowledged() ? user : null;
    }

    private void saveReaderToNeo4j(Reader reader) {
        Objects.requireNonNull(reader);

        try (Session session = this.neo4jManager.getSession()) {
            String cypher = "CREATE (r:Reader {mid: $userId, name: $userName})";
            session.executeWrite(
                    tx -> tx.run(
                            cypher,
                            parameters(
                                    "userId", reader.getId().toHexString(),
                                    "userName", reader.getName()
                            )
                    )
            );
        }
    }

    /**
     * @param users users to insert
     * @return the inserted users
     */
    @Override
    public List<User> insertAll(List<User> users) {
        Objects.requireNonNull(users);
        if(users.isEmpty()) return List.of();

        logger.debug("Inserting many user: {}", users.size());

        try (ClientSession mongoSession = this.mongoManager.getMongoClient().startSession()) {
            mongoSession.startTransaction();

            try {
                this.mongoCollection.insertMany(mongoSession, users);

                saveReadersToNeo4j(users);

                mongoSession.commitTransaction();

                this.cacheUserInThread(users);

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
            try (Session session = this.neo4jManager.getSession()) {
                session.executeWrite(
                        tx -> tx.run(
                                "UNWIND $users as user CREATE (r:Reader {mid: user.userId, name: user.userName})",
                                parameters("users", noe4jBatch)
                        )
                );
            }
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

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idUser)),
                        Updates.set("name", newName)
                );

        if(updateResult.getModifiedCount() > 0) {
            this.deleteCache(idUser);
            return true;
        }

        return false;
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

        if(updateResult.getModifiedCount() > 0) {
            this.deleteCache(idUser);
            return true;
        }

        return false;
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

        if(updateResult.getModifiedCount() > 0) {
            this.deleteCache(idUser);
            return true;
        }

        return false;
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

        if(updateResult.getModifiedCount() > 0) {
            this.deleteCache(idUser);
            return true;
        }

        return false;
    }

    /**
     * @param idUser user's id
     */
    @Override
    public boolean delete(String idUser) {
        Objects.requireNonNull(idUser);

        try(ClientSession mongoSession = this.mongoManager.getMongoClient().startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection
                        .deleteOne(Filters.eq("_id", new ObjectId(idUser)));
                if(deleteResult.getDeletedCount() > 0) {
                    deleteUserFromNeo4j(idUser);
                    mongoSession.commitTransaction();
                    this.deleteCache(idUser);
                    return true;
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return false;
    }

    private void deleteUserFromNeo4j(String idUser) {
        Objects.requireNonNull(idUser);

        try (Session session = this.neo4jManager.getSession()) {
            session.executeWrite(
                    tx -> tx.run(
                            "OPTIONAL MATCH (r:Reader {mid: $userId}) DETACH DELETE r",
                            parameters("userId", idUser)
                    )
            );
        }
    }

    /**
     * @param idUsers user's ids
     * @return true if the users were deleted successfully, false otherwise
     */
    @Override
    public boolean deleteAll(List<String> idUsers) {
        Objects.requireNonNull(idUsers);

        try (ClientSession mongoSession = this.mongoManager.getMongoClient().startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection
                        .deleteMany(
                                Filters.in("_id", idUsers.stream().map(ObjectId::new).toList())
                        );
                if(deleteResult.getDeletedCount() > 0) {
                    deleteUsersBatchFromNeo4j(idUsers);
                    mongoSession.commitTransaction();
                    this.deleteCacheInThread(idUsers);
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
        try (Session session = this.neo4jManager.getSession()) {
            session.executeWrite(
                    tx -> tx.run(
                            "OPTIONAL MATCH (r:Reader) WHERE r.mid IN $userIds DETACH DELETE r",
                            parameters("userIds", idUsers)
                    )
            );
        }
    }

    /**
     * @param idUser user's id
     * @return a user associated with the given id or empty if not found
     */
    @Override
    public Optional<User> findById(String idUser) {
        Objects.requireNonNull(idUser);

        User cachedUser = this.cacheService.get(generateCacheKey(idUser), User.class);
        if (cachedUser != null) {
            return Optional.of(cachedUser);
        }

        User user = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idUser)))
                .first();

        if (user != null) {
            this.cacheUser(user);
            return Optional.of(user);
        }

        return Optional.empty();
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

        if (user != null) {
            this.cacheUser(user);
            return Optional.of(user);
        }

        return Optional.empty();
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
                .into(List.of());

        long total = this.mongoCollection
                .countDocuments();

        cacheUserInThread(users);

        return new PageResult<>(users, total, page, size);
    }
}
