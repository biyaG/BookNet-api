package it.unipi.booknetapi.model.user;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(key = "role", value = "Admin")
public class Admin extends InternalUser {

    private Role role = Role.Admin;

}
