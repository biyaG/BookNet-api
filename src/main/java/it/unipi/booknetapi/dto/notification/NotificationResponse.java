package it.unipi.booknetapi.dto.notification;

import it.unipi.booknetapi.model.notification.Notification;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String idNotification;

    private String title;
    private String message;

    private Date createdAt;

    private Boolean read;


    public NotificationResponse(Notification notification) {
        this.idNotification = notification.getId().toHexString();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
        this.read = notification.getRead();
    }
}
