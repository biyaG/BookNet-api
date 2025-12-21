package it.unipi.booknetapi.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {

    private String username;
    private String password;

}
