package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeleteManyCommand extends BaseCommand {

    private List<String> ids = new ArrayList<>();

}
