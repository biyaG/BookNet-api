package it.unipi.booknetapi.controller.author;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.author.*;
import it.unipi.booknetapi.dto.author.AuthorCreateRequest;
import it.unipi.booknetapi.dto.author.AuthorResponse;
import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.author.AuthorService;
import it.unipi.booknetapi.service.fetch.ImportEntityType;
import it.unipi.booknetapi.service.fetch.ImportService;
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
@RequestMapping("/author")
@Tag(name = "Author", description = "Author endpoints")
public class AuthorController {

    private final AuthService authService;
    private final AuthorService authorService;
    private final ImportService importService;

    public AuthorController(
            AuthService authService,
            AuthorService authorService,
            ImportService importService
    ) {
        this.authService = authService;
        this.authorService = authorService;
        this.importService = importService;
    }


    @PostMapping(value = "upload/goodreads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import author", description = "Uploads a file containing authors in NDJSON format.")
    public ResponseEntity<String> importAuthorsFromGoodreads(
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        return ResponseEntity.ok(this.importService.importData(Source.GOOD_READS, ImportEntityType.GOOD_READS_AUTHOR, file));
    }


    @GetMapping("/{idAuthor}")
    @Operation(summary = "Get author information")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable String idAuthor) {
        AuthorGetCommand command = AuthorGetCommand.builder()
                .id(idAuthor)
                .build();

        return ResponseEntity.ok(this.authorService.getAuthorById(command));
    }

    @DeleteMapping("/{idAuthor}")
    @Operation(summary = "Delete author")
    public ResponseEntity<String> deleteAuthorById(@PathVariable String idAuthor, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthorDeleteCommand command = AuthorDeleteCommand.builder()
                .id(idAuthor)
                .build();
        command.setUserToken(userToken);

        boolean result = this.authorService.deleteAuthor(command);

        return ResponseEntity.ok(result ? "Author deleted successfully" : "Error deleting author");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi author")
    public ResponseEntity<String> deleteMultiAuthors(@RequestBody List<String> ids, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthorIdsDeleteCommand command = AuthorIdsDeleteCommand.builder()
                .ids(ids)
                .build();
        command.setUserToken(userToken);

        boolean result = this.authorService.deleteMultiAuthors(command);

        return ResponseEntity.ok(result ? "Authors deleted successfully" : "Error deleting authors");
    }

    @PostMapping
    @Operation(summary = "Create author")
    public ResponseEntity<AuthorResponse> createAuthor(@RequestBody AuthorCreateRequest request, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthorCreateCommand command = new AuthorCreateCommand(request);
        command.setUserToken(userToken);

        AuthorResponse result = this.authorService.saveAuthor(command);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "Get all authors")
    public ResponseEntity<PageResult<AuthorSimpleResponse>> getAllAuthors(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();
        AuthorListCommand command = AuthorListCommand.builder()
                .pagination(paginationRequest)
                .build();

        PageResult<AuthorSimpleResponse> result = this.authorService.getAllAuthors(command);

        return ResponseEntity.ok(result);
    }

}
