package it.unipi.booknetapi.command.user;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeleteManyCommand {

    private List<String> ids = new ArrayList<>();

}
