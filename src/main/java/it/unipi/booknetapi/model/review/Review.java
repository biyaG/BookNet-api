package it.unipi.booknetapi.model.review;

import it.unipi.booknetapi.model.user.UserEmbed;
import it.unipi.booknetapi.shared.model.Source;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @BsonId
    private ObjectId id;

    private ObjectId bookId;
    private UserEmbed user;

    private Float rating;
    private String comment;
    private Date dateAdded;

    private Source source;

}
