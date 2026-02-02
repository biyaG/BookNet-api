package it.unipi.booknetapi.repository.stat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.stat.ReadEvent;
import it.unipi.booknetapi.model.stat.UserMonthlyStat;
import it.unipi.booknetapi.model.stat.UserYearlyStat;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Repository
public class UserMonthlyStatRepository implements UserMonthlyStatRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(UserMonthlyStatRepository.class);

    private final MongoClient mongoClient;
    private final MongoCollection<UserMonthlyStat> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;

    public UserMonthlyStatRepository(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("user_monthly_stats", UserMonthlyStat.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }


    @Override
    public UserMonthlyStat getMonthlyStats(String userId, int year, int month) {
        if(userId == null || !ObjectId.isValid(userId)) return null;

        String compositeId = userId + "_" + year + "_" + month;

        return this.mongoCollection.find(Filters.eq("_id", compositeId)).first();
    }

    @Override
    public List<UserMonthlyStat> getMonthlyStats(String userId, int year) {
        if(userId == null || !ObjectId.isValid(userId)) return null;

        List<UserMonthlyStat> results = new ArrayList<>();

        this.mongoCollection.find(
                        Filters.and(
                                Filters.eq("userId", new ObjectId(userId)),
                                Filters.eq("year", year)
                        ))
                .sort(Sorts.ascending("month")) // Order by Jan -> Dec
                .into(results);

        return results;
    }

    @Override
    public void addReadEvent(
            ObjectId userId,
            BookEmbed book
    ) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(book);
        // Objects.requireNonNull(genres);

        logger.debug("[REPOSITORY] [USER MONTHLY STAT] [ADD READ EVENT] user id: {}, book id: {}", userId.toHexString(), book.getId().toHexString());

        LocalDate nowLocal = LocalDate.now();
        int year = nowLocal.getYear();
        int month = nowLocal.getMonthValue();
        String compositeId = userId.toHexString() + "_" + year + "_" + month;

        ReadEvent newEvent = ReadEvent.builder()
                .book(book)
                .pages(book.getNumPage())
                .genres(book.getGenres())
                .dateRead(Date.from(nowLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .build();

        List<Bson> updates = new ArrayList<>();

        updates.add(Updates.inc("totalBooksRead", 1));
        updates.add(Updates.inc("totalPagesRead", book.getNumPage() != null ? book.getNumPage() : 0));

        if(book.getGenres() != null && !book.getGenres().isEmpty()) {
            for (GenreEmbed genre : book.getGenres()) {
                updates.add(Updates.inc("genreDistribution." + genre.getName(), 1));
            }
        }

        updates.add(Updates.push("readingLog", newEvent));

        updates.add(Updates.setOnInsert("userId", userId));
        updates.add(Updates.setOnInsert("year", year));
        updates.add(Updates.setOnInsert("month", month));

        this.mongoCollection.updateOne(
                Filters.eq("_id", compositeId),
                Updates.combine(updates),
                new UpdateOptions().upsert(true)
        );
    }


    @Override
    public UserYearlyStat getYearlyStats(String userId, int year) {
        if(userId == null || !ObjectId.isValid(userId)) return null;

        return this.mongoCollection.withDocumentClass(UserYearlyStat.class)
                .aggregate(Arrays.asList(
                        Aggregates.match(Filters.and(
                                Filters.eq("userId", new ObjectId(userId)),
                                Filters.eq("year", year)
                        )),

                        Aggregates.group("$userId",
                                Accumulators.sum("yearlyBooks", "$totalBooksRead"),
                                Accumulators.sum("yearlyPages", "$totalPagesRead"),
                                Accumulators.max("topMonth", "$totalBooksRead")
                        )
                ))
                .first(); // Returns null if no stats found for that year
    }

}
