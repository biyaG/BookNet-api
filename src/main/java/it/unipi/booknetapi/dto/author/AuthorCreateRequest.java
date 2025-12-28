package it.unipi.booknetapi.dto.author;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorCreateRequest {

    private String name;
    private String description;
    private String imageUrl;

}
