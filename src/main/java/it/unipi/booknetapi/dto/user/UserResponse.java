package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends UserSimpleResponse {

    private Role role;

    public UserResponse(User user) {
        super(user);
        this.role = user.getRole();
    }

}
