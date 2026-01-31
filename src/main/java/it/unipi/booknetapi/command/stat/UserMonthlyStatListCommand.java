package it.unipi.booknetapi.command.stat;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserMonthlyStatListCommand extends BaseCommand {

    private String idUser;
    private Integer year;

}
