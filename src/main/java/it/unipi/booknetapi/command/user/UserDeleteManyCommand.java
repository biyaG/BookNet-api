package it.unipi.booknetapi.command.user;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class UserDeleteManyCommand {

    private List<String> ids = new ArrayList<>();

}
