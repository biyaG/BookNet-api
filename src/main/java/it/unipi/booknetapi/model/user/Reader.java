package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Reader extends User {

    private List<String> reviews;
    private List<BookEmbed> shelf;
    private UserPreference preference;

}
