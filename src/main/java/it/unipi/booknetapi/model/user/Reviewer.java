package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(key = "role", value = "Reviewer")
public class Reviewer extends User {

    private Role role = Role.Reviewer;

    private List<UserBookShelf> shelf = new ArrayList<>();
    private ExternalId externalId;

}
