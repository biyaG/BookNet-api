package it.unipi.booknetapi.dto.notification;

import it.unipi.booknetapi.model.notification.Notification;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationResponse extends NotificationEmbedResponse {

    private Boolean read;

    public NotificationResponse(Notification notification) {
        super(notification);
        this.read = notification.getRead();
    }

}
