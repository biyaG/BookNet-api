package it.unipi.booknetapi.model.user;

import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerRead {

    private ObjectId userId;
    private ObjectId bookId;
    private Boolean isRead;
    private Date readAt;
    private Date startedAt;

}
