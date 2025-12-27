package it.unipi.booknetapi.model.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookEmbed {

    private ObjectId id;
    private String title;
    private String description;
    private List<String> images;

    public BookEmbed(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.images = book.getImages();
    }

}
