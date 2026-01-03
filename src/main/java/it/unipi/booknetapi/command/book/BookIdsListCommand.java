package it.unipi.booknetapi.command.book;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class BookIdsListCommand extends BaseCommand {

    private List<ObjectId> ids;
}
