package it.unipi.booknetapi.dto.author;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.author.AuthorStats;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorStatResponse extends AuthorEmbedResponse {

    private Long count;

    public AuthorStatResponse(AuthorEmbed author) {
        super(author);
    }

    public AuthorStatResponse(AuthorStats author) {
        super(author);

        this.count = author.getCount();
    }
}
