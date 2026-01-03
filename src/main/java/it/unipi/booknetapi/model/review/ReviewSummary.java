package it.unipi.booknetapi.model.review;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {
    private float rating;
    private int count;
}
