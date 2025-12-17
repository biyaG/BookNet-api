package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import lombok.Data;

import java.util.List;

@Data
public class UserPreference {

    private List<AuthorEmbed> authors;
    private List<GenreEmbed> genres;
    private List<String> languages;

}
