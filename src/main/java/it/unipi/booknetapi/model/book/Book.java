package it.unipi.booknetapi.model.book;

import it.unipi.booknetapi.command.book.BookCreateCommand;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
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
    private Integer numPage;
    private Date publicationDate;
    private Integer publicationYear;
    private Integer publicationMonth;
    private Integer publicationDay;
    private List<String> languages = new ArrayList<>();
    private List<String> images = new ArrayList<>();
    private List<String> previews = new ArrayList<>();
    private List<String> publishers = new ArrayList<>();
    private List<ObjectId> reviews = new ArrayList<>(); // we should change List<Review> to List<String> to save the ids only for the review
    private List<AuthorEmbed> authors = new ArrayList<>();
    private List<GenreEmbed> genres = new ArrayList<>();
    private List<BookEmbed> similarBooks = new ArrayList<>();
    private ReviewSummary ratingReview;
    private FormatTypeEnum format;
    private ExternalId externalId;

    public Book(BookCreateCommand command) {
        this.title = command.getTitle();
        this.description = command.getDescription();
        this.images = command.getImages();
        this.isbn = command.getIsbn();
        this.isbn13 = command.getIsbn13();
        this.subtitle = command.getSubtitle();
        this.numPage = command.getNumPages();
        this.publicationDate = command.getPublicationDate();
        this.externalId = command.getExternalId();
        this.languages = command.getLanguage();
        this.previews = command.getPreview();
        this.publishers = command.getPublishers();
        this.similarBooks = command.getSimilarBooks();
    }


}
