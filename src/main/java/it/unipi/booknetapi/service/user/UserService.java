package it.unipi.booknetapi.service.user;

import it.unipi.booknetapi.dto.user.ReaderComplexResponse;
import it.unipi.booknetapi.dto.user.ReaderResponse;
import it.unipi.booknetapi.dto.user.UserResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.lib.authentication.JwtService;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.shared.lib.encryption.EncryptionManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final CacheService cacheService;
    private final EncryptionManager encryptionManager;
    private final JwtService jwtService;


    private static final String CACHE_PREFIX = "user:";
    private static final int CACHE_TTL = 3600; // 1 hour


    public UserService(
            UserRepository userRepository,
            AuthorRepository authorRepository,
            BookRepository bookRepository,
            CacheService cacheService,
            EncryptionManager encryptionManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
        this.encryptionManager = encryptionManager;
        this.jwtService = jwtService;
    }


    private static String generateCacheKey(String idUser) {
        return CACHE_PREFIX + idUser;
    }

    private void cacheUser(UserResponse user) {
        this.cacheService.save(generateCacheKey(user.getIdUser()), user, CACHE_TTL);
    }

    private void cacheUser(List<UserResponse> users) {
        users.forEach(this::cacheUser);
    }

    private void cacheUserInThread(List<UserResponse> users) {
        Thread thread = new Thread(() -> cacheUser(users));
        thread.start();
    }

    private void deleteCache(String idUser) {
        this.cacheService.delete(generateCacheKey(idUser));
    }

    private void deleteCache(List<String> idUsers) {
        idUsers.forEach(this::deleteCache);
    }

    private void deleteCacheInThread(List<String> idUsers) {
        Thread thread = new Thread(() -> deleteCache(idUsers));
        thread.start();
    }


    public ReaderComplexResponse getReaderById(String idUser) {
        try {
            ReaderComplexResponse userResponse = this.cacheService.get(generateCacheKey(idUser), ReaderComplexResponse.class);
            if(userResponse != null) return userResponse;
        } catch (Exception ignored) {}

        User user =  userRepository.findById(idUser)
                .orElse(null);

        if(user == null || user.getRole() == null || user.getRole() != Role.READER) return null;

        ReaderComplexResponse readerResponse = new ReaderComplexResponse(user);
        this.cacheUser(readerResponse);
        return readerResponse;
    }


    public UserResponse getUserById(String idUser) {

        try {
            UserResponse userResponse = this.cacheService.get(generateCacheKey(idUser), UserResponse.class);
            if(userResponse != null) return userResponse;
        } catch (Exception ignored) {}

        User user =  userRepository.findById(idUser).orElse(null);

        if(user == null) {
            return null;
        }

        System.out.println(user);

        if(user.getRole() == Role.ADMIN) {
            UserResponse userResponse = new UserResponse(user);
            this.cacheUser(userResponse);

            return userResponse;
        }

        ReaderResponse readerResponse = new ReaderResponse(user);
        this.cacheUser(readerResponse);
        return readerResponse;
    }

}
