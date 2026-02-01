package it.unipi.booknetapi.service.book;

import it.unipi.booknetapi.command.book.*;
import it.unipi.booknetapi.command.stat.AnalyticsGetListCommand;
import it.unipi.booknetapi.command.user.ReaderAddBookToShelfCommand;
import it.unipi.booknetapi.command.user.ReaderRemoveBookInShelfCommand;
import it.unipi.booknetapi.command.user.ReaderShelfGetCommand;
import it.unipi.booknetapi.command.user.ReaderUpdateBookStatusInShelfCommand;
import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.dto.book.BookRecommendationResponse;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.dto.book.BookSimpleResponse;
import it.unipi.booknetapi.dto.stat.ChartDataPointResponse;
import it.unipi.booknetapi.dto.user.UserBookShelfResponse;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.BookRecommendation;
import it.unipi.booknetapi.model.stat.ActivityType;
import it.unipi.booknetapi.model.stat.ChartDataPoint;
import it.unipi.booknetapi.model.stat.ChartHelper;
import it.unipi.booknetapi.model.user.BookShelfStatus;
import it.unipi.booknetapi.model.user.UserBookShelf;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.stat.AnalyticsRepository;
import it.unipi.booknetapi.repository.stat.UserMonthlyStatRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;


@Service
public class BookService {

    private final AnalyticsRepository analyticsRepository;
    private final BookRepository bookRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final UserRepository userRepository;

    public BookService(
            AnalyticsRepository analyticsRepository,
            BookRepository bookRepository,
            UserMonthlyStatRepository userMonthlyStatRepository,
            UserRepository userRepository
    ) {
        this.analyticsRepository = analyticsRepository;
        this.bookRepository = bookRepository;
        this.userMonthlyStatRepository = userMonthlyStatRepository;
        this.userRepository = userRepository;
    }


    public void migrate() {
        Thread thread = new Thread(this.bookRepository::migrate);
        thread.start();
    }


    public BookResponse saveBook(BookCreateCommand command){
        if(command.getTitle() == null || command.getTitle().isBlank()) return null;

        Book bookNew = new Book(command);
        Book book = this.bookRepository.save(bookNew);

        if(book == null)return null;

        return new BookResponse(book);
    }

