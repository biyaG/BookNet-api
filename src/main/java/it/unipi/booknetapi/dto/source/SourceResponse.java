package it.unipi.booknetapi.dto.source;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceResponse {

    private String idSource;
    private String name;
    private String description;

}
