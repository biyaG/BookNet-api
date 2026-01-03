package it.unipi.booknetapi.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Reader extends User {

    private List<ObjectId> reviews;
    private List<UserBookShelf> shelf;
    private UserPreference preference;

}
