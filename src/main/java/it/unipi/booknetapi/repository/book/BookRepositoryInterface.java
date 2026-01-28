package it.unipi.booknetapi.repository.book;

import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.book.BookRecommendation;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookRepositoryInterface {
    Book save(Book book);
    List<Book> saveAll(List<Book> books);

    List<Book> importBooks(List<BookGoodReads> books);
    void importBooksAuthors(Map<ObjectId, List<AuthorEmbed>> bookAuthors);

    boolean deleteBook(String idBook);
    boolean deleteAllBooks(List<ObjectId> idBooks);
    boolean addReview(Review review);
    boolean removeReview(String idBook, String idUser, String idReview);
    boolean updateImage(String idBook, String newImageUrl);
    boolean updatePreview(String idBook, String newPreviewImageUrl);
    boolean deletePreview(String idBook, String deletePreviewImageUrl);
    boolean updateSimilarBooks (String idBook, List<BookEmbed> book);
    boolean updateSimilarBooks (Map<String, List<BookEmbed>> mapBooks);
    boolean updateGenres(String idBook, List<GenreEmbed> genres);
    boolean addGenre(String idBook, GenreEmbed genre);
    boolean removeGenre(String idBook, GenreEmbed genre);

    PageResult<Book> findAll(int page, int size);
    PageResult<Book> search(String title, int page, int size);
    Optional<Book> findById(String idBook);
    List<Book> findByTitle(String title);
    List<Book> searchByTitle(String title);
    List<Book> findByTitle(List<String> titles);

    PageResult<BookEmbed> findBooksByGenre(String idGenre, int page, int size);

    List<Book> findByGoodReadsExternIds(List<String> externBookIds);

    List<BookRecommendation> findRandomBooks(int limit);
    List<BookRecommendation> findRandomBooks(String idUser, int limit);
    List<BookRecommendation> findPopularBooksByRating(int limit);
    List<BookRecommendation> findPopularBooksByRating(Long dayAgo, int limit);
    List<BookRecommendation> findPopularBooksByShelf(int limit);
    List<BookRecommendation> findCollaborativeRecommendationsBooks(String idUser, int limit);
}
