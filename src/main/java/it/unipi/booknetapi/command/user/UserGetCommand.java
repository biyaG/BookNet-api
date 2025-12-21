package it.unipi.booknetapi.command.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserGetCommand {

    private String id;

}
