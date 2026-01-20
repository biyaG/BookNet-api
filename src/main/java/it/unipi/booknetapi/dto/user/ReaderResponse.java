package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.model.user.User;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
// @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReaderResponse extends UserResponse {

    private List<ReviewResponse> reviews = List.of();

    public ReaderResponse(User user) {
        super(user);
    }

}
