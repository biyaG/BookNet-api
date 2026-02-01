package it.unipi.booknetapi.dto.notification;

import it.unipi.booknetapi.model.notification.Notification;
import it.unipi.booknetapi.model.notification.NotificationEmbed;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEmbedResponse {

    private String idNotification;

    private String title;
    private String message;

    private Date createdAt;

    public NotificationEmbedResponse(NotificationEmbed notification) {
        this.idNotification = notification.getId().toHexString();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
    }

    public NotificationEmbedResponse(Notification notification) {
        this.idNotification = notification.getId().toHexString();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
    }


}
