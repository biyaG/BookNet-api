package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserUpdatePasswordCommand extends BaseCommand {

    private String password;

}