    public List<BookResponse> importBooks(List<BookCreateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Convert the list of Commands into a list of Book Entities
        List<Book> booksToSave = commands.stream()
                .map(Book::new) // Uses your constructor: public Book(BookCreateCommand command)
                .toList();

        // 2. Save all books to the database in one batch
        List<Book> savedBooks = this.bookRepository.saveAll(booksToSave);

        // 3. Convert saved Entities to Responses and Cache them
        return savedBooks.stream()
                .map(BookResponse::new)
                .toList();
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

    private void logBookActivityInThread(Book book, ActivityType type, int ratingValue) {
        Thread thread = new Thread(() -> logBookActivity(book, type, ratingValue));
        thread.start();
    }

    private void logBookActivity(BookEmbed book, ActivityType type, int ratingValue) {
        this.analyticsRepository.recordActivity(
                book.getId(), book.getTitle(),
                null, null,
                null,
                type,
                ratingValue
        );
    }

    private void logBookActivityInThread(BookEmbed book, ActivityType type, int ratingValue) {
        Thread thread = new Thread(() -> logBookActivity(book, type, ratingValue));
        thread.start();
    }

    public BookResponse getBookById(BookGetCommand command) {
        if (command.getId() == null) return null;
        if(!ObjectId.isValid(command.getId())) return null;

        Book book = this.bookRepository.findById(command.getId()).orElse(null);
        if (book == null) return null;

        logBookActivityInThread(book, ActivityType.VIEW, 0);

        return new BookResponse(book);
    }


    public boolean deleteBookById(BookDeleteCommand command){
        if(command.getId() == null)return false;
        return this.bookRepository.deleteBook(command.getId());
    }

    public boolean deleteManyBooks(BookDeleteManyCommand command){
        if(command.getIds() == null) return false;
        return this.bookRepository.deleteAllBooks(command.getIds());
    }

    public PageResult<BookSimpleResponse> getAllBooks(BookListCommand command){
        int page = command.getPagination() == null ? 0 : command.getPagination().getPage();
        int size = command.getPagination() == null ? 10 : command.getPagination().getSize();

        PageResult<Book> result = this.bookRepository.findAll(page, size);

        return new PageResult<>(
                result.getContent().stream().map(BookSimpleResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }


    public PageResult<BookSimpleResponse> searchBooks(BookSearchCommand command){
        if(command.getTitle() == null || command.getTitle().isBlank()) return new PageResult<>(List.of(), 0, 0, 0);

        int page = command.getPagination() == null ? 0 : command.getPagination().getPage();
        int size = command.getPagination() == null ? 10 : command.getPagination().getSize();

        PageResult<Book> result = this.bookRepository.search(command.getTitle(), page, size);

        return new PageResult<>(
                result.getContent().stream().map(BookSimpleResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }


    public PageResult<BookEmbedResponse> getBooksByGenre(BookGetByGenreCommand command){
        if(command.getIdGenre() == null) return new PageResult<>(List.of(), 0, 0, 0);

        int page = command.getPagination() == null ? 0 : command.getPagination().getPage();
        int size = command.getPagination() == null ? 10 : command.getPagination().getSize();

        PageResult<BookEmbed> result = this.bookRepository.findBooksByGenre(command.getIdGenre(), page, size);

        return new PageResult<>(
                result.getContent().stream().map(BookEmbedResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }


    public List<ChartDataPointResponse> getAnalytics(AnalyticsGetListCommand command) {
        if (command.getId() == null) return null;
        if(!ObjectId.isValid(command.getId())) return null;

        Book book = this.bookRepository.findById(command.getId()).orElse(null);
        if(book == null) return null;

        ChartHelper.ChartParams params = ChartHelper.normalizeParams(command.getStart(), command.getEnd(), command.getGranularity());

        List<ChartDataPoint> chartDataPoints = this.analyticsRepository.getChartData(book.getId(), params.start(), params.end(), params.granularity());

        return chartDataPoints.stream().map(ChartDataPointResponse::new).toList();
    }


    private int getDefaultLimitIfNull(Integer limit) {
        return limit == null ? 20 : limit;
    }

    public List<BookRecommendationResponse> getRandomBooks(BookRandomCommand command) {
        int limit = getDefaultLimitIfNull(command.getLimit());
        List<BookRecommendation> books = command.getIdUser() == null ? this.bookRepository.findRandomBooks(limit) : this.bookRepository.findRandomBooks(command.getIdUser(), limit);
        return books.stream().map(BookRecommendationResponse::new).toList();
    }

    public List<BookRecommendationResponse> getPopularBooksRating(BookPopularByRatingCommand command) {
        int limit = getDefaultLimitIfNull(command.getLimit());
        List<BookRecommendation> books = command.getDayAgo() == null ? this.bookRepository.findPopularBooksByRating(limit) : this.bookRepository.findPopularBooksByRating(command.getDayAgo(), limit);
        return books.stream().map(BookRecommendationResponse::new).toList();
    }

    public List<BookRecommendationResponse> getPopularBooksShelf(BookPopularByShelfCommand command) {
        int limit = getDefaultLimitIfNull(command.getLimit());
        List<BookRecommendation> books = this.bookRepository.findPopularBooksByShelf(limit);
        return books.stream().map(BookRecommendationResponse::new).toList();
    }

    public List<BookRecommendationResponse> getCollaborativeBooks(BookRecommendationCommand command) {
        int limit = getDefaultLimitIfNull(command.getLimit());
        List<BookRecommendation> books = this.bookRepository.findCollaborativeRecommendationsBooks(command.getIdUser(), limit);
        return books.stream().map(BookRecommendationResponse::new).toList();
    }


    public boolean addBookInShelf(ReaderAddBookToShelfCommand command) {
        if(command.getUserToken() == null || command.getIdBook() == null) return false;

        if(!ObjectId.isValid(command.getIdBook())) return false;

        Book book = this.bookRepository.findById(command.getIdBook()).orElse(null);
        if(book == null) return false;

        BookEmbed bookEmbed = new BookEmbed(book);
        return this.userRepository.addBookInShelf(command.getUserToken().getIdUser(), bookEmbed);
    }

    public boolean updateBookStatusInShelf(ReaderUpdateBookStatusInShelfCommand command) {
        if(command.getUserToken() == null || command.getIdBook() == null) return false;

        if(!ObjectId.isValid(command.getIdBook())) return false;

        Book book = this.bookRepository.findById(command.getIdBook()).orElse(null);
        if(book == null) return false;

        List<UserBookShelf> shelf = this.userRepository.getShelf(command.getUserToken().getIdUser());
        UserBookShelf shelfBook = shelf == null ? null : shelf.stream()
                .filter(s -> s.getBook().getId().equals(book.getId()))
                .findFirst()
                .orElse(null);

        if(shelf == null || shelf.isEmpty() || shelfBook == null) {
            ReaderAddBookToShelfCommand addCommand = ReaderAddBookToShelfCommand.builder()
                    .userToken(command.getUserToken())
                    .idBook(command.getIdBook())
                    .build();
            return this.addBookInShelf(addCommand);
        }

        BookShelfStatus status = command.getStatus() != null ? command.getStatus() : BookShelfStatus.nextStatus(shelfBook.getStatus());

        BookEmbed bookEmbed = new BookEmbed(book);
        boolean updated = this.userRepository.updateShelfStatus(command.getUserToken().getIdUser(), bookEmbed, status);

        if(updated) {
            LocalDate updateDate = shelfBook.getDateUpdated().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            boolean isNotCurrentMonth = !YearMonth.from(updateDate).equals(YearMonth.now());
            if(isNotCurrentMonth) {
                ObjectId userId = new ObjectId(command.getUserToken().getIdUser());
                this.userMonthlyStatRepository.addReadEvent(userId, bookEmbed);
            }

            if(status == BookShelfStatus.READING || status == BookShelfStatus.FINISHED) {
                logBookActivity(book, ActivityType.READ, 0);
            }
        }

        return updated;
    }


    public boolean removeBookFromShelf(ReaderRemoveBookInShelfCommand command) {
        if(command.getUserToken() == null || command.getIdBook() == null) return false;

        if(!ObjectId.isValid(command.getIdBook())) return false;

        return this.userRepository.removeBookFromShelf(command.getUserToken().getIdUser(), command.getIdBook());
    }

}
