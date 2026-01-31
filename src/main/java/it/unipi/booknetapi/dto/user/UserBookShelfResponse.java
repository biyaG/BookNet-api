package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.model.user.BookShelfStatus;
import it.unipi.booknetapi.model.user.UserBookShelf;
import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBookShelfResponse {

    private BookEmbedResponse book;
    private BookShelfStatus status;

    private Date dateAdded;
    private Date dateUpdated;

    public UserBookShelfResponse(UserBookShelf userBook) {

        this.book = new BookEmbedResponse(userBook.getBook());
        this.status = userBook.getStatus();
        this.dateAdded = userBook.getDateAdded();
        this.dateUpdated = userBook.getDateUpdated();
    }
}
