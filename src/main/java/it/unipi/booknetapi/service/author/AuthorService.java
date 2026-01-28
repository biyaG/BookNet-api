package it.unipi.booknetapi.service.author;

import it.unipi.booknetapi.command.author.*;
import it.unipi.booknetapi.dto.author.AuthorResponse;
import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.dto.author.AuthorStatResponse;
import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorStats;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void migrate() {
        Thread thread = new Thread(this.authorRepository::migrateAuthors);
        thread.start();
    }


    public AuthorResponse saveAuthor(AuthorCreateCommand command) {
        if(command.getName() == null) return null;

        Author authorNew = new Author(command);
        Author author = this.authorRepository.insert(authorNew);

        if(author == null) return null;

        return new AuthorResponse(author);
    }


    public AuthorResponse getAuthorById(AuthorGetCommand command) {
        if(command.getId() == null) return null;

        Author author = this.authorRepository.findById(command.getId()).orElse(null);
        if(author == null) return null;

        return new AuthorResponse(author);
    }

    public boolean deleteAuthor(AuthorDeleteCommand command) {
        if(command.getId() == null) return false;

        return this.authorRepository.delete(command.getId());
    }

    public boolean deleteMultiAuthors(AuthorIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;

        return this.authorRepository.delete(command.getIds());
    }

    public PageResult<AuthorSimpleResponse> getAllAuthors(AuthorListCommand command) {

        PageResult<Author> result = this.authorRepository
                .findAll(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(AuthorSimpleResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<AuthorSimpleResponse> searchAuthors(AuthorSearchCommand command) {
        if(command.getName() == null || command.getName().isBlank()) return new PageResult<>(List.of(), 0, 0, 0);

        int page = command.getPagination() == null ? 0 : command.getPagination().getPage();
        int size = command.getPagination() == null ? 10 : command.getPagination().getSize();

        PageResult<Author> result = this.authorRepository.search(command.getName(), page, size);

        return new PageResult<>(
                result.getContent().stream().map(AuthorSimpleResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public List<AuthorSimpleResponse> getAuthors(AuthorIdsListCommand command) {
        Objects.requireNonNull(command.getIds());

        List<Author> authors = this.authorRepository.findAll(command.getIds());

        return authors.stream()
                .map(AuthorSimpleResponse::new)
                .toList();
    }

    public List<BookEmbedResponse> getAuthorBooks(AuthorBooksGetCommand command) {
        Objects.requireNonNull(command.getId());

        List<BookEmbed> books = this.authorRepository.findBooksByAuthor(command.getId());

        if(books == null) return List.of();

        return books.stream()
                .map(BookEmbedResponse::new)
                .toList();
    }

    public List<AuthorStatResponse> getMostWrittenBooksAuthors(AuthorGetMostWrittenBooksCommand command) {
        List<AuthorStats> stats = this.authorRepository.findMostWrittenBooksAuthors(command.getLimit() != null ? command.getLimit() : 20);

        return stats.stream()
                .map(AuthorStatResponse::new)
                .toList();
    }

    public List<AuthorStatResponse> getMostFollowedAuthors(AuthorGetMostFollowedAuthors command) {
        List<AuthorStats> stats = this.authorRepository.findMostFollowedAuthors(command.getLimit() != null ? command.getLimit() : 20);

        return stats.stream()
                .map(AuthorStatResponse::new)
                .toList();
    }

    public List<AuthorStatResponse> getMostReadAuthors(AuthorGetMostReadCommand command) {
        List<AuthorStats> stats = this.authorRepository.findMostReadAuthors(command.getLimit() != null ? command.getLimit() : 20);

        return stats.stream()
                .map(AuthorStatResponse::new)
                .toList();
    }

}
