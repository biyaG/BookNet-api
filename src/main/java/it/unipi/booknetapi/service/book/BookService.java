package it.unipi.booknetapi.service.book;

import it.unipi.booknetapi.command.book.*;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.dto.book.BookSimpleResponse;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "book:";
    private static final int CACHE_TTL = 3600;

    public BookService(BookRepository bookRepository, CacheService cacheService) {
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
    }

    private static String generateCacheKey(String idBook){ return CACHE_PREFIX + idBook; }

    private void cacheBook(BookResponse book){
        this.cacheService.save(generateCacheKey(book.getIdBook()), book, CACHE_TTL);
    }

    private void deleteCache(String idBook){ this.cacheService.delete(generateCacheKey(idBook));}

    private void deleteCache(List<ObjectId> idBooks) {
        idBooks.forEach(id -> deleteCache(id.toString()));
    }


    public BookResponse saveBook(BookCreateCommand command){
        if(command.getTitle() == null || command.getTitle().isBlank()) return null;

        Book bookNew = new Book(command);
        Book book = this.bookRepository.save(bookNew);

        if(book == null)return null;

        BookResponse bookResponse = new BookResponse(book);
        this.cacheBook(bookResponse);
        return bookResponse;
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
                .map(book -> {
                    BookResponse response = new BookResponse(book);
                    this.cacheBook(response); // Caching each book for fast retrieval later
                    return response;
                })
                .toList();
    }

    public BookResponse getBookById(BookGetCommand command) {
        if (command.getId() == null) return null;

        try {
            BookResponse bookResponse = this.cacheService.get(generateCacheKey(command.getId()), BookResponse.class);
            if (bookResponse != null) return bookResponse;
        } catch (Exception ignored) {
        }

        Book book = this.bookRepository.findById(command.getId()).orElse(null);
        if (book == null) return null;
        BookResponse bookResponse = new BookResponse(book);
        this.cacheBook(bookResponse);

        return bookResponse;
    }


    public boolean deleteBookById(BookDeleteCommand command){
        if(command.getId() == null)return false;
        boolean result = this.bookRepository.deleteBook(command.getId());
        this.deleteCache(command.getId());
        return result;
    }

    public boolean deleteManyBooks(BookDeleteManyCommand command){
        if(command.getIds() == null)return false;
        boolean result = this.bookRepository.deleteAllBooks(command.getIds());
        this.deleteCache(command.getIds());
        return result;
    }

    public PageResult<BookSimpleResponse> getAllBooks(BookListCommand command){
        PageResult<Book> result = this.bookRepository.findAll(command.getPagination().getPage(),command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(BookSimpleResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }



//    public List<BookSimpleResponse> getBooks(BookIdsListCommand command){
//        Objects.requireNonNull(command.getIds());
//
//        List<Book> books = this.bookRepository.findAll(command.getIds());
//
//        return books.stream()
//                .map(BookSimpleResponse :: new)
//                .toList();
//    }
}
