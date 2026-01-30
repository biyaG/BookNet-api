package it.unipi.booknetapi.service.user;

import it.unipi.booknetapi.command.user.*;
import it.unipi.booknetapi.dto.user.*;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;


    public UserService(
            AuthorRepository authorRepository,
            GenreRepository genreRepository,
            UserRepository userRepository
    ) {
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.userRepository = userRepository;
    }


    public ReaderComplexResponse getReaderById(String idUser) {
        User user =  userRepository.findById(idUser)
                .orElse(null);

        if(user == null || user.getRole() == null || user.getRole() != Role.Reader) return null;

        return new ReaderComplexResponse(user);
    }


    public UserResponse get(UserGetCommand command) {
        User user =  userRepository.findById(command.getId()).orElse(null);

        if(user == null) {
            return null;
        }

        return switch (user.getRole()) {
            case Admin -> new AdminResponse(user);
            case Reader -> new ReaderResponse(user);
            case Reviewer -> new ReviewerResponse(user);
            default -> new UserResponse(user);
        };
    }

    public UserResponse update(UserUpdateCommand command) {
        if(command.getUserToken() == null || command.getUserToken().getIdUser() == null) return null;

        if(command.getName() == null) return null;

        boolean updated = this.userRepository.updateName(command.getUserToken().getIdUser(), command.getName());
        if(!updated) return null;

        return this.get(UserGetCommand.builder().id(command.getUserToken().getIdUser()).userToken(command.getUserToken()).build());
    }

    public ReaderResponse update(ReaderUpdatePreferenceCommand command) {
        if(command.getUserToken() == null || command.getUserToken().getIdUser() == null) return null;

        List<Author> authors = this.authorRepository.findAllById(command.getAuthors());
        List<Genre> genres = this.genreRepository.find(command.getGenres());
        List<String> languages = command.getLanguages() != null ? command.getLanguages() : List.of();

        ReaderPreference readerPreference = ReaderPreference.builder()
                .authors(authors.stream().map(AuthorEmbed::new).toList())
                .genres(genres.stream().map(GenreEmbed::new).toList())
                .languages(languages)
                .build();

        boolean updated = this.userRepository.updatePreference(command.getUserToken().getIdUser(), readerPreference);
        if(!updated) return null;

        User user =  this.userRepository.findById(command.getUserToken().getIdUser())
                .orElse(null);

        return new ReaderResponse(user);
    }


    public PageResult<AdminResponse> list(AdminListCommand command) {
        PageResult<Admin> result = this.userRepository.findAllAdmin(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(AdminResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReaderResponse> list(ReaderListCommand command) {
        PageResult<Reader> result = this.userRepository.findAllReader(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReaderResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReviewerResponse> list(ReviewerListCommand command) {
        PageResult<Reviewer> result = this.userRepository.findAllReviewer(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewerResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public void migrate() {
        migrateReaders();
        migrateReviewers();
    }

    public void migrateReaders() {
        Thread thread = new Thread(this.userRepository::migrateReaders);
        thread.start();
    }

    public void migrateReviewers() {
        Thread thread = new Thread(this.userRepository::migrateReviewers);
        thread.start();
    }

}
