package it.unipi.booknetapi.repository.notification;

import it.unipi.booknetapi.model.notification.Notification;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryInterface {

    Notification insert(Notification notification);

    boolean read(String idNotification, Boolean read);

    boolean delete(String idNotification);
    boolean delete(List<String> idNotifications);

    Optional<Notification> findById(String idNotification);

    List<Notification> findByIds(List<String> idNotifications);
    List<Notification> findByOIds(List<ObjectId> idNotifications);

    PageResult<Notification> findAll(int page, int pageSize);
    PageResult<Notification> findAll(String idUser, int page, int pageSize);
    PageResult<Notification> findAll(String idUser, Boolean read, int page, int pageSize);

}
