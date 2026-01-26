package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.notification.NotificationEmbed;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(key = "role", value = "Admin")
public class Admin extends InternalUser {

    private Role role = Role.Admin;

    private List<NotificationEmbed> lastNotifications = new ArrayList<>();
    private List<ObjectId> notifications = new ArrayList<>();

}
