package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.Reviewer;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;

@Data
@NoArgsConstructor
// @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewerResponse extends UserResponse {

    private ExternalId externalId;

    public ReviewerResponse(User user) {
        super(user);

        if(user instanceof Reviewer) {
            this.externalId = ((Reviewer) user).getExternalId();
        }
    }

}
