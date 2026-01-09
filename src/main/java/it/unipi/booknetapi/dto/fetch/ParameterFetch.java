package it.unipi.booknetapi.dto.fetch;

import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.shared.model.Source;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterFetch<T> {

    private Source source;
    private EntityType entityType;
    private String fileUrl;
    private String fileName;
    private String fileContentType;
    private List<T> data = new ArrayList<>();

}
