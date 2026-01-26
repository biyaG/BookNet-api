package it.unipi.booknetapi.model.user;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BsonDiscriminator(key = "role", value = "User")
public class User {

    @Id @BsonId
    private ObjectId id;

    private String name;
    private String imageUrl;

    private Role role;

    public UserEmbed toEmbed() {
        return new UserEmbed(this);
    }

}
