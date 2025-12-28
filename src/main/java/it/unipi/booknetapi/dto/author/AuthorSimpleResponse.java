package it.unipi.booknetapi.dto.author;

import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSimpleResponse {

    private String id;

    private String name;
    private String description;
    private String imageUrl;

    public AuthorSimpleResponse(Author author) {
        this.id = author.getId().toHexString();
        this.name = author.getName();
        this.description = author.getDescription();
        this.imageUrl = author.getImageUrl();
    }

    public AuthorSimpleResponse(AuthorEmbed authorEmbed) {
    }
}
