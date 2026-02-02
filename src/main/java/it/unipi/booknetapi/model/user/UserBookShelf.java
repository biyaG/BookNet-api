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

    public UserBookShelf(ReviewerRead read) {
        this.book = read.getBook();

        boolean isFinished = read.getIsRead() == null || read.getIsRead();
        this.status = isFinished ? BookShelfStatus.FINISHED : BookShelfStatus.READING;

        this.dateAdded = read.getStartedAt() != null ? read.getStartedAt() : read.getReadAt();
        this.dateUpdated = read.getReadAt() != null ? read.getReadAt() : read.getStartedAt();
    }
}
