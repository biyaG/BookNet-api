package it.unipi.booknetapi.model.genre;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Genre {

    @Id
    private String id;
    private String name;

}
