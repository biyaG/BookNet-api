package it.unipi.booknetapi.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.dto.user.UserResponse;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.author.AuthorService;
import it.unipi.booknetapi.service.user.UserService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "User endpoints")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }


    @Operation(summary = "Get Current User Data")
    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userService.getUserById(userToken.getIdUser()));
    }

    @GetMapping("/{idUser}")
    @Operation(summary = "Get user information")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String idUser) {
        return ResponseEntity.ok(userService.getUserById(idUser));
    }

}
