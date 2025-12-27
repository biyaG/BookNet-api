package it.unipi.booknetapi.model.genre;

import it.unipi.booknetapi.command.genre.GenreCreateCommand;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @BsonId
    private ObjectId id;
    private String name;


    public Genre(GenreCreateCommand command) {
        this.name = command.getName();
    }

}
