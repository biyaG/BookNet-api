package it.unipi.booknetapi.dto.genre;

import it.unipi.booknetapi.model.genre.Genre;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponse {

    private String id;

    private String name;

    public GenreResponse(Genre genre) {
        this.id = genre.getId().toHexString();
        this.name = genre.getName();
    }

}
