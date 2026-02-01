package it.unipi.booknetapi.controller.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.fetch.ImportDataCommand;
import it.unipi.booknetapi.command.review.*;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.dto.review.ReviewUpdateRequest;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.fetch.ImportEntityType;
import it.unipi.booknetapi.service.fetch.ImportService;
import it.unipi.booknetapi.service.review.ReviewService;
import it.unipi.booknetapi.service.source.SourceService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import it.unipi.booknetapi.shared.model.Source;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/review")
@Tag(name = "Review", description = "Review endpoints")
public class ReviewController {

    private final AuthService authService;
    private final ImportService importService;
    private final ReviewService reviewService;
    private final SourceService sourceService;

    public ReviewController(
            AuthService authService,
            ImportService importService,
            ReviewService reviewService,
            SourceService sourceService
    ) {
        this.authService = authService;
        this.importService = importService;
        this.reviewService = reviewService;
        this.sourceService = sourceService;
    }


    @PostMapping(value = "upload/{idSource}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import reviews (Admin only)", description = "Uploads a file containing reviews in NDJSON format.")
    public ResponseEntity<String> importAuthorsFromGoodreads(
            @PathVariable String idSource,
            @RequestHeader("Authorization") String token,
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ) {
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Source source = this.sourceService.getEnumSource(idSource);

        if(source == null) return ResponseEntity.badRequest().body("Invalid source");

        ImportDataCommand command = ImportDataCommand.builder()
                .source(source)
                .importEntityType(ImportEntityType.REVIEW)
                .file(file)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.importService.importData(command));
    }



    @GetMapping("/migrate")
    @Operation(summary = "Migrate review from mongodb to neo4j (Admin only)", description = "Migrates all reviews from mongodb to neo4j.")
    public ResponseEntity<String> migrateGenres(@RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        this.reviewService.migrate();

        return ResponseEntity.ok("Starting migration");
    }



    @GetMapping("/{idReview}")
    @Operation(summary = "Get Review by ID")
    @SecurityRequirements(value = {})
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable String idReview) {
        ReviewGetCommand command = ReviewGetCommand.builder()
                .id(idReview)
                .build();

        return ResponseEntity.ok(this.reviewService.getReviewById(command));
    }

    @PostMapping("/{idReview}")
    @Operation(summary = "Update review (Reader only)")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String idReview,
            @RequestBody ReviewUpdateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReviewGetCommand commandGet = ReviewGetCommand.builder()
                .id(idReview)
                .build();
        ReviewResponse review = this.reviewService.getReviewById(commandGet);

        if(review == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if(Objects.equals(userToken.getIdUser(), review.getUser().getIdUser()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ReviewUpdateCommand command = ReviewUpdateCommand.builder()
                .id(idReview)
                .rating(request.getRating())
                .comment(request.getComment())
                .userToken(userToken)
                .build();

        ReviewResponse reviewResponse = this.reviewService.updateReview(command);

        if(reviewResponse == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(reviewResponse);
    }

    @DeleteMapping("/{idReview}")
    @Operation(summary = "Delete Review")
    public ResponseEntity<String> deleteReviewById(
            @PathVariable String idReview,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(userToken.getRole() != Role.Admin) {
            ReviewGetCommand command = ReviewGetCommand.builder()
                    .id(idReview)
                    .build();

            ReviewResponse review = this.reviewService.getReviewById(command);

            if(review == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            if(Objects.equals(userToken.getIdUser(), review.getUser().getIdUser()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReviewDeleteCommand command = ReviewDeleteCommand.builder()
                .id(idReview)
                .userToken(userToken)
                .build();

        boolean result = this.reviewService.deleteReview(command);

        return ResponseEntity.ok(result ? "Review deleted successfully" : "Error deleting review");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi review (Admin only)")
    public ResponseEntity<String> deleteMultiReviews(
            @RequestBody List<String> ids,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReviewIdsDeleteCommand command = ReviewIdsDeleteCommand.builder()
                .ids(ids)
                .userToken(userToken)
                .build();

        boolean result = this.reviewService.deleteReview(command);

        return ResponseEntity.ok(result ? "Reviews deleted successfully" : "Error deleting reviews");
    }

    @GetMapping
    @Operation(summary = "Get all reviews (Admin only)")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        ReviewListCommand command = ReviewListCommand.builder()
                .pagination(paginationRequest)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.reviewService.getReviews(command));
    }

}
