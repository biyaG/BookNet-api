package it.unipi.booknetapi.analytics.service;

import it.unipi.booknetapi.analytics.dto.AnalyticsOverviewDTO;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.user.ReaderStatsRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ReaderStatsRepository readerStatsRepository;

    public AnalyticsService(
            UserRepository userRepository,
            BookRepository bookRepository,
            ReviewRepository reviewRepository,
            ReaderStatsRepository readerStatsRepository
    ) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.readerStatsRepository = readerStatsRepository;
    }

    public AnalyticsOverviewDTO getOverview() {

        AnalyticsOverviewDTO dto = new AnalyticsOverviewDTO();

        dto.setTotalUsers(
                userRepository.findAll(0, 1).getTotalElements()
        );

        dto.setTotalBooks(
                bookRepository.findAll(0, 1).getTotalElements()
        );

        dto.setTotalReviews(
                reviewRepository.findAll(0, 1).getTotalElements()
        );

        // Will be implemented later using aggregation
        dto.setTotalReads(0L);

        return dto;
    }
}
