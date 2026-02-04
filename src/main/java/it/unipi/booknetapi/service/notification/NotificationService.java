package it.unipi.booknetapi.service.notification;

import it.unipi.booknetapi.command.notification.*;
import it.unipi.booknetapi.dto.notification.NotificationEmbedResponse;
import it.unipi.booknetapi.dto.notification.NotificationResponse;
import it.unipi.booknetapi.model.notification.Notification;
import it.unipi.booknetapi.model.notification.NotificationEmbed;
import it.unipi.booknetapi.model.user.Admin;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.repository.notification.NotificationRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }


    public NotificationResponse get(NotificationGetCommand command) {
        if(command.getId() == null) return null;

        Notification notification = this.notificationRepository.findById(command.getId()).orElse(null);
        if(notification == null) return null;

        return new NotificationResponse(notification);
    }

    public PageResult<NotificationResponse> get(NotificationGetByUserCommand command) {
        if(command.getIdUser() == null) return null;

        List<ObjectId> idNotifications = this.userRepository.getNotificationsIds(command.getIdUser());

        if(idNotifications.isEmpty()) return new PageResult<>(List.of(), 0, command.getPagination().getPage(), command.getPagination().getSize());

        int totalElements = idNotifications.size();
        int skip = command.getPagination().getPage() * command.getPagination().getSize();
        if(skip >= totalElements) return new PageResult<>(List.of(), 0, command.getPagination().getPage(), command.getPagination().getSize());

        int limit = Math.min(command.getPagination().getSize(), totalElements - skip);
        List<ObjectId> idNotificationsPage = idNotifications.subList(skip, skip + limit);

        List<Notification> notifcations = this.notificationRepository.findByOIds(idNotificationsPage);

        return new PageResult<>(
                notifcations.stream().map(NotificationResponse::new).toList(),
                totalElements,
                command.getPagination().getPage(),
                command.getPagination().getSize()
        );
    }

    public List<NotificationEmbedResponse> get(NotificationLastestCommand command) {
        if(command.getIdUser() == null) return null;

        User user = this.userRepository.findById(command.getIdUser()).orElse(null);
        if(user == null) return null;

        if(user instanceof Admin admin) {
            if(admin.getLastNotifications() == null || admin.getLastNotifications().isEmpty()) return new ArrayList<>();

            return admin.getLastNotifications().stream().map(NotificationEmbedResponse::new).toList();
        }

        return null;
    }

    public NotificationResponse add(NotificationCreateCommand command) {
        if(command.getIdUser() == null) return null;
        User user = this.userRepository.findById(command.getIdUser()).orElse(null);

        if(user instanceof Admin admin) {
            Notification notification = this.notificationRepository.insert(new Notification(command));
            if(notification != null) {
                this.userRepository.addNotification(command.getIdUser(), new NotificationEmbed(notification));

                return new NotificationResponse(notification);
            }
        }

        return null;
    }

    public boolean read(NotificationReadCommand command) {
        if(command.getId() == null) return false;

        return this.notificationRepository.read(command.getId(), command.getRead());
    }

    public boolean delete(NotificationDeleteCommand command) {
        if(command.getId() == null || command.getIdUser() == null) return false;

        boolean deleted = this.notificationRepository.delete(command.getId());
        if(deleted) {
            this.userRepository.deleteNotification(command.getIdUser(), command.getId());
        }

        return deleted;
    }

    public boolean delete(NotificationIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;

        boolean deleted = this.notificationRepository.delete(command.getIds());
        if(deleted) {
            this.userRepository.deleteNotification(command.getIdUser(), command.getIds());
        }

        return deleted;
    }

}
