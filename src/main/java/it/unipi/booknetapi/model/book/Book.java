package it.unipi.booknetapi.model.book;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.enums.FormatTypeEnum;
import it.unipi.booknetapi.model.enums.SourceFromEnum;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.review.ReviewEmbed;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Book {
    @Id
    private String id;
    private String isbn;
    private String isbn13;
    private String title;
    private String subtitle;
    private String description;
    private Integer num_pages;
    private Date publication_date;
    private ReviewEmbed review; //review is a document
    private List<String> language = new ArrayList<>();
    private List<String> images = new ArrayList<>();
    private List<String> preview = new ArrayList<>();
    private List<Review> reviews = new ArrayList<>();
    private List<AuthorEmbed> authors = new ArrayList<>();
    private List<GenreEmbed> genres = new ArrayList<>();
    private List<String> publishers = new ArrayList<>();
    private List<Book> similar_books = new ArrayList<>();
    private List<ReviewEmbed> ratingReview = new ArrayList<>();
    private FormatTypeEnum formats;
    private SourceFromEnum source;



}
