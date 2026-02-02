package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerRead {

    private ObjectId userId;
    private BookEmbed book;
    private Boolean isRead;
    private Date readAt;
    private Date startedAt;

}
