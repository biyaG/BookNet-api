package it.unipi.booknetapi.command.review;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewCreateCommand extends BaseCommand {

    private String bookId;

    private Integer rating;
    private String comment;

}
