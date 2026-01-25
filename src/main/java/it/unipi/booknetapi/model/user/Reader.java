package it.unipi.booknetapi.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(value = "reader")
public class Reader extends InternalUser {

    private Role role = Role.READER;

    private List<ObjectId> reviews;
    private List<UserBookShelf> shelf;
    private UserPreference preference;

}
