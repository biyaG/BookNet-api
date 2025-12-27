package it.unipi.booknetapi.model.book;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.review.ReviewSummary;
import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @BsonId
    private ObjectId id;
    private String isbn;
    private String isbn13;
    private String title;
    private String subtitle;
    private String description;
    private Integer num_pages;
    private Date publication_date;
    private ReviewSummary review; //review is a document
    private List<String> language = new ArrayList<>();
    private List<String> images = new ArrayList<>();
    private List<String> preview = new ArrayList<>();
    private List<String> publishers = new ArrayList<>();
    private List<Review> reviews = new ArrayList<>();
    private List<AuthorEmbed> authors = new ArrayList<>();
    private List<GenreEmbed> genres = new ArrayList<>();
    private List<BookEmbed> similar_books = new ArrayList<>();
    private List<ReviewSummary> ratingReview = new ArrayList<>();
    private FormatTypeEnum formats;
    private SourceFromEnum source;
    private ExternalId externalId;



}
