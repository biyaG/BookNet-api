package it.unipi.booknetapi.command.stat;

import it.unipi.booknetapi.shared.command.BaseCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsGetListCommand extends BaseCommand {

    private String id;

    private Date start;
    private Date end;

    private String granularity;

}
