package it.unipi.booknetapi.repository.user;

import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.notification.NotificationEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryInterface {

    <T extends User> T insert(T user);
    <T extends User> T insertWithThread(T user);
    <T extends User> List<T> insert(List<T> users);

    boolean updateName(String idUser, String newName);
    // boolean updateRole(String idUser, Role newRole);
    boolean updatePassword(String idUser, String newPassword);
    boolean updateImage(String idUser, String newImageUrl);
    boolean updatePreference(String idUser, ReaderPreference preference);

    int importGoodReadsReviewsRead(List<ReviewerRead> reads);

    List<UserBookShelf> getShelf(String idUser);
    boolean updateShelf(String idUser, List<BookEmbed> shelf);
    boolean addBookInShelf(String idUser, BookEmbed book);
    boolean removeBookFromShelf(String idUser, String idBook);
    boolean updateShelfStatus(String idUser, BookEmbed book, BookShelfStatus newStatus);

    boolean addReview(Review review);
    boolean deleteReview(String idUser, String idBook, String idReview);
    List<ObjectId> getReviewsIds(String idUser);

    boolean addNotification(String idUser, NotificationEmbed notification);
    boolean deleteNotification(String idUser, String idNotification);
    boolean deleteNotification(String idUser, List<String> idNotification);
    List<ObjectId> getNotificationsIds(String idUser);

    boolean delete(String idUser);
    boolean deleteAll(List<String> idUsers);

    Optional<User> findById(String idUser);
    Optional<Reader> findReaderById(String idUser);

    Optional<User> findByUsername(String username);

    PageResult<User> findAll(int page, int size);
    PageResult<Admin> findAllAdmin(int page, int size);
    PageResult<Reader> findAllReader(int page, int size);
    PageResult<Reviewer> findAllReviewer(int page, int size);

    List<Reviewer> findByGoodReadsExternIds(List<String> externUserIds);

    void migrateReaders();
    void migrateReviewers();

    void migrate();

}
