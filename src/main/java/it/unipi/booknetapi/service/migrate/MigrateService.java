package it.unipi.booknetapi.service.migrate;

import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MigrateService {

    Logger logger = LoggerFactory.getLogger(MigrateService.class);

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public MigrateService(
            AuthorRepository authorRepository,
            BookRepository bookRepository,
            GenreRepository genreRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    public void migrateData() {

        migrateGenres();

        migrateAuthors();

        migrateBooks();

        migrateReaders();

        try {

        } catch (Exception ignored) {}
    }

    private void migrateGenres() {
        try {
            this.genreRepository.migrate();
        } catch (Exception ignored) {}
    }

    private void migrateAuthors() {
        try {
            this.authorRepository.migrate();
        } catch (Exception ignored) {}
    }

    private void migrateBooks() {
        try {
            this.bookRepository.migrate();
        } catch (Exception ignored) {}
    }

    private void migrateReaders() {
        try {
            this.userRepository.migrateReaders();
        } catch (Exception ignored) {}
    }

}
