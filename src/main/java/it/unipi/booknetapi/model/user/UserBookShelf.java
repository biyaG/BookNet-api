package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookShelf {

    private BookEmbed book;
    private BookShelfStatus status;

    private Date dateAdded;
    private Date dateUpdated;

}
