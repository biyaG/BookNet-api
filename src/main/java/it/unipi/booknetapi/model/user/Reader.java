package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(key = "role", value = "Reader")
public class Reader extends InternalUser {

    private Role role = Role.Reader;

    private List<ObjectId> reviews;
    private List<UserBookShelf> shelf;
    private UserPreference preference;

}
