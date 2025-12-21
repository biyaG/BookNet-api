package it.unipi.booknetapi.dto.user;

import lombok.Data;

@Data
public class UserRegistrationRequest {

    private String name;
    private String username;
    private String password;

}
