package it.unipi.booknetapi.repository.genre;

import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface GenreRepositoryInterface {

    Genre insert(Genre genre);
    List<Genre> insert(List<Genre> genres);

    boolean delete(String idGenre);
    boolean delete(List<String> idGenres);

    Optional<Genre> findById(String idGenre);
    List <GenreEmbed> findAllById(List<String> idGenre);


    List<Genre> find(List<String> idGenres);
    PageResult<Genre> findAll(int page, int size);

    List<Genre> findByName(List<String> GenreNames);

    PageResult<Genre> search(String name, int page, int size);

}
