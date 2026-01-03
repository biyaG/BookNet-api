package it.unipi.booknetapi.command.review;

import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewByReaderListCommand extends BaseCommand {

    private String readerId;

    private PaginationRequest pagination;

}
