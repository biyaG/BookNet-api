package it.unipi.booknetapi.command.notification;

import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationCreateCommand extends BaseCommand {

    private String title;
    private String message;

    private String idUser;
    private String idEntity;
    private EntityType entityType;

}
