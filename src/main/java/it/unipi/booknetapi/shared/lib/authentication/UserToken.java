package it.unipi.booknetapi.shared.lib.authentication;

import it.unipi.booknetapi.model.user.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserToken {

    private String name;
    private String username;
    private Role role;

    public UserToken() {}

    public UserToken(String name, String username, Role role) {
        this.name = name;
        this.username = username;
        this.role = role;
    }

}