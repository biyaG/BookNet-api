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

        if(user instanceof Reader reader) {
            if(reader.getShelf() != null) this.shelf = reader.getShelf().stream().map(UserBookShelfResponse::new).toList();
            if(reader.getPreference() != null) this.preference = new UserPreferenceResponse(reader.getPreference());
        }
    }

}
