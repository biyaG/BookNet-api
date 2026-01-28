package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.review.ReviewSummary;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookResponse extends BookSimpleResponse {

    private Integer publicationYear;
    private Integer publicationMonth;
    private Integer publicationDay;

    private ReviewSummary ratingReview;

    private List<String> languages = new ArrayList<>();
    private List<String> previews = new ArrayList<>();
    private List<String> publishers = new ArrayList<>();

    private List<BookSimpleResponse> similarBooks = List.of();

    public BookResponse(Book book) {
        super(book);

        this.publicationYear = book.getPublicationYear();
        this.publicationMonth = book.getPublicationMonth();
        this.publicationDay = book.getPublicationDay();

        this.ratingReview = book.getRatingReview();

        this.languages = book.getLanguages();
        this.previews = book.getPreviews();
        this.publishers = book.getPublishers();

        if(book.getSimilarBooks() != null){
            this.similarBooks = book.getSimilarBooks().stream()
                    .map(BookSimpleResponse::new)
                    .toList();
        }
    }
}

