package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.model.book.Book;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class BookResponse extends BookSimpleResponse{
    private List<AuthorSimpleResponse> authors = List.of();
    private List<GenreResponse> genres = List.of();

    public BookResponse(Book book) {
        super(book);

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

