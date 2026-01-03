package it.unipi.booknetapi.model.user;

import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id @BsonId
    private ObjectId id;

    private String name;
    private String username;
    private String password;
    private String imageUrl;

    private Role role;

    private ExternalId externalId;

}
