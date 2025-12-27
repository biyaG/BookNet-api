package it.unipi.booknetapi.command.author;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorIdsListCommand extends BaseCommand {

    private List<String> ids;

}
