package it.unipi.booknetapi.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.dto.user.UserLoginRequest;
import it.unipi.booknetapi.dto.user.UserRegistrationRequest;
import it.unipi.booknetapi.service.user.UserService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    @Operation(summary = "Register a new Reader")
    @PostMapping("/register/reader")
    public ResponseEntity<String> registerReader(@RequestBody UserRegistrationRequest request) {
        userService.registerReader(request);
        return ResponseEntity.ok("Reader registered successfully");
    }

    @Operation(summary = "Register a new Admin")
    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@RequestBody UserRegistrationRequest request) {
        userService.registerAdmin(request);
        return ResponseEntity.ok("Admin registered successfully");
    }

    @Operation(summary = "Login to get JWT Token")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        String token = userService.login(request);

        response.addHeader("Authorization", "Bearer " + token);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Login successful.");
    }

    @Operation(summary = "Refresh JWT Token")
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        String newToken = userService.refreshAccessToken(token);

        response.addHeader("Authorization", "Bearer " + newToken);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Token refreshed successfully.");
    }

    @Operation(summary = "Get User Token Data")
    @GetMapping("/user")
    public ResponseEntity<UserToken> getUserToken(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getUserToken(token));
    }

}
