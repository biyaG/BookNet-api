package it.unipi.booknetapi.dto.genre;

import it.unipi.booknetapi.model.genre.GenreEmbed;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreEmbedResponse {

    private String idGenre;
    private String name;

    public GenreEmbedResponse(GenreEmbed genre) {
        this.idGenre = genre.getId().toHexString();
        this.name = genre.getName();
    }
}
