package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.shared.model.PaginationRequest;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListCommand {

    private PaginationRequest pagination;

}
