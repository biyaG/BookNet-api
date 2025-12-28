package it.unipi.booknetapi.command.book;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class BookDeleteCommand extends BaseCommand {
    private String id;
}
