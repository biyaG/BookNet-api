package it.unipi.booknetapi.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsOverviewDTO {

    private long totalBooks;
    private long totalAuthors;
    private long totalUsers;
    private long totalInteractions;
}
