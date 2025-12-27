package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;
import lombok.*;

@Data
@NoArgsConstructor
// @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReaderResponse extends UserResponse {

    public ReaderResponse(User user) {
        super(user);
    }

}
