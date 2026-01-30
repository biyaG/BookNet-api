package it.unipi.booknetapi.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class ReaderStatusReport {
    private String id; // Maps to _id
    private String name;
    private Integer totalBooksRead;
    private Integer totalPagesRead;
    private List<String> genreDistribution;
    private List<ActivityPoint> activityTimeline;

    // Standard Getters and Setters

    public static class ActivityPoint {
        private String date;
        private String bookId;
        private String title;

        // Getters and Setters
    }
}
