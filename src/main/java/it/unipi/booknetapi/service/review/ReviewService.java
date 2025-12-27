package it.unipi.booknetapi.service.review;

import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import org.springframework.stereotype.Service;

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


    private static String generateCacheKeyForBook(String idBook, int page) {
        return CACHE_PREFIX + "book:" + idBook + ":page:" + page;
    }

    private static String generateCacheKeyForReader(String idReader, int page) {
        return CACHE_PREFIX + "reader:" + idReader + ":page:" + page;
    }


}
