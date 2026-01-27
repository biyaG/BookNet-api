package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.FormatTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSimpleResponse {

    private String idBook;
    private String title;
    private String description;
    private Integer numPage;
    private FormatTypeEnum format;
    private List<String> images;
    private List<AuthorSimpleResponse> authors = List.of();
    private List<GenreResponse> genres = List.of();

    public BookSimpleResponse(Book book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();

        if (book.getAuthors() != null) {
            this.authors = book.getAuthors().stream()
                    .map(AuthorSimpleResponse::new)
                    .toList();
        }

        if(book.getGenres() != null){
            this.genres = book.getGenres().stream()
                    .map(GenreResponse :: new)
                    .toList();
        }
    }

    public BookSimpleResponse(BookEmbed book) {
        this.idBook = book.getId().toHexString();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.numPage = book.getNumPage();
        this.format = book.getFormat();
        this.images = book.getImages();

        if (book.getAuthors() != null) {
            this.authors = book.getAuthors().stream()
                    .map(AuthorSimpleResponse::new)
                    .toList();
        }

        if(book.getGenres() != null){
            this.genres = book.getGenres().stream()
                    .map(GenreResponse :: new)
                    .toList();
        }
    }

}
