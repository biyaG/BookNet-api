package it.unipi.booknetapi.service.book;

import it.unipi.booknetapi.command.book.*;
import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.dto.book.BookRecommendationResponse;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.dto.book.BookSimpleResponse;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.BookRecommendation;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
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

    public BookResponse getBookById(BookGetCommand command) {
        if (command.getId() == null) return null;

        Book book = this.bookRepository.findById(command.getId()).orElse(null);
        if (book == null) return null;

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

}
