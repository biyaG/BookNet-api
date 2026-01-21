package it.unipi.booknetapi.repository.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.model.user.UserPreference;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryInterface {

    <T extends User> T insert(T user);
    <T extends User> T insertWithThread(T user);
    <T extends User> List<T> insert(List<T> users);

    boolean updateName(String idUser, String newName);
    boolean updateRole(String idUser, Role newRole);
    boolean updatePassword(String idUser, String newPassword);
    boolean updateImage(String idUser, String newImageUrl);
    boolean updatePreference(String idUser, UserPreference preference);

    boolean updateShelf(String idUser, List<BookEmbed> shelf);
    boolean addBookInShelf(String idUser, BookEmbed book);
    boolean removeBookFromShelf(String idUser, String idBook);

    boolean addReview(Review review);
    boolean deleteReview(String idUser, String idBook, String idReview);

    boolean delete(String idUser);
    boolean deleteAll(List<String> idUsers);

    Optional<User> findById(String idUser);
    Optional<User> findByUsername(String username);
    PageResult<User> findAll(int page, int size);

    List<User> findByGoodReadsExternIds(List<String> externUserIds);
}
