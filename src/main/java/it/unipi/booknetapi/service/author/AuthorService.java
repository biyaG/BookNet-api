package it.unipi.booknetapi.service.author;

import it.unipi.booknetapi.command.author.*;
import it.unipi.booknetapi.dto.author.AuthorResponse;
import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "author:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public AuthorService(AuthorRepository authorRepository, CacheService cacheService) {
        this.authorRepository = authorRepository;
        this.cacheService = cacheService;
    }

    private static String generateCacheKey(String idAuthor) {
        return CACHE_PREFIX + idAuthor;
    }

    private void cacheAuthor(AuthorResponse author) {
        this.cacheService.save(generateCacheKey(author.getId()), author, CACHE_TTL);
    }

    private void deleteCache(String idAuthor) {
        this.cacheService.delete(generateCacheKey(idAuthor));
    }

    public AuthorResponse saveAuthor(AuthorCreateCommand command) {
        if(command.getName() == null) return null;

        Author authorNew = new Author(command);
        Author author = this.authorRepository.insert(authorNew);

        if(author == null) return null;

        AuthorResponse authorResponse = new AuthorResponse(author);
        this.cacheAuthor(authorResponse);
        return authorResponse;
    }


    public AuthorResponse getAuthorById(AuthorGetCommand command) {
        if(command.getId() == null) return null;

        try {
            AuthorResponse authorResponse = this.cacheService.get(generateCacheKey(command.getId()), AuthorResponse.class);
            if(authorResponse != null) return authorResponse;
        } catch (Exception ignored) {}

        Author author = this.authorRepository.findById(command.getId()).orElse(null);
        if(author == null) return null;

        AuthorResponse authorResponse = new AuthorResponse(author);
        this.cacheAuthor(authorResponse);

        return authorResponse;
    }

    public boolean deleteAuthorById(AuthorDeleteCommand command) {
        if(command.getId() == null) return false;

        boolean result =  this.authorRepository.delete(command.getId());

        this.deleteCache(command.getId());

        return result;
    }

    public boolean deleteMultiAuthors(AuthorIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;

        boolean result = this.authorRepository.delete(command.getIds());

        command.getIds().forEach(this::deleteCache);

        return result;
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

    public List<AuthorSimpleResponse> getAuthors(AuthorIdsListCommand command) {
        Objects.requireNonNull(command.getIds());

        List<Author> authors = this.authorRepository.findAll(command.getIds());

        return authors.stream()
                .map(AuthorSimpleResponse::new)
                .toList();
    }

}
