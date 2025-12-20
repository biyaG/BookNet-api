package it.unipi.booknetapi.model.user;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class User {

    @Id
    private ObjectId id;

    private String name;
    private String username;
    private String password;
    private String imageUrl;

    private Role role;

}
