package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.model.user.UserEmbed;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleResponse {

    private String id;

    private String name;

    private String imageUrl;

    public UserSimpleResponse(UserEmbed user) {
        this.id = user.getId().toHexString();
        this.name = user.getName();
        this.imageUrl = user.getImageUrl();
    }

    public UserSimpleResponse(User user) {
        this.id = user.getId().toHexString();
        this.name = user.getName();
        this.imageUrl = user.getImageUrl();
    }
}
