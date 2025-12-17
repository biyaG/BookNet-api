package it.unipi.booknetapi.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class User {

    @Id
    private String id;

    private String name;
    private String username;
    private String password;
    private String imageUrl;

    private Role role;

}
