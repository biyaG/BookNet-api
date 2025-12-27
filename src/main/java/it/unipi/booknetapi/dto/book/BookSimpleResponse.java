package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSimpleResponse {

    private String id;
    private String title;
    private String description;
    private List<String> images;

    public BookSimpleResponse(Book book) {
        this.id = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.images = book.getImages();
    }

    public BookSimpleResponse(BookEmbed book) {
        this.id = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.images = book.getImages();
    }

}
