package it.unipi.booknetapi.dto.review;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractionGoodReads {

    @JsonProperty("review_id")
    private String reviewId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("book_id")
    private String bookId;

    @JsonProperty("is_read")
    private Boolean isRead;

    private Integer rating;

    @JsonProperty("review_text_incomplete")
    private String reviewTextIncomplete;

    @JsonProperty("date_added")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "en_US")
    private Date dateAdded;

    @JsonProperty("date_updated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "en_US")
    private Date dateUpdated;

    @JsonProperty("read_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "en_US")
    private Date readAt;

    @JsonProperty("started_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "en_US")
    private Date startedAt;


}
