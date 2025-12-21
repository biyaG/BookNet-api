package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class ReaderResponse extends UserResponse {

    public ReaderResponse(User user) {
        super(user);
    }

}
