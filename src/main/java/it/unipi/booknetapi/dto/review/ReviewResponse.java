package it.unipi.booknetapi.dto.review;

import it.unipi.booknetapi.dto.user.UserSimpleResponse;
import it.unipi.booknetapi.model.review.Review;
import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String idReview;

    private String idBook;
    private UserSimpleResponse user;

    private Float rating;
    private String comment;
    private Date dateAdded;

    public ReviewResponse(Review review) {
        this.idReview = review.getId().toHexString();
        this.idBook = review.getBookId().toHexString();
        this.user = new UserSimpleResponse(review.getUser());
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.dateAdded = review.getDateAdded();
    }

}
