package it.unipi.booknetapi.command.user;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGetCommand {

    private String id;

}
