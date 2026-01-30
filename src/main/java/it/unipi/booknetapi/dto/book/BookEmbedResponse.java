package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.dto.author.AuthorEmbedResponse;
import it.unipi.booknetapi.dto.genre.GenreEmbedResponse;
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
    private List<AuthorEmbedResponse> authors = new ArrayList<>();
    private List<GenreEmbedResponse> genres = new ArrayList<>();

    public BookEmbedResponse(BookEmbed book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();
        if(book.getAuthors() != null) this.authors = book.getAuthors().stream().map(AuthorEmbedResponse::new).toList();
        if(book.getGenres() != null) this.genres = book.getGenres().stream().map(GenreEmbedResponse::new).toList();
    }

    public BookEmbedResponse(BookRecommendation book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();
        if(book.getAuthors() != null) this.authors = book.getAuthors().stream().map(AuthorEmbedResponse::new).toList();
        if(book.getGenres() != null) this.genres = book.getGenres().stream().map(GenreEmbedResponse::new).toList();
    }
}
