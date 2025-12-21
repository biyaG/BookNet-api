package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.shared.model.PaginationRequest;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserListCommand {

    private PaginationRequest pagination;

}
