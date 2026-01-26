package it.unipi.booknetapi.repository.notification;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import it.unipi.booknetapi.model.notification.Notification;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class NotificationRepository implements NotificationRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(NotificationRepository.class);

    private final MongoCollection<Notification> mongoCollection;

    public NotificationRepository(MongoDatabase mongoDatabase) {
        this.mongoCollection = mongoDatabase.getCollection("notifications", Notification.class);
    }


    /**
     * @param notification
     * @return
     */
    @Override
    public Notification insert(Notification notification) {
        Objects.requireNonNull(notification);

        logger.debug("[NOTIFICATION] [REPOSITORY] [INSERT] notification: {}", notification);

        InsertOneResult result = this.mongoCollection.insertOne(notification);

        if(result.wasAcknowledged()) return notification;

        return null;
    }

    /**
     * @param idNotification
     * @return
     */
    @Override
    public boolean read(String idNotification, Boolean read) {
        Objects.requireNonNull(idNotification);
        if(read == null) read = true;

        if(!ObjectId.isValid(idNotification)) return false;

        logger.debug("[NOTIFICATION] [REPOSITORY] [UPDATE] [READ] notification: {} - read: {}", idNotification, read);

        UpdateResult updateResult = this.mongoCollection
                .updateOne(
                        Filters.eq("_id", new ObjectId(idNotification)),
                        Updates.set("read", read)
                );

        return updateResult.getModifiedCount() > 0;
    }

    /**
     * @param idNotification
     * @return
     */
    @Override
    public boolean delete(String idNotification) {
        Objects.requireNonNull(idNotification);

        if(!ObjectId.isValid(idNotification)) return true;

        logger.debug("[NOTIFICATION] [REPOSITORY] [DELETE] notification: {}", idNotification);

        DeleteResult deleteResult = this.mongoCollection.deleteOne(Filters.eq("_id", new ObjectId(idNotification)));

        return deleteResult.getDeletedCount() > 0;
    }

    /**
     * @param idNotifications
     * @return
     */
    @Override
    public boolean delete(List<String> idNotifications) {
        Objects.requireNonNull(idNotifications);

        List<ObjectId> ids = idNotifications.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        if(ids.isEmpty()) return true;

        logger.debug("[NOTIFICATION] [REPOSITORY] [DELETE MANY] notification size: {}", ids.size());

        DeleteResult deleteResult = this.mongoCollection.deleteMany(Filters.in("_id", ids));

        return deleteResult.getDeletedCount() > 0;
    }

    /**
     * @param idNotification
     * @return
     */
    @Override
    public Optional<Notification> findById(String idNotification) {
        Objects.requireNonNull(idNotification);

        if(!ObjectId.isValid(idNotification)) return Optional.empty();

        logger.debug("[NOTIFICATION] [REPOSITORY] [FIND BY ID] notification: {}", idNotification);

        Notification notification = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idNotification)))
                .first();

        return notification != null ? Optional.of(notification) : Optional.empty();
    }

    /**
     * @param idNotifications
     * @return
     */
    @Override
    public List<Notification> findByIds(List<String> idNotifications) {
        Objects.requireNonNull(idNotifications);

        if(idNotifications.isEmpty()) return new ArrayList<>();

        List<ObjectId> ids = idNotifications.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        if(ids.isEmpty()) return new ArrayList<>();

        logger.debug("[NOTIFICATION] [REPOSITORY] [FIND BY IDS] notification size: {}", ids.size());

        return this.mongoCollection
                .find(Filters.in("_id", ids))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
    }

    /**
     * @param idNotifications
     * @return
     */
    @Override
    public List<Notification> findByOIds(List<ObjectId> idNotifications) {
        Objects.requireNonNull(idNotifications);

        if(idNotifications.isEmpty()) return new ArrayList<>();

        logger.debug("[NOTIFICATION] [REPOSITORY] [FIND BY IDS] notification size: {}", idNotifications.size());

        return this.mongoCollection
                .find(Filters.in("_id", idNotifications))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
    }

    /**
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<Notification> findAll(int page, int pageSize) {
        logger.debug("[REPOSITORY] [NOTIFICATION] [FIND] [ALL] page: {}, page size: {}", page, pageSize);

        int skip = page * pageSize;

        List<Notification> notifications = this.mongoCollection
                .find()
                .sort(Sorts.descending("createdAt"))
                .skip(skip)
                .limit(pageSize)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments();

        return new PageResult<>(notifications, total, page, pageSize);
    }

    /**
     * @param idUser
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<Notification> findAll(String idUser, int page, int pageSize) {
        Objects.requireNonNull(idUser);

        if(!ObjectId.isValid(idUser)) return new PageResult<>(new ArrayList<>(), 0, page, pageSize);

        logger.debug("[REPOSITORY] [NOTIFICATION] [FIND] [BY USER ID] user: {}, page: {}, page size: {}", idUser, page, pageSize);

        int skip = page * pageSize;

        List<Notification> notifications = this.mongoCollection
                .find(Filters.eq("userId", new ObjectId(idUser)))
                .sort(Sorts.descending("createdAt"))
                .skip(skip)
                .limit(pageSize)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments(Filters.eq("userId", new ObjectId(idUser)));

        return new PageResult<>(notifications, total, page, pageSize);
    }

    /**
     * @param idUser
     * @param read
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult<Notification> findAll(String idUser, Boolean read, int page, int pageSize) {
        Objects.requireNonNull(idUser);

        if(read == null) read = false;

        if(!ObjectId.isValid(idUser)) return new PageResult<>(new ArrayList<>(), 0, page, pageSize);

        logger.debug("[REPOSITORY] [NOTIFICATION] [FIND] [BY USER ID] user: {}, page: {}, page size: {}", idUser, page, pageSize);

        int skip = page * pageSize;

        List<Notification> notifications = this.mongoCollection
                .find(
                        Filters.and(
                                Filters.eq("userId", new ObjectId(idUser)),
                                Filters.eq("read", read)
                        )
                )
                .sort(Sorts.descending("createdAt"))
                .skip(skip)
                .limit(pageSize)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments(
                Filters.and(
                        Filters.eq("userId", new ObjectId(idUser)),
                        Filters.eq("read", read)
                )
        );

        return new PageResult<>(notifications, total, page, pageSize);
    }

}
