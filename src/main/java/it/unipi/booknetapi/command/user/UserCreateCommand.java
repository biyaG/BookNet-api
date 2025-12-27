package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.dto.user.UserCreateRequest;
import it.unipi.booknetapi.dto.user.AdminRegistrationRequest;
import it.unipi.booknetapi.model.user.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateCommand {

    private String name;
    private String username;
    private String password;
    private String imageUrl;

    private Role role;


    public UserCreateCommand(AdminRegistrationRequest adminRegistrationRequest) {
        this.name = adminRegistrationRequest.getName();
        this.username = adminRegistrationRequest.getUsername();
        this.password = adminRegistrationRequest.getPassword();
        this.role = Role.READER;
    }

    public UserCreateCommand(UserCreateRequest userCreateRequest) {
        this.name = userCreateRequest.getName();
        this.username = userCreateRequest.getUsername();
        this.password = userCreateRequest.getPassword();
        this.role = Role.READER;
    }

}
