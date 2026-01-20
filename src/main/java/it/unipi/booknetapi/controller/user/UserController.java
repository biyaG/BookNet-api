package it.unipi.booknetapi.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.review.ReviewByReaderListCommand;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.dto.user.UserResponse;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.author.AuthorService;
import it.unipi.booknetapi.service.review.ReviewService;
import it.unipi.booknetapi.service.user.UserService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "User endpoints")
public class UserController {

    private final AuthService authService;
    private final ReviewService reviewService;
    private final UserService userService;

    public UserController(
            AuthService authService,
            ReviewService reviewService,
            UserService userService
    ) {
        this.authService = authService;
        this.reviewService = reviewService;
        this.userService = userService;
    }


    @GetMapping
    @Operation(summary = "Get Current User Data")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userService.getUserById(userToken.getIdUser()));
    }

    @GetMapping("/{idUser}")
    @Operation(summary = "Get user information")
    @SecurityRequirements(value = {})
    public ResponseEntity<UserResponse> getUserById(@PathVariable String idUser) {
        return ResponseEntity.ok(userService.getUserById(idUser));
    }


    @GetMapping("/{idUser}/reviews")
    @Operation(summary = "Get user reviews")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<ReviewResponse>> getUserReviews(
            @PathVariable String idUser,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        ReviewByReaderListCommand command = ReviewByReaderListCommand.builder()
                .readerId(idUser)
                .pagination(paginationRequest)
                .build();

        return ResponseEntity.ok(this.reviewService.getReviews(command));
    }

}
