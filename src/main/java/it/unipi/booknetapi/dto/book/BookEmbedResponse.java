package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.BookRecommendation;
import it.unipi.booknetapi.model.book.FormatTypeEnum;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookEmbedResponse {

    private String idBook;
    private String title;
    private String description;
    private Integer numPage;
    private FormatTypeEnum format;
    private List<String> images = new ArrayList<>();

    public BookEmbedResponse(BookEmbed book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();
    }

    public BookEmbedResponse(BookRecommendation book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();
    }
}
