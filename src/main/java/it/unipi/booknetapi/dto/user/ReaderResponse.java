package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class ReaderResponse extends UserResponse {

    public ReaderResponse(User user) {
        super(user);
    }

}
