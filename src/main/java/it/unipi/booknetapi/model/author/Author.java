package it.unipi.booknetapi.model.author;

import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public class Author {
    private ObjectId _id;
    private String name;
    private String description;
    private String image_url;
    private BookEmbed books;
}
