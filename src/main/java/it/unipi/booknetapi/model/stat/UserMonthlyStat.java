package it.unipi.booknetapi.model.stat;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMonthlyStat {

    @Id @BsonId
    private String id; // Composite String ID: "userId_year_month"

    // SHARD KEY FIELDS (Compound Index)
    private ObjectId userId;
    private Integer year;
    private Integer month; // 1 to 12

    // AGGREGATED STATS
    private Integer totalBooksRead = 0;
    private Integer totalPagesRead = 0;

    // DISTRIBUTION (Map for Genres)
    // Key: Genre Name, Value: Count
    private Map<String, Integer> genreDistribution = new HashMap<>();

    // DETAILED HISTORY (The "Bucket")
    // Stores the individual events inside this time bucket
    private List<ReadEvent> readingLog = new ArrayList<>();


}
