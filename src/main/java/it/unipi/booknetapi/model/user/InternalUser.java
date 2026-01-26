package it.unipi.booknetapi.model.user;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(key = "role", value = "Internal")
public class InternalUser extends User {

    private String username;
    private String password;

    private Date dateAdd;

}
