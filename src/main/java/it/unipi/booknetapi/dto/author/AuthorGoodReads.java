package it.unipi.booknetapi.dto.author;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorGoodReads {

    @JsonProperty("author_id")
    private String authorId;

    private String name;

    @JsonProperty("average_rating")
    private Double averageRating;

    @JsonProperty("text_reviews_count")
    private Integer textReviewsCount;

    @JsonProperty("ratings_count")
    private Long ratingsCount;

}
