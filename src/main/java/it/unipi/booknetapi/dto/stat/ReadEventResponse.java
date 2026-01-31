package it.unipi.booknetapi.dto.stat;

import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.dto.genre.GenreEmbedResponse;
import it.unipi.booknetapi.model.stat.ReadEvent;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadEventResponse {

    private String idBook;
    private BookEmbedResponse book;
    private Date dateRead;
    private Integer pages;
    private List<GenreEmbedResponse> genres = new ArrayList<>();

    public ReadEventResponse(ReadEvent readEvent) {
        this.idBook = readEvent.getBookId().toHexString();
        this.dateRead = readEvent.getDateRead();
        this.pages = readEvent.getPages();
        if(readEvent.getGenres() != null) this.genres = readEvent.getGenres().stream().map(GenreEmbedResponse::new).toList();
    }

}
