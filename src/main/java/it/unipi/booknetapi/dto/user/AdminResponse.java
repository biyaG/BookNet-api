package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;

public class AdminResponse extends InternalUserResponse {

    public AdminResponse(User user) {
        super(user);
    }
}
