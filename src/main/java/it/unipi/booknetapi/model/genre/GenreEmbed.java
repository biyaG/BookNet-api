package it.unipi.booknetapi.model.genre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreEmbed {

    private ObjectId id;
    private String name;

    public GenreEmbed(Genre genre) {
        this.id = genre.getId();
        this.name = genre.getName();
    }

}
