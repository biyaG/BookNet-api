package it.unipi.booknetapi.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegistrationRequest {

    private String name;
    private String username;
    private String password;

}
