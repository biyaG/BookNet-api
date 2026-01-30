package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderPreference {

    private List<AuthorEmbed> authors = new ArrayList<>();
    private List<GenreEmbed> genres = new ArrayList<>();
    private List<String> languages = new ArrayList<>();

}
