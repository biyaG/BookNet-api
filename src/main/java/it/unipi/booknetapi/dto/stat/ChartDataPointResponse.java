package it.unipi.booknetapi.dto.stat;

import it.unipi.booknetapi.model.stat.ChartDataPoint;
import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPointResponse {

    private Date date;

    private Integer reads;
    private Integer views;
    private Integer reviews;
    private Double avgRating;

    public ChartDataPointResponse(ChartDataPoint chartDataPoint) {
        this.date = chartDataPoint.getDate();
        this.reads = chartDataPoint.getReads();
        this.views = chartDataPoint.getViews();
        this.reviews = chartDataPoint.getReviews();
        this.avgRating = chartDataPoint.getAvgRating();
    }

}
