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


    public UserEmbed(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.imageUrl = user.getImageUrl();
    }

}
