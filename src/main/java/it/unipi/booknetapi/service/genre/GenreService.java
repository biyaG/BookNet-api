package it.unipi.booknetapi.service.genre;

import it.unipi.booknetapi.command.genre.*;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;
    private final CacheService cacheService;

    public GenreService(GenreRepository genreRepository, CacheService cacheService) {
        this.genreRepository = genreRepository;
        this.cacheService = cacheService;
    }


    private static final String CACHE_PREFIX = "genre:";
    private static final int CACHE_TTL = 3600; // 1 hour


    private static String generateCacheKey(String idGenre) {
        return CACHE_PREFIX + idGenre;
    }

    private void cacheGenre(GenreResponse genre) {
        this.cacheService.save(generateCacheKey(genre.getIdGenre()), genre, CACHE_TTL);
    }

    private void deleteCache(String idGenre) {
        this.cacheService.delete(generateCacheKey(idGenre));
    }


    public GenreResponse saveGenre(GenreCreateCommand command) {
        if(command.getName() == null || command.getName().isBlank()) return null;

        Genre genreNew = new Genre(command);
        Genre genre = this.genreRepository.insert(genreNew);

        GenreResponse genreResponse = new GenreResponse(genre);
        cacheGenre(genreResponse);

        return genreResponse;
    }


    public GenreResponse getGenreById(GenreGetCommand command) {
        if(command.getId() == null) return null;

        try {
            GenreResponse genreResponse = this.cacheService.get(generateCacheKey(command.getId()), GenreResponse.class);
            if(genreResponse != null) return genreResponse;
        } catch (Exception ignored) {}

        Genre genre = this.genreRepository.findById(command.getId()).orElse(null);
        if(genre == null) return null;

        GenreResponse genreResponse = new GenreResponse(genre);
        this.cacheGenre(genreResponse);

        return genreResponse;
    }

    public boolean deleteGenreById(GenreDeleteCommand command) {
        if(command.getId() == null) return false;

        boolean result =  this.genreRepository.delete(command.getId());

        this.deleteCache(command.getId());

        return result;
    }

    public boolean deleteAllGenres(GenreIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;

        boolean result = this.genreRepository.delete(command.getIds());

        command.getIds().forEach(this::deleteCache);

        return result;
    }

    public PageResult<GenreResponse> getAllGenres(GenreListCommand command) {
        PageResult<Genre> result = this.genreRepository
                .findAll(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(GenreResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<GenreResponse> getGenresByAuthorId(GenreIdsListCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) {
            return new PageResult<>(List.of(), 0, 0, 0);
        }

        List<Genre> genres = this.genreRepository.find(command.getIds());

        return new PageResult<>(
                genres.stream().map(GenreResponse::new).toList(),
                genres.size(),
                0,
                genres.size()
        );
    }

    public PageResult<GenreResponse> searchGenre(GenreSearchCommand command) {
        if(command.getName() == null || command.getName().isBlank()) return new PageResult<>(List.of(), 0, 0, 0);

        int page = command.getPagination() == null ? 0 : command.getPagination().getPage();
        int size = command.getPagination() == null ? 10 : command.getPagination().getSize();

        PageResult<Genre> result = this.genreRepository.search(command.getName(), page, size);

        return new PageResult<>(
                result.getContent().stream().map(GenreResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

}
