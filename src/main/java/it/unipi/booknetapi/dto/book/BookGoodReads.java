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
    private String languageCode;
    @JsonProperty("popular_shelves")
    private List<BookShelfGoodReads> popularShelves;
    @JsonProperty("similar_books")
    private List<String> similarBooks;
    private List<BookAuthorGoodReads> authors;
    private String title;
    @JsonProperty("title_without_series")
    private String titleWithoutSeries;
    private String description;
    private String publisher;
    @JsonProperty("average_rating")
    private String averageRating;
    @JsonProperty("rating_count")
    private String ratingCount;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("book_id")
    private String bookId;
    //Preview
    @JsonProperty("url")
    private String url;

    @JsonProperty("publication_year")
    private String publicationYear;
    @JsonProperty("publication_month")
    private String publicationMonth;
    @JsonProperty("publication_day")
    private String publicationDay;

}
