package it.unipi.booknetapi.model.user;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "UserName can not be blank")
    private String username;

    @NotBlank(message = "Password can not be blank")
    private String password;

    private Date dateAdd;

}
