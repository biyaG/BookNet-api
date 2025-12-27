package it.unipi.booknetapi.model.author;

import it.unipi.booknetapi.command.author.AuthorCreateCommand;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Author {

    @Id
    @BsonId
    private ObjectId id;

    private String name;
    private String description;
    private String imageUrl;

    private List<BookEmbed> books = List.of();

    private ExternalId externalId = new ExternalId();

    public Author(AuthorCreateCommand command) {
        this.name = command.getName();
        this.description = command.getDescription();
        this.imageUrl = command.getImageUrl();

        this.externalId = command.getExternalId();
    }
}
