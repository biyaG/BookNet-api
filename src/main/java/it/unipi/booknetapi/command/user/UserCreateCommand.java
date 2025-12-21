package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.dto.user.UserCreateRequest;
import it.unipi.booknetapi.dto.user.UserRegistrationRequest;
import it.unipi.booknetapi.model.user.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCreateCommand {

    private String name;
    private String username;
    private String password;
    private String imageUrl;

    private Role role;


    public UserCreateCommand(UserRegistrationRequest userRegistrationRequest) {
        this.name = userRegistrationRequest.getName();
        this.username = userRegistrationRequest.getUsername();
        this.password = userRegistrationRequest.getPassword();
        this.role = Role.READER;
    }

    public UserCreateCommand(UserCreateRequest userCreateRequest) {
        this.name = userCreateRequest.getName();
        this.username = userCreateRequest.getUsername();
        this.password = userCreateRequest.getPassword();
        this.role = Role.READER;
    }

}
