package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(value = "reviewer")
public class Reviewer extends User {

    private Role role = Role.REVIEWER;

    private ExternalId externalId;

}
