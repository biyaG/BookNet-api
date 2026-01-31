package it.unipi.booknetapi.repository.stat;

import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.stat.ActivityType;
import it.unipi.booknetapi.model.stat.ChartDataPoint;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

public interface AnalyticsRepositoryInterface {

    void recordActivity(
            ObjectId bookId, String bookTitle,
            ObjectId authorId, String authorName,
            List<GenreEmbed> genres,
            ActivityType type, int ratingValue
    );

    List<ChartDataPoint> getChartData(
            ObjectId entityId,
            Date start, Date end,
            String granularity
    );

}
