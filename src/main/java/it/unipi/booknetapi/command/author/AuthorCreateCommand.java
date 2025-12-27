package it.unipi.booknetapi.command.author;

import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorCreateCommand extends BaseCommand {

    private String name;
    private String description;
    private String imageUrl;

    private ExternalId externalId;

}
