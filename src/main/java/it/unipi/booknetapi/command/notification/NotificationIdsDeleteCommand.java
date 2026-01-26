package it.unipi.booknetapi.command.notification;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationIdsDeleteCommand extends BaseCommand {

    private String idUser;
    private List<String> ids;

}
