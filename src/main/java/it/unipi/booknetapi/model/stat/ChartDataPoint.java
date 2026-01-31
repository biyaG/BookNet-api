package it.unipi.booknetapi.model.stat;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {

    /// The start date of the window (Week start, Month start, etc.)
    @BsonId
    private Date date;

    private Integer reads;
    private Integer reviews;
    private Double avgRating;

}
