package it.unipi.booknetapi.analytics.service;

import it.unipi.booknetapi.analytics.dto.AnalyticsOverviewDTO;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    // overview endpoint logic
    public AnalyticsOverviewDTO getOverview() {
        return new AnalyticsOverviewDTO(
                getBooksCount(),
                getAuthorsCount(),
                getUsersCount(),
                getInteractionsCount()
        );
    }
//05955547
    // mock values for now (replace later with DB)
    public long getBooksCount() {
        return 0;
    }

    public long getAuthorsCount() {
        return 0;
    }

    public long getUsersCount() {
        return 0;
    }

    public long getInteractionsCount() {
        return 0;
    }
}
