package it.unipi.booknetapi.command.genre;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenreDeleteCommand extends BaseCommand {

    private String id;

}
