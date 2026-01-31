package it.unipi.booknetapi.service.review;

import it.unipi.booknetapi.command.review.*;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.stat.ActivityType;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.model.user.UserEmbed;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.stat.AnalyticsRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ReviewService {

    private final AnalyticsRepository analyticsRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;


    public ReviewService(
            AnalyticsRepository analyticsRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            BookRepository bookRepository
    ) {
        this.analyticsRepository = analyticsRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }


    private void logBookActivity(Book book, ActivityType type, int ratingValue) {
        this.analyticsRepository.recordActivity(
                book.getId(), book.getTitle(),
                null, null,
                null,
                type,
                ratingValue
        );
    }

    public ReviewResponse saveReview(ReviewCreateCommand command) {
        if(!command.hasUser()) return null;
        if(command.getBookId() == null || !ObjectId.isValid(command.getBookId())) return null;

        User user = this.userRepository.findById(command.getUserToken().getIdUser()).orElse(null);

        if(user == null) return null;

        UserEmbed userEmbed = new UserEmbed(user);

        Book book = this.bookRepository.findById(command.getBookId()).orElse(null);

        if(book == null) return null;

        Review review = Review.builder()
                .bookId(book.getId())
                .user(userEmbed)
                .rating(command.getRating())
                .comment(command.getComment())
                .dateAdded(new Date())
                .build();

        Review reviewSaved = this.reviewRepository.insert(review);
        if(reviewSaved == null) return null;

        Runnable task = () -> {
            try {
                this.userRepository.addReview(reviewSaved);
                this.bookRepository.addReview(reviewSaved);
                logBookActivity(book, ActivityType.RATING, command.getRating());
                if(command.getComment() != null) logBookActivity(book, ActivityType.REVIEW, 0);
            } catch (Exception ignored) {}
        };
        Thread thread = new Thread(task);
        thread.start();

        return new ReviewResponse(reviewSaved);
    }

    public ReviewResponse updateReview(ReviewUpdateCommand command) {
        if(!command.hasUser()) return null;
        if(command.getId() == null || !ObjectId.isValid(command.getId())) return null;

        boolean updated = this.reviewRepository.updateReview(command.getId(), command.getRating(), command.getComment());
        if(!updated) return null;

        Review review = this.reviewRepository.findById(command.getId()).orElse(null);
        if(review == null) return null;

        return new ReviewResponse(review);
    }

    public ReviewResponse getReviewById(ReviewGetCommand command) {
        if(command.getId() == null) return null;

        Review review = this.reviewRepository.findById(command.getId()).orElse(null);
        if(review == null) return null;

        return new ReviewResponse(review);
    }

    public boolean deleteReview(ReviewDeleteCommand command) {
        if(command.getId() == null) return false;
        if(!command.hasUser()) return false;

        Review review = this.reviewRepository.findById(command.getId())
                .orElse(null);

        if(review == null) return false;

        if(!Objects.equals(command.getUserToken().getIdUser(), review.getUser().getId().toHexString())) return false;

        return this.reviewRepository.delete(command.getId());
    }

    public boolean deleteReview(ReviewIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;
        if(!command.hasUser()) return false;

        if(command.getUserToken().getRole() != Role.Admin) return false;

        return this.reviewRepository.delete(command.getIds());
    }

    public List<ReviewResponse> getReviews(ReviewIdsListCommand command) {
        Objects.requireNonNull(command.getIds());

        List<Review> reviews = this.reviewRepository.findAll(command.getIds());

        return reviews.stream()
                .map(ReviewResponse::new)
                .toList();
    }

    public PageResult<ReviewResponse> getReviews(ReviewListCommand command) {
        PageResult<Review> result = this.reviewRepository.findAll(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReviewResponse> getReviews(ReviewByBookListCommand command) {
        PageResult<Review> result = this.reviewRepository.findByBook(command.getBookId(), command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReviewResponse> getReviews(ReviewByReaderListCommand command) {
        PageResult<Review> result = this.reviewRepository.findByReader(command.getReaderId(), command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

}
