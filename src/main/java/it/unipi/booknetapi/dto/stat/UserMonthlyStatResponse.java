package it.unipi.booknetapi.dto.stat;

import it.unipi.booknetapi.model.stat.UserMonthlyStat;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMonthlyStatResponse {

    private String idUser;

    private Integer year;
    private Integer month;

    private Integer totalBooksRead = 0;
    private Integer totalPagesRead = 0;

    private Map<String, Integer> genreDistribution = new HashMap<>();

    private List<ReadEventResponse> readingLog = new ArrayList<>();

    public UserMonthlyStatResponse(UserMonthlyStat userMonthlyStat) {
        this.idUser = userMonthlyStat.getUserId().toHexString();
        this.year = userMonthlyStat.getYear();
        this.month = userMonthlyStat.getMonth();
        this.totalBooksRead = userMonthlyStat.getTotalBooksRead();
        this.totalPagesRead = userMonthlyStat.getTotalPagesRead();
        this.genreDistribution = userMonthlyStat.getGenreDistribution();
        this.readingLog = userMonthlyStat.getReadingLog().stream().map(ReadEventResponse::new).toList();
    }

}
