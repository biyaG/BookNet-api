package it.unipi.booknetapi.service.user;

import it.unipi.booknetapi.dto.user.UserLoginRequest;
import it.unipi.booknetapi.dto.user.UserRegistrationRequest;
import it.unipi.booknetapi.dto.user.UserResponse;
import it.unipi.booknetapi.model.user.Admin;
import it.unipi.booknetapi.model.user.Reader;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.lib.authentication.JwtService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.lib.encryption.EncryptionManager;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionManager encryptionManager;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, EncryptionManager encryptionManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.encryptionManager = encryptionManager;
        this.jwtService = jwtService;
    }

    public UserResponse registerAdmin(UserRegistrationRequest registrationRequest) {
        Admin admin = new Admin();
        admin.setName(registrationRequest.getName());
        admin.setUsername(registrationRequest.getUsername());
        admin.setPassword(encryptionManager.encrypt(registrationRequest.getPassword()));
        admin.setRole(Role.ADMIN);
        return new UserResponse(userRepository.insertWithThread(admin));
    }

    public UserResponse registerReader(UserRegistrationRequest registrationRequest) {
        Reader reader = new Reader();
        reader.setName(registrationRequest.getName());
        reader.setUsername(registrationRequest.getUsername());
        reader.setPassword(encryptionManager.encrypt(registrationRequest.getPassword()));
        reader.setRole(Role.READER);
        return new UserResponse(userRepository.insertWithThread(reader));
    }

    public String login(UserLoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);
        if (user == null) {
            return null;
        }

        if (!encryptionManager.checkPassword(loginRequest.getPassword(), user.getPassword())) {
            return null;
        }

        return jwtService.createToken(user);
    }

    public String refreshAccessToken(String token) {
        // add more control like fetch the user from db to ensure they still exist/aren't banned etc.
        return jwtService.refreshToken(token);
    }

    public UserToken getUserToken(String token) {
        return jwtService.validateToken(token);
    }



    public UserResponse getUserById(String id) {
        User user =  userRepository.findById(id).orElse(null);

        if(user == null) {
            return null;
        }

        return new UserResponse(user);
    }

}
