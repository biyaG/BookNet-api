package it.unipi.booknetapi.command.review;

import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewListCommand extends BaseCommand {

    private PaginationRequest pagination;

}
