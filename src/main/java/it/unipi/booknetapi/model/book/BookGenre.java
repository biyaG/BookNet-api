package it.unipi.booknetapi.model.book;

import it.unipi.booknetapi.model.genre.GenreEmbed;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookGenre {

    private ObjectId id;
    private List<GenreEmbed> genres = new ArrayList<>();

}
