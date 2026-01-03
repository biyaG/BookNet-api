package it.unipi.booknetapi.command.book;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class BookDeleteManyCommand extends BaseCommand {
    private List<ObjectId> ids = new ArrayList<>();

}
