package it.unipi.booknetapi.model.notification;

import it.unipi.booknetapi.command.notification.NotificationCreateCommand;
import it.unipi.booknetapi.model.fetch.EntityType;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @BsonId
    private ObjectId id;

    private String title;
    private String message;

    private ObjectId userId;
    private ObjectId entityId;
    private EntityType entityType;

    private Date createdAt;

    private Boolean read;

    public Notification(NotificationCreateCommand command) {
        this.title = command.getTitle();
        this.message = command.getMessage();
        this.userId = new ObjectId(command.getIdUser());
        this.entityId = new ObjectId(command.getIdEntity());
        this.entityType = command.getEntityType();

        this.createdAt = new Date();
        this.read = false;
    }
}
