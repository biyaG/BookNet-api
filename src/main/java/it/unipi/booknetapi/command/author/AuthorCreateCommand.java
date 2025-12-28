package it.unipi.booknetapi.command.author;

import it.unipi.booknetapi.dto.author.AuthorCreateRequest;
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

    private ExternalId externalId = new ExternalId();

    public AuthorCreateCommand(AuthorCreateRequest request) {
        this.name = request.getName();
        this.description = request.getDescription();
        this.imageUrl = request.getImageUrl();

        this.externalId = new ExternalId();
    }

}
