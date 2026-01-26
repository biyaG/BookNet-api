package it.unipi.booknetapi.model.notification;

import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEmbed {

    private ObjectId id;

    private String title;
    private String message;

    private Date createdAt;

    public NotificationEmbed(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
    }

}
