package it.unipi.booknetapi.command.fetch;

import it.unipi.booknetapi.service.fetch.ImportEntityType;
import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.Source;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImportDataCommand extends BaseCommand {

    private Source source;
    private ImportEntityType importEntityType;
    private MultipartFile file;


    public boolean isValid() {
        return this.source != null && this.importEntityType != null && this.file != null;
    }

}