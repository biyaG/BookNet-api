package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.InternalUser;
import it.unipi.booknetapi.model.user.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InternalUserResponse extends UserResponse {

    private String username;
    private Date dateAdd;

    public InternalUserResponse(User user) {
        super(user);

        if(user instanceof InternalUser internalUser) {
            this.username = internalUser.getUsername();
            this.dateAdd = internalUser.getDateAdd();
        }
    }
}
