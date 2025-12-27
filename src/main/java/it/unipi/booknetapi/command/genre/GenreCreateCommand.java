package it.unipi.booknetapi.command.genre;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GenreCreateCommand extends BaseCommand {

    private String name;
}
