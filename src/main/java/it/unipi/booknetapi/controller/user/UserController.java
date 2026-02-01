package it.unipi.booknetapi.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.review.ReviewByReaderListCommand;
import it.unipi.booknetapi.command.stat.UserMonthlyStatGetCommand;
import it.unipi.booknetapi.command.stat.UserMonthlyStatListCommand;
import it.unipi.booknetapi.command.stat.UserYearlyStatGetCommand;
import it.unipi.booknetapi.command.user.*;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.dto.stat.UserMonthlyStatResponse;
import it.unipi.booknetapi.dto.stat.UserYearlyStatResponse;
import it.unipi.booknetapi.dto.user.*;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.review.ReviewService;
import it.unipi.booknetapi.service.user.UserService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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


    @GetMapping("/migrate")
    @Operation(summary = "Migrate user from mongodb to neo4j (Admin only)", description = "Migrate all users (Reader and Reviewer) from mongodb to neo4j.")
    public ResponseEntity<String> migrateUsers(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        this.userService.migrate();

        return ResponseEntity.ok("Starting migration");
    }


    @GetMapping("/migrate/reader")
    @Operation(summary = "Migrate reader from mongodb to neo4j (Admin only)", description = "Migrate all readers from mongodb to neo4j.")
    public ResponseEntity<String> migrateReader(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        this.userService.migrateReaders();

        return ResponseEntity.ok("Starting migration");
    }


    @GetMapping("/migrate/reviewer")
    @Operation(summary = "Migrate reviewer from mongodb to neo4j (Admin only)", description = "Migrate all reviewers from mongodb to neo4j.")
    public ResponseEntity<String> migrateReviewer(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        this.userService.migrateReviewers();

        return ResponseEntity.ok("Starting migration");
    }


    @GetMapping("/admin")
    @Operation(summary = "Get list of admin (Admin only)")
    public ResponseEntity<PageResult<AdminResponse>> getAdminUser(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null  || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 100)
                .build();

        AdminListCommand command = AdminListCommand.builder()
                .pagination(paginationRequest)
                .userToken(userToken)
                .build();

        PageResult<AdminResponse> response = this.userService.list(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reader")
    @Operation(summary = "Get list of reader (Admin only)")
    public ResponseEntity<PageResult<ReaderResponse>> getReaderUser(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null  || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 100)
                .build();

        ReaderListCommand command = ReaderListCommand.builder()
                .pagination(paginationRequest)
                .userToken(userToken)
                .build();

        PageResult<ReaderResponse> response = this.userService.list(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviewer")
    @Operation(summary = "Get list of reviewer (Admin only)")
    public ResponseEntity<PageResult<ReviewerResponse>> getReviewerUser(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null  || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 100)
                .build();

        ReviewerListCommand command = ReviewerListCommand.builder()
                .pagination(paginationRequest)
                .userToken(userToken)
                .build();

        PageResult<ReviewerResponse> response = this.userService.list(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Data")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserGetCommand command = UserGetCommand.builder()
                .id(userToken.getIdUser())
                .userToken(userToken)
                .build();

        UserResponse userResponse = this.userService.get(command);

        if(userResponse == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping
    @Operation(summary = "update user name")
    public ResponseEntity<UserResponse> updateUser(
            @RequestBody UserUpdateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserUpdateCommand command = UserUpdateCommand.builder()
                .userToken(userToken)
                .name(request.getName())
                .build();

        UserResponse userResponse = this.userService.update(command);

        if(userResponse == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/preference")
    @Operation(summary = "update user")
    public ResponseEntity<ReaderPreferenceResponse> updatePreference(
            @RequestBody ReaderPreferenceRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReaderUpdatePreferenceCommand command = ReaderUpdatePreferenceCommand.builder()
                .userToken(userToken)
                .authors(request.getAuthors())
                .genres(request.getGenres())
                .languages(request.getLanguages())
                .build();

        ReaderPreferenceResponse readerPreferenceResponse = this.userService.update(command);

        if(readerPreferenceResponse == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(readerPreferenceResponse);
    }

    @GetMapping("/{idUser}")
    @Operation(summary = "Get user information")
    @SecurityRequirements(value = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = {
                                    AdminResponse.class,
                                    ReaderResponse.class,
                                    ReviewerResponse.class,
                                    UserResponse.class
                            })
                    )
            ),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable String idUser) {
        UserGetCommand command = UserGetCommand.builder()
                .id(idUser)
                .build();

        UserResponse userResponse = this.userService.get(command);

        if(userResponse == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(userResponse);
    }


    @GetMapping("/reader/{idUser}")
    @Operation(summary = "Get user information")
    @SecurityRequirements(value = {})
    public ResponseEntity<ReaderComplexResponse> getUserReaderById(@PathVariable String idUser) {
        ReaderComplexResponse reader = userService.getReaderById(idUser);
        if(reader == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(reader);
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


    // stats


    @GetMapping("/stat/monthly")
    @Operation(summary = "Get user monthly stat")
    public ResponseEntity<UserMonthlyStatResponse> getUserMonthlyStat(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserMonthlyStatGetCommand command = UserMonthlyStatGetCommand.builder()
                .idUser(userToken.getIdUser())
                .year(year)
                .month(month)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.userService.getStat(command));
    }

    @GetMapping("/stat/monthly/list")
    @Operation(summary = "Get user monthly stat by year")
    public ResponseEntity<List<UserMonthlyStatResponse>> getUserMonthlyStat(
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserMonthlyStatListCommand command = UserMonthlyStatListCommand.builder()
                .idUser(userToken.getIdUser())
                .year(year)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.userService.getStats(command));
    }

    @GetMapping("/stat/yearly")
    @Operation(summary = "Get user yearly stat")
    public ResponseEntity<UserYearlyStatResponse> getUserYearlyStat(
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserYearlyStatGetCommand command = UserYearlyStatGetCommand.builder()
                .idUser(userToken.getIdUser())
                .year(year)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.userService.getYearlyStat(command));
    }


}
