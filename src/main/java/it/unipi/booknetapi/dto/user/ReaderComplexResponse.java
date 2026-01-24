package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.Reader;
import it.unipi.booknetapi.model.user.User;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
// @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReaderComplexResponse extends ReaderResponse {

    private List<UserBookShelfResponse> shelf = new ArrayList<>();
    private UserPreferenceResponse preference = new UserPreferenceResponse();

    public ReaderComplexResponse(User user) {
        super(user);

        if(user instanceof Reader) {
            if(((Reader) user).getShelf() != null) this.shelf = ((Reader) user).getShelf().stream().map(UserBookShelfResponse::new).toList();
            if(((Reader) user).getPreference() != null) this.preference = new UserPreferenceResponse(((Reader) user).getPreference());
        }
    }

}
