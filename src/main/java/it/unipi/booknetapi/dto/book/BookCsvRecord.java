package it.unipi.booknetapi.dto.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookCsvRecord {

    private int bookId;
    private String title;
    private List<String> authors;
    private double averageRating;
    private String isbn;
    private String isbn13;
    private String languageCode;
    private int numPages;
    private long ratingsCount;
    private long textReviewsCount;
    private LocalDate publicationDate;
    private String publisher;

    public BookCsvRecord(BookCsvRecord record) {
        this.bookId = record.getBookId();
        this.title = record.getTitle();
        this.authors = record.getAuthors();
        this.averageRating = record.getAverageRating();
        this.isbn = record.getIsbn();
        this.isbn13 = record.getIsbn13();
        this.languageCode = record.getLanguageCode();
        this.numPages = record.getNumPages();
        this.ratingsCount = record.getRatingsCount();
        this.textReviewsCount = record.getTextReviewsCount();
        this.publicationDate = record.getPublicationDate();
        this.publisher = record.getPublisher();
    }

}
