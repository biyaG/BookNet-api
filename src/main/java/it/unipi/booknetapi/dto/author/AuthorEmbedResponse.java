package it.unipi.booknetapi.dto.author;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEmbedResponse {

    private String idAuthor;
    private String name;

    public AuthorEmbedResponse(AuthorEmbed author) {
        this.idAuthor = author.getId().toHexString();
        this.name = author.getName();
    }
}
