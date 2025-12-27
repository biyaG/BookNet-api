package it.unipi.booknetapi.command.author;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorGetCommand extends BaseCommand {

    private String id;

}
