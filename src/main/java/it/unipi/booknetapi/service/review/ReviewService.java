package it.unipi.booknetapi.service.review;

import it.unipi.booknetapi.command.review.*;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.model.user.UserEmbed;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "review:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, BookRepository bookRepository, CacheService cacheService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
    }

    private static String generateCacheKey(String idReview) {
        return CACHE_PREFIX + idReview;
    }

    private static String generateCacheKeyForBook(String idBook, int page) {
        return CACHE_PREFIX + "book:" + idBook + ":page:" + page;
    }

    private static String generateCacheKeyForReader(String idReader, int page) {
        return CACHE_PREFIX + "reader:" + idReader + ":page:" + page;
    }

    private void cacheReview(ReviewResponse review) {
        this.cacheService.save(generateCacheKey(review.getId()), review, CACHE_TTL);
    }

    private void deleteCache(String idReview) {
        this.cacheService.delete(generateCacheKey(idReview));
    }

    public ReviewResponse saveReview(ReviewCreateCommand command) {
        if(!command.hasUser()) return null;
        if(command.getBookId() == null || !ObjectId.isValid(command.getBookId())) return null;

        ObjectId bookId = new ObjectId(command.getBookId());

        User user = this.userRepository.findById(command.getUserToken().getIdUser()).orElse(null);

        if(user == null) return null;

        UserEmbed userEmbed = new UserEmbed(user);

        Review review = Review.builder()
                .bookId(bookId)
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
            } catch (Exception ignored) {}
        };
        Thread thread = new Thread(task);
        thread.start();

        ReviewResponse reviewResponse = new ReviewResponse(reviewSaved);
        this.cacheReview(reviewResponse);
        return reviewResponse;
    }

    public ReviewResponse getReviewById(ReviewGetCommand command) {
        if(command.getId() == null) return null;

        try {
            ReviewResponse reviewResponse = this.cacheService.get(generateCacheKey(command.getId()), ReviewResponse.class);
            if(reviewResponse != null) return reviewResponse;
        } catch (Exception ignored) {}

        Review review = this.reviewRepository.findById(command.getId()).orElse(null);
        if(review == null) return null;

        ReviewResponse reviewResponse = new ReviewResponse(review);
        this.cacheReview(reviewResponse);

        return reviewResponse;
    }

    public boolean deleteReview(ReviewDeleteCommand command) {
        if(command.getId() == null) return false;
        if(!command.hasUser()) return false;

        Review review = this.reviewRepository.findById(command.getId())
                .orElse(null);

        if(review == null) return false;

        if(!Objects.equals(command.getUserToken().getIdUser(), review.getUser().getId().toHexString())) return false;

        boolean result = this.reviewRepository.delete(command.getId());

        this.cacheService.delete(generateCacheKey(command.getId()));

        return result;
    }

    public boolean deleteReview(ReviewIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;
        if(!command.hasUser()) return false;

        if(command.getUserToken().getRole() != Role.ADMIN) return false;

        boolean result = this.reviewRepository.delete(command.getIds());

        command.getIds().forEach(this::deleteCache);

        return result;
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
