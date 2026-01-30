package it.unipi.booknetapi.command.review;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewUpdateCommand extends BaseCommand {

    private String id;

    private Integer rating;
    private String comment;

}
