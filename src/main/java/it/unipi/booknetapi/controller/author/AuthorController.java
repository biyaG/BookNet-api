package it.unipi.booknetapi.controller.author;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.author.AuthorDeleteCommand;
import it.unipi.booknetapi.command.author.AuthorGetCommand;
import it.unipi.booknetapi.command.author.AuthorListCommand;
import it.unipi.booknetapi.dto.author.AuthorResponse;
import it.unipi.booknetapi.dto.author.AuthorSimpleResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.author.AuthorService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/author")
@Tag(name = "Author", description = "Author endpoints")
public class AuthorController {

    private final AuthService authService;
    private final AuthorService authorService;

    public AuthorController(AuthService authService, AuthorService authorService) {
        this.authService = authService;
        this.authorService = authorService;
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

        boolean result = this.authorService.deleteAuthorById(command);

        return ResponseEntity.ok(result ? "Author deleted successfully" : "Error deleting author");
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
