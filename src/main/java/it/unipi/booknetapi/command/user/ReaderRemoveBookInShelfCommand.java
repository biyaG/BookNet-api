package it.unipi.booknetapi.command.user;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReaderRemoveBookInShelfCommand extends BaseCommand {

    private String idBook;

}
