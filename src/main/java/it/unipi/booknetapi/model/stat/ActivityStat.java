package it.unipi.booknetapi.model.stat;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStat {

    @BsonId
    private String id; // Composite: "ENTITY_ID_YYYY-MM-DD"

    // INDEXED FIELDS
    private ObjectId entityId;  // The ID of the Book, Author, or Genre
    private String type;        // "BOOK", "AUTHOR", "GENRE"
    private String name;        // "Dune", "Frank Herbert", or "Sci-Fi"
    private Date date;          // Truncated to midnight (UTC)

    // AGGREGATES
    private int readCount;
    private int viewCount;
    private int reviewCount;
    private int ratingCount;
    private long ratingSum;

}
