package it.unipi.booknetapi.model.author;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorStats {

    private String id;
    private String name;
    private Long count;

}
