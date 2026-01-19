package it.unipi.booknetapi.dto.source;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceCreateRequest {

    private String name;
    private String description;

}
