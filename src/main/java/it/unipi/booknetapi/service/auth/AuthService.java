package it.unipi.booknetapi.service.auth;

import it.unipi.booknetapi.dto.user.AdminRegistrationRequest;
import it.unipi.booknetapi.dto.user.ReaderRegistrationRequest;
import it.unipi.booknetapi.dto.user.UserLoginRequest;
import it.unipi.booknetapi.dto.user.UserResponse;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.lib.authentication.JwtService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.lib.encryption.EncryptionManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EncryptionManager encryptionManager;

    public AuthService(
            AuthorRepository authorRepository,
            GenreRepository genreRepository,
            UserRepository userRepository,
            JwtService jwtService,
            EncryptionManager encryptionManager
    ) {
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.encryptionManager = encryptionManager;
    }


    public UserResponse registerAdmin(AdminRegistrationRequest registrationRequest) {
        Admin admin = new Admin();
        admin.setName(registrationRequest.getName());
        admin.setUsername(registrationRequest.getUsername());
        admin.setPassword(encryptionManager.hashPassword(registrationRequest.getPassword()));
        admin.setRole(Role.Admin);
        return new UserResponse(userRepository.insertWithThread(admin));
    }

    public UserResponse registerReader(ReaderRegistrationRequest registrationRequest) {
        ReaderPreference readerPreference = new ReaderPreference();
        if(registrationRequest.getPreference() != null) {
            List<Author> authorList =
                    registrationRequest.getPreference().getAuthors() != null && !registrationRequest.getPreference().getAuthors().isEmpty()
                            ? this.authorRepository.findAll(registrationRequest.getPreference().getAuthors())
                            : new ArrayList<>();
            readerPreference.setAuthors(authorList.stream().map(AuthorEmbed::new).toList());

            List<Genre> genres =
                    registrationRequest.getPreference().getGenres() != null && !registrationRequest.getPreference().getGenres().isEmpty()
                            ? this.genreRepository.find(registrationRequest.getPreference().getGenres())
                            : new ArrayList<>();
            readerPreference.setGenres(genres.stream().map(GenreEmbed::new).toList());

            List<String> languages =
                    registrationRequest.getPreference().getLanguages() != null
                            ? registrationRequest.getPreference().getLanguages()
                            : new ArrayList<>();
            readerPreference.setLanguages(languages);
        } else {
            readerPreference.setAuthors(new ArrayList<>());
            readerPreference.setGenres(new ArrayList<>());
            readerPreference.setLanguages(new ArrayList<>());
        }

        Reader reader = new Reader();
        reader.setName(registrationRequest.getName());
        reader.setUsername(registrationRequest.getUsername());
        reader.setPassword(encryptionManager.hashPassword(registrationRequest.getPassword()));
        reader.setRole(Role.Reader);
        reader.setPreference(readerPreference);
        return new UserResponse(userRepository.insertWithThread(reader));
    }


    public String login(UserLoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);
        if (user == null || user.getPassword() == null) {
            return null;
        }

        if (!encryptionManager.checkPassword(loginRequest.getPassword(), user.getPassword())) {
            return null;
        }

        return jwtService.createToken(user);
    }

    public String loginAlt(UserLoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);
        if (user == null) {
            return null;
        }

        return jwtService.createToken(user);
    }

    public String refreshAccessToken(String token) {
        if(token == null) return null;

        // add more control like fetch the user from db to ensure they still exist/aren't banned etc.
        return jwtService.refreshToken(token.replace("Bearer ", ""));
    }

    public UserToken getUserToken(String token) {
        if(token == null) return null;

        return jwtService.validateToken(token.replace("Bearer ", ""));
    }

}
