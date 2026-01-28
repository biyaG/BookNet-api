package it.unipi.booknetapi.dto.author;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.author.AuthorStats;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEmbedResponse {

    private String idAuthor;
    private String name;

    public AuthorEmbedResponse(AuthorEmbed author) {
        this.idAuthor = author.getId().toHexString();
        this.name = author.getName();
    }

    public AuthorEmbedResponse(AuthorStats author) {
        this.idAuthor = author.getId();
        this.name = author.getName();
    }
}
