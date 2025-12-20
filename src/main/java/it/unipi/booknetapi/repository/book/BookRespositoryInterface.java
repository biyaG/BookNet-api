package it.unipi.booknetapi.repository.book;

import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.ReviewEmbed;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface BookRespositoryInterface {
    boolean save(Book book);
    boolean delete(String idBook);
    boolean addReview(String idBook, String idUser, ReviewEmbed reviewEmbed);
    boolean removeReview(String idBook, String idUser, String idReview);
    boolean updateImage(String idBook, String newImageUrl);
    boolean updatePreview(String idBook, String newPreviewImageUrl);
    boolean updateListSimilarBooks (String idBook, List<BookEmbed> similar_books); //adding list of similarbooks
    boolean updateSimilarBooks (String idBook, BookEmbed book); //adding one book inside the list similarbooks
    boolean addGenre(String idGenre, String name);
    PageResult<Book> findAll(int page, int size);
    Optional<Book> findById(String idBook);
    Optional<Book> findByTitle(String title);
}
