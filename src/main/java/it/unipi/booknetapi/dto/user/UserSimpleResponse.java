package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.model.user.UserEmbed;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleResponse {

    private String idUser;

    private String name;

    private String imageUrl;

    public UserSimpleResponse(UserEmbed user) {
        this.idUser = user.getId().toHexString();
        this.name = user.getName();
        this.imageUrl = user.getImageUrl();
    }

    public UserSimpleResponse(User user) {
        this.idUser = user.getId().toHexString();
        this.name = user.getName();
        this.imageUrl = user.getImageUrl();
    }
}
