package it.unipi.booknetapi.repository.stat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.stat.ActivityStat;
import it.unipi.booknetapi.model.stat.ActivityType;
import it.unipi.booknetapi.model.stat.ChartDataPoint;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Repository
public class AnalyticsRepository implements AnalyticsRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(AnalyticsRepository.class);

    private final MongoClient mongoClient;
    private final MongoCollection<ActivityStat> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;

    public AnalyticsRepository(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("activity_stats", ActivityStat.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }


    /**
     * Updates stats for the Book, the Author, and all Genres in one go.
     */
    public void recordActivity(
            ObjectId bookId, String bookTitle,
            ObjectId authorId, String authorName,
            List<GenreEmbed> genres,
            ActivityType type,
            int ratingValue
    ) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Date date = Date.from(today.atStartOfDay(ZoneOffset.UTC).toInstant());

        if(bookId != null) {
            upsertStat(bookId, "BOOK", bookTitle, date, type, ratingValue);
        }

        if (authorId != null) {
            upsertStat(authorId, "AUTHOR", authorName, date, type, ratingValue);
        }

        if (genres != null) {
            genres.forEach(genre -> {
                upsertStat(genre.getId(), "GENRE", genre.getName(), date, type, ratingValue);
            });
        }
    }

    private void upsertStat(ObjectId entityId, String type, String name, Date date, ActivityType activityType, int rating) {
        String compositeId = entityId.toHexString() + "_" + date.toInstant().toString();

        List<Bson> updates = new ArrayList<>();

        updates.add(Updates.setOnInsert("entityId", entityId));
        updates.add(Updates.setOnInsert("type", type));
        updates.add(Updates.setOnInsert("name", name)); // Store name for easy chart labeling
        updates.add(Updates.setOnInsert("date", date));

        switch (activityType) {
            case READ -> updates.add(Updates.inc("readCount", 1));
            case REVIEW -> updates.add(Updates.inc("reviewCount", 1));
            case RATING -> {
                updates.add(Updates.inc("ratingCount", 1));
                updates.add(Updates.inc("ratingSum", rating));
            }
            case VIEW -> updates.add(Updates.inc("viewCount", 1));
        }

        this.mongoCollection.updateOne(
                Filters.eq("_id", compositeId),
                Updates.combine(updates),
                new UpdateOptions().upsert(true)
        );
    }



    public List<ChartDataPoint> getChartData(ObjectId entityId, Date start, Date end, String granularity) {

        return this.mongoCollection.withDocumentClass(ChartDataPoint.class)
                .aggregate(Arrays.asList(
                        // 1. Filter: Select the relevant daily buckets
                        Aggregates.match(Filters.and(
                                Filters.eq("entityId", entityId),
                                Filters.gte("date", start),
                                Filters.lte("date", end)
                        )),

                        // 2. Group: Roll up Daily buckets into the requested Granularity
                        Aggregates.group(
                                // The result of this becomes the '_id' (which maps to ChartDataPoint.date)
                                new Document("$dateTrunc", new Document("date", "$date").append("unit", granularity)),

                                // Calculate Totals
                                Accumulators.sum("reads", "$readCount"),
                                Accumulators.sum("views", "$viewCount"),
                                Accumulators.sum("reviews", "$reviewCount"),
                                Accumulators.sum("totalRatings", "$ratingCount"),
                                Accumulators.sum("sumRatings", "$ratingSum")
                        ),

                        // 3. Project: Calculate Average Rating
                        Aggregates.project(new Document()
                                .append("reads", 1)
                                .append("views", 1)
                                .append("reviews", 1)
                                .append("avgRating", new Document("$cond", Arrays.asList(
                                        new Document("$eq", Arrays.asList("$totalRatings", 0)),
                                        0.0,
                                        new Document("$divide", Arrays.asList("$sumRatings", "$totalRatings"))
                                )))
                        ),

                        // 4. Sort: Ensure the chart flows chronologically
                        Aggregates.sort(Sorts.ascending("_id"))
                ))
                .into(new ArrayList<>());
    }

}
