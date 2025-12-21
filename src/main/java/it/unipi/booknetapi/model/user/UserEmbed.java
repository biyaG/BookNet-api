package it.unipi.booknetapi.model.user;

import lombok.*;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmbed {

    private ObjectId id;

    private String name;

    private String imageUrl;

}
