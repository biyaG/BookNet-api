package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import lombok.*;

@Data
@NoArgsConstructor
// @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends UserSimpleResponse {

    private String username;

    private Role role;

    public UserResponse(User user) {
        super(user);
        this.username = user.getUsername();
        this.role = user.getRole();
    }

}
