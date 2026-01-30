package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.dto.user.ReaderPreferenceRequest;
import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReaderUpdatePreferenceCommand extends BaseCommand {

    private List<String> authors = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<String> languages = new ArrayList<>();

    public ReaderUpdatePreferenceCommand(ReaderPreferenceRequest request) {
        if(request.getAuthors() != null) this.authors = request.getAuthors();
        if(request.getGenres() != null) this.genres = request.getGenres();
        if(request.getLanguages() != null) this.languages = request.getLanguages();
    }
}
