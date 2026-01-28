package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.BookRecommendation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookRecommendationResponse extends BookEmbedResponse {

    private Long score;

    public BookRecommendationResponse(BookEmbed book) {
        super(book);
    }

    public BookRecommendationResponse(BookEmbed book, Long score) {
        super(book);
        this.score = score;
    }

    public BookRecommendationResponse(BookRecommendation book) {
        super(book);

        this.score = book.getScore();
    }

}
