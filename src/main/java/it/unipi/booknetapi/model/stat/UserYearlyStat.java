package it.unipi.booknetapi.model.stat;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserYearlyStat {

    @BsonId // Maps the aggregation grouping key ('_id') to this field
    private ObjectId userId;

    private Integer yearlyBooks;
    private Integer yearlyPages;
    private Integer topMonth;

}
