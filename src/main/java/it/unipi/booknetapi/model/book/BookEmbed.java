package it.unipi.booknetapi.model.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookEmbed {

    private ObjectId id;
    private String title;
    private String description;
    private Integer numPage;
    private FormatTypeEnum format;
    private List<String> images = new ArrayList<>();;
//    private String externalBookId; //We should have this because that is how we can link the similarbooks

    public BookEmbed(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();
    }

}
