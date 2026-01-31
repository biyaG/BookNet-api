package it.unipi.booknetapi.repository.user;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ReaderStatsRepository {

    public List<Document> getAggregatedStatisticsReader( MongoCollection<Document> collection, String dateFormat){
        List<Document> pipeline = Arrays.asList(
                new Document("$project", new Document()
                        .append("idUser", "$idUser")
                        .append("userName", "$userName")

                        .append("totalBooksRead",
                                new Document("$size",
                                        new Document("$filter",
                                                new Document("input", "$shelf")
                                                        .append("as", "item")
                                                        .append("cond", new Document("eq", List.of("$$item.status", "READ"))
                                                        )
                                        )
                                )
                        )

                        // 2. Total pages read
                        .append("totalPagesRead",
                                new Document("$reduce",
                                        new Document("input", "$shelf")
                                                .append("initialValue", 0)
                                                .append("in",
                                                        new Document("$cond", List.of(
                                                                new Document("$eq", List.of("$$this.status", "READ")),
                                                                new Document("$add", List.of(
                                                                        "$$value",
                                                                        new Document("$ifNull", List.of("$$this.book.numPage", 0))
                                                                )),
                                                                "$$value"
                                                        ))
                                                )
                                )
                        )

                        // 3. Genre distribution
                        .append("genreDistribution",
                                new Document("$reduce",
                                        new Document("input", "$shelf")
                                                .append("initialValue", List.of())
                                                .append("in",
                                                        new Document("$cond", List.of(
                                                                new Document("$eq", List.of("$$this.status", "READ")),
                                                                new Document("$concatArrays", List.of(
                                                                        "$$value",
                                                                        new Document("$ifNull", List.of("$$this.book.genres", List.of()))
                                                                )),
                                                                "$$value"
                                                        ))
                                                )
                                )
                        )

                        // 4. Activity timeline
                        .append("activityTimeline",
                                new Document("$map",
                                        new Document("input",
                                                new Document("$filter",
                                                        new Document("input", "$shelf")
                                                                .append("as", "s")
                                                                .append("cond",
                                                                        new Document("$eq", List.of("$$s.status", "READ"))
                                                                )
                                                )
                                        )
                                                .append("as", "item")
                                                .append("in",
                                                        new Document("date",
                                                                new Document("$dateToString",
                                                                        new Document("format", dateFormat)
                                                                                .append("date", "$$item.dateAdded")
                                                                )
                                                        )
                                                                .append("bookId", "$$item.book.idBook")
                                                                .append("title", "$$item.book.title")
                                                )
                                )
                        )
                )
        );

        return collection.aggregate(pipeline).into(new ArrayList<>());
    }


}
