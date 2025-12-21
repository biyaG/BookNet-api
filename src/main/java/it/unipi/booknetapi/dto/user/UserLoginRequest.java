package it.unipi.booknetapi.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
public class UserLoginRequest {

    private String username;
    private String password;

}
