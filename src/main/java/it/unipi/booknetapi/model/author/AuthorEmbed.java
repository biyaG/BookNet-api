package it.unipi.booknetapi.model.author;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEmbed {

    private ObjectId id;
    private String name;

    public AuthorEmbed(Author author) {
        this.id = author.getId();
        this.name = author.getName();
    }

}
