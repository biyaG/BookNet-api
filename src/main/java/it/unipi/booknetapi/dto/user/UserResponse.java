package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;

    private String name;
    private String username;
    private String imageUrl;

    private Role role;

    public UserResponse(User user) {
        this.id = user.getId().toHexString();
        this.name = user.getName();
        this.username = user.getUsername();
        this.imageUrl = user.getImageUrl();
        this.role = user.getRole();
    }

}
