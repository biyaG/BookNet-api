package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.dto.author.AuthorEmbedResponse;
import it.unipi.booknetapi.dto.genre.GenreEmbedResponse;
import it.unipi.booknetapi.model.user.UserPreference;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {

    private List<AuthorEmbedResponse> authors = new ArrayList<>();
    private List<GenreEmbedResponse> genres = new ArrayList<>();
    private List<String> languages = new ArrayList<>();

    public UserPreferenceResponse(UserPreference userPreference) {
        if(userPreference.getAuthors() != null) this.authors = userPreference.getAuthors().stream().map(AuthorEmbedResponse::new).toList();
        if(userPreference.getGenres() != null) this.genres = userPreference.getGenres().stream().map(GenreEmbedResponse::new).toList();
        if(userPreference.getLanguages() != null) this.languages = userPreference.getLanguages();
    }

}
