package it.unipi.booknetapi.dto.author;

import it.unipi.booknetapi.dto.book.BookSimpleResponse;
import it.unipi.booknetapi.model.author.Author;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorResponse extends AuthorSimpleResponse {

    private List<BookSimpleResponse> books = List.of();

    public AuthorResponse(Author author) {
        super(author);

        if(author.getBooks() != null) {
            this.books = author.getBooks().stream()
                    .map(BookSimpleResponse::new)
                    .toList();
        }
    }

}
