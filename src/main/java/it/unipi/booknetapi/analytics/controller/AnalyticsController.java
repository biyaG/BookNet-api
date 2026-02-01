package it.unipi.booknetapi.analytics.controller;

import it.unipi.booknetapi.analytics.dto.AnalyticsOverviewDTO;
import it.unipi.booknetapi.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // =========================
    // OVERVIEW
    // =========================
    @GetMapping("/overview")
    public AnalyticsOverviewDTO getOverview() {
        return analyticsService.getOverview();
    }

    // =========================
    // COUNTS
    // =========================
    @GetMapping("/books/count")
    public long getBooksCount() {
        return analyticsService.getBooksCount();
    }

    @GetMapping("/authors/count")
    public long getAuthorsCount() {
        return analyticsService.getAuthorsCount();
    }

    @GetMapping("/users/count")
    public long getUsersCount() {
        return analyticsService.getUsersCount();
    }

    @GetMapping("/interactions/count")
    public long getInteractionsCount() {
        return analyticsService.getInteractionsCount();
    }
}
