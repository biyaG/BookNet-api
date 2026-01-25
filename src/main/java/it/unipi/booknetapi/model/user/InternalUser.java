package it.unipi.booknetapi.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(value = "internal")
public class InternalUser extends User {

    private String username;
    private String password;

    private String dateAdd;

}
