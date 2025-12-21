package it.unipi.booknetapi.command.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdateNameCommand {

    private String name;

}
