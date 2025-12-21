package it.unipi.booknetapi.command.user;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateImageCommand {

    private String image;

}
