package it.unipi.booknetapi.dto.stat;

import it.unipi.booknetapi.model.stat.UserYearlyStat;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserYearlyStatResponse {

    private String userId;

    private Integer yearlyBooks = 0;
    private Integer yearlyPages = 0;
    private Integer topMonth = 1;

    public UserYearlyStatResponse(UserYearlyStat userYearlyStat) {
        this.userId = userYearlyStat.getUserId().toHexString();
        this.yearlyBooks = userYearlyStat.getYearlyBooks();
        this.yearlyPages = userYearlyStat.getYearlyPages();
        this.topMonth = userYearlyStat.getTopMonth();
    }

}
