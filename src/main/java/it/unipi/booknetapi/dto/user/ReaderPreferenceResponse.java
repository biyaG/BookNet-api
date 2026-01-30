package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.dto.author.AuthorEmbedResponse;
import it.unipi.booknetapi.dto.genre.GenreEmbedResponse;
import it.unipi.booknetapi.model.user.ReaderPreference;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderPreferenceResponse {

    private List<AuthorEmbedResponse> authors = new ArrayList<>();
    private List<GenreEmbedResponse> genres = new ArrayList<>();
    private List<String> languages = new ArrayList<>();

    public ReaderPreferenceResponse(ReaderPreference readerPreference) {
        if(readerPreference.getAuthors() != null) this.authors = readerPreference.getAuthors().stream().map(AuthorEmbedResponse::new).toList();
        if(readerPreference.getGenres() != null) this.genres = readerPreference.getGenres().stream().map(GenreEmbedResponse::new).toList();
        if(readerPreference.getLanguages() != null) this.languages = readerPreference.getLanguages();
    }

}
