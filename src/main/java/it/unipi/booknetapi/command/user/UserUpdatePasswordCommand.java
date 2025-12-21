package it.unipi.booknetapi.command.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdatePasswordCommand {

    private String password;

}
