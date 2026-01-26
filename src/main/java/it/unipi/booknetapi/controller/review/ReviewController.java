package it.unipi.booknetapi.controller.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.review.ReviewDeleteCommand;
import it.unipi.booknetapi.command.review.ReviewGetCommand;
import it.unipi.booknetapi.command.review.ReviewIdsDeleteCommand;
import it.unipi.booknetapi.command.review.ReviewListCommand;
import it.unipi.booknetapi.dto.review.ReviewResponse;
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
    @Operation(summary = "Import reviews", description = "Uploads a file containing reviews in NDJSON format.")
    public ResponseEntity<String> importAuthorsFromGoodreads(
            @PathVariable String idSource,
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Source source = this.sourceService.getEnumSource(idSource);

        if(source == null) return ResponseEntity.badRequest().body("Invalid source");

        return ResponseEntity.ok(this.importService.importData(source, ImportEntityType.REVIEW, file));
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

    @DeleteMapping("/{idReview}")
    @Operation(summary = "Delete Review")
    public ResponseEntity<String> deleteReviewById(
            @PathVariable String idReview,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReviewDeleteCommand command = ReviewDeleteCommand.builder()
                .id(idReview)
                .build();
        command.setUserToken(userToken);

        boolean result = this.reviewService.deleteReview(command);

        return ResponseEntity.ok(result ? "Review deleted successfully" : "Error deleting review");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi review")
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
                .build();
        command.setUserToken(userToken);

        boolean result = this.reviewService.deleteReview(command);

        return ResponseEntity.ok(result ? "Reviews deleted successfully" : "Error deleting reviews");
    }

    @GetMapping
    @Operation(summary = "Get all reviews")
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
                .build();
        command.setUserToken(userToken);

        return ResponseEntity.ok(this.reviewService.getReviews(command));
    }

}
