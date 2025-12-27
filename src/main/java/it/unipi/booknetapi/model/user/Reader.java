package it.unipi.booknetapi.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Reader extends User {

    private List<String> reviews;
    private List<UserBookShelf> shelf;
    private UserPreference preference;

}
