package it.unipi.booknetapi.model.book;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookRecommendation extends BookEmbed {

    private Double score;

    public BookRecommendation(Book book) {
        super(book);
    }

    public BookRecommendation(Book book, Double score) {
        super(book);
        this.score = score;
    }

}
