package it.unipi.booknetapi.repository.stat;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.stat.UserMonthlyStat;
import it.unipi.booknetapi.model.stat.UserYearlyStat;
import org.bson.types.ObjectId;

import java.util.List;

public interface UserMonthlyStatRepositoryInterface {

    UserMonthlyStat getMonthlyStats(String userId, int year, int month);

    List<UserMonthlyStat> getMonthlyStats(String userId, int year);

    void addReadEvent(ObjectId userId, BookEmbed book);

    public UserYearlyStat getYearlyStats(String userId, int year);

}
