package it.unipi.booknetapi.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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


    @PostMapping("/register/reader")
    @Operation(summary = "Register a new Reader")
    @SecurityRequirements(value = {})
    public ResponseEntity<String> registerReader(@RequestBody ReaderRegistrationRequest request) {
        this.authService.registerReader(request);
        return ResponseEntity.ok("Reader registered successfully");
    }

    @PostMapping("/register/admin")
    @Operation(summary = "Register a new Admin")
    @SecurityRequirements(value = {})
    public ResponseEntity<String> registerAdmin(@RequestBody AdminRegistrationRequest request) {
        this.authService.registerAdmin(request);
        return ResponseEntity.ok("Admin registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT Token")
    @SecurityRequirements(value = {})
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        String token = this.authService.login(request);

        if(token == null) {
            return ResponseEntity.badRequest().body("Invalid username or password.");
        }

        response.addHeader("Authorization", "Bearer " + token);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Login successful.");
    }

    @PostMapping("/login-alt")
    @Operation(summary = "Login to get JWT Token")
    @SecurityRequirements(value = {})
    public ResponseEntity<String> loginAlt(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        String token = this.authService.loginAlt(request);

        if(token == null) {
            return ResponseEntity.badRequest().body("Invalid username or password.");
        }

        response.addHeader("Authorization", "Bearer " + token);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Login successful.");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT Token")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        String newToken = this.authService.refreshAccessToken(token);

        response.addHeader("Authorization", "Bearer " + newToken);

        response.addHeader("Access-Control-Expose-Headers", "Authorization");

        return ResponseEntity.ok("Token refreshed successfully.");
    }

    @GetMapping("/me")
    @Operation(summary = "Get User Token Data")
    public ResponseEntity<UserToken> getUserToken(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(this.authService.getUserToken(token));
    }

}
