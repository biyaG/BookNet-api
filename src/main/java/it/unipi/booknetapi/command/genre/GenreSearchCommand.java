package it.unipi.booknetapi.command.genre;

import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenreSearchCommand extends BaseCommand {

    private String name;

    private PaginationRequest pagination;

}
