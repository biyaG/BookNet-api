package it.unipi.booknetapi.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

// @JsonIgnoreProperties(ignoreUnknown = true) helps avoid errors if fields are missing in the POJO
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookGoodReads {

    private String isbn;
    private String isbn13;
    @JsonProperty("text_reviews_count")
    private String textReviewsCount;
//    private List<String> series;
    @JsonProperty("country_code")
    private String countryCode;
    @JsonProperty("language_code")
    private String language_code;
    @JsonProperty("popular_shelves")
    private List<BookShelfGoodReads> popularShelves;
    @JsonProperty("similar_books")
    private List<String> similarBooks;
    private List<BookAuthorGoodReads> authors;
    private String title;
    private String description;
    @JsonProperty("average_rating")
    private String averageRating;
    @JsonProperty("rating_count")
    private String rating_count;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("book_id")
    private String externalId;
    //Preview
    @JsonProperty("url")
    private List<String> url;

}
