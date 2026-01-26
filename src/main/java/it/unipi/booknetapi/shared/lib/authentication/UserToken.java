package it.unipi.booknetapi.shared.lib.authentication;

import it.unipi.booknetapi.model.user.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserToken {

    private String idUser;
    private String name;
    private String username;
    private Role role;

    public UserToken() {}

    public UserToken(InternalUser user) {
        this.idUser = user.getId().toHexString();
        this.name = user.getName();
        if(user instanceof Admin) this.username = user.getUsername();
        else if (user instanceof Reader) this.username = user.getUsername();
        else this.username = "";
        this.role = user.getRole();
    }

    public UserToken(String idUser, String name, String username, Role role) {
        this.idUser = idUser;
        this.name = name;
        this.username = username;
        this.role = role;
    }

}