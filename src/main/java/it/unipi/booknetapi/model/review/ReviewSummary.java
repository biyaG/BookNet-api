package it.unipi.booknetapi.model.review;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {
    private Float rating;
    private Integer count;
}
