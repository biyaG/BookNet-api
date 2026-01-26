package it.unipi.booknetapi.command.notification;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationReadCommand extends BaseCommand {

    private String id;
    private Boolean read;

}
