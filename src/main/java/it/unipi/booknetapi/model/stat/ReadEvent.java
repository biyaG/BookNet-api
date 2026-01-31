package it.unipi.booknetapi.model.stat;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadEvent {

    private BookEmbed book;
    private Date dateRead;
    private Integer pages;
    private List<GenreEmbed> genres;

}
