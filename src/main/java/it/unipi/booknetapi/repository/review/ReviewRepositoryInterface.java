package it.unipi.booknetapi.repository.review;

import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.ReviewerRead;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface ReviewRepositoryInterface {

    Review insert(Review review);
    List<Review> insert(List<Review> reviews);

    List<Review> insertFromGoodReads(List<Review> reviews);
    void importGoodReadsReviewsRead(List<ReviewerRead> reads);

    boolean updateReview(String idReview, Float rating, String comment);

    boolean delete(String idReview);
    boolean delete(List<String> idReviews);

    Optional<Review> findById(String idReview);

    List<Review> findAll(List<String> idReviews);

    PageResult<Review> findByBook(String idBook, int page, int size);
    PageResult<Review> findByReader(String idReader, int page, int size);
    PageResult<Review> findAll(int page, int size);

}
