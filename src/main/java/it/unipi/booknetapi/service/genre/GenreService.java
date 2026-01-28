package it.unipi.booknetapi.service.genre;

import it.unipi.booknetapi.command.genre.*;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }


    public GenreResponse saveGenre(GenreCreateCommand command) {
        if(command.getName() == null || command.getName().isBlank()) return null;

        Genre genreNew = new Genre(command);
        Genre genre = this.genreRepository.insert(genreNew);

        return new GenreResponse(genre);
    }


    public GenreResponse getGenreById(GenreGetCommand command) {
        if(command.getId() == null) return null;

        Genre genre = this.genreRepository.findById(command.getId()).orElse(null);
        if(genre == null) return null;

        return new GenreResponse(genre);
    }

    public boolean deleteGenreById(GenreDeleteCommand command) {
        if(command.getId() == null) return false;

        return this.genreRepository.delete(command.getId());
    }

    public boolean deleteAllGenres(GenreIdsDeleteCommand command) {
        if(command.getIds() == null || command.getIds().isEmpty()) return false;

        return this.genreRepository.delete(command.getIds());
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
