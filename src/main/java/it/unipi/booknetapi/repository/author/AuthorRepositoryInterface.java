package it.unipi.booknetapi.repository.author;

import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface AuthorRepositoryInterface {

    Author insert(Author author);
    List<Author> insert(List<Author> authors);

    List<Author> importAuthors(List<AuthorGoodReads> importedAuthors);

    boolean updateDescription(String idAuthor, String newDescription);
    boolean updateImage(String idAuthor, String newImageUrl);

    boolean updateBooks(String idAuthor, List<BookEmbed> books);
    boolean addBook(String idAuthor, BookEmbed book);
    boolean removeBook(String idAuthor, String idBook);

    boolean delete(String idAuthor);
    boolean delete(List<String> idAuthors);

    Optional<Author> findById(String idAuthor);

    PageResult<Author> findAll(int page, int size);
    List<Author> findAll(List<String> idAuthors);
    List<Author> find(List<ObjectId> idAuthors);

    List<Author> findByExternGoodReadIds(List<String> idAuthors);

    void migrateAuthors();
}
