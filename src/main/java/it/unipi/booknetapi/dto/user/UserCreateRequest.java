package it.unipi.booknetapi.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    private String name;
    private String username;
    private String password;

}
