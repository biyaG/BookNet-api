package it.unipi.booknetapi.command.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDeleteCommand {

    private String id;

}
