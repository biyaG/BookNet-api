package it.unipi.booknetapi.repository.book;

import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.ReviewEmbed;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface BookRepositoryInterface {
    Book save(Book book);
    boolean deleteBook(String idBook);
    boolean deleteAllBooks(List<String> idBooks);
    boolean addReview(String idBook, String idUser, ReviewEmbed reviewEmbed);
    boolean removeReview(String idBook, String idUser, String idReview);
    boolean updateImage(String idBook, String newImageUrl);
    boolean updatePreview(String idBook, String newPreviewImageUrl);
    boolean deletePreview(String idBook, String deletePreviewImageUrl);
    boolean updateSimilarBooks (String idBook, BookEmbed book); //adding one book inside the list similarbooks
    boolean addGenre(String idBook, GenreEmbed genre);
    boolean removeGenre(String idBook, GenreEmbed genre);

    PageResult<Book> findAll(int page, int size);
    Optional<Book> findById(String idBook);
    Optional<List<Book>> findByTitle(String title);
}
