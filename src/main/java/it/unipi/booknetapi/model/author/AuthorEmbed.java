package it.unipi.booknetapi.model.author;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorEmbed {

    private String id;
    private String name;

}
