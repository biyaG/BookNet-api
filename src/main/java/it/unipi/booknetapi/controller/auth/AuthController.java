package it.unipi.booknetapi.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.dto.user.ReaderRegistrationRequest;
import it.unipi.booknetapi.dto.user.UserLoginRequest;
import it.unipi.booknetapi.dto.user.AdminRegistrationRequest;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.user.UserService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }


    @Operation(summary = "Register a new Reader")
    @PostMapping("/register/reader")
    public ResponseEntity<String> registerReader(@RequestBody ReaderRegistrationRequest request) {
        this.authService.registerReader(request);
        return ResponseEntity.ok("Reader registered successfully");
    }

    @Operation(summary = "Register a new Admin")
    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@RequestBody AdminRegistrationRequest request) {
        this.authService.registerAdmin(request);
        return ResponseEntity.ok("Admin registered successfully");
    }

    @Operation(summary = "Login to get JWT Token")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        String token = this.authService.login(request);

        if(token == null) {
            return ResponseEntity.badRequest().body("Invalid username or password.");
        }

        response.addHeader("Authorization", "Bearer " + token);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Login successful.");
    }

    @Operation(summary = "Login to get JWT Token")
    @PostMapping("/login-alt")
    public ResponseEntity<String> loginAlt(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        String token = this.authService.loginAlt(request);

        if(token == null) {
            return ResponseEntity.badRequest().body("Invalid username or password.");
        }

        response.addHeader("Authorization", "Bearer " + token);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Login successful.");
    }

    @Operation(summary = "Refresh JWT Token")
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        String newToken = this.authService.refreshAccessToken(token);

        response.addHeader("Authorization", "Bearer " + newToken);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Token refreshed successfully.");
    }

    @Operation(summary = "Get User Token Data")
    @GetMapping("/me")
    public ResponseEntity<UserToken> getUserToken(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(this.authService.getUserToken(token));
    }

}
