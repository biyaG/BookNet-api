package it.unipi.booknetapi.command.genre;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenreIdsDeleteCommand extends BaseCommand {

    private List<String> ids;

}
