package it.unipi.booknetapi.model.user;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@BsonDiscriminator(key = "_t", value = "user")
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
