package it.unipi.booknetapi.shared.command;

import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import lombok.*;

@Data
// @Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseCommand {

    protected UserToken userToken;

    public boolean hasUser() {
        return userToken != null;
    }

}
