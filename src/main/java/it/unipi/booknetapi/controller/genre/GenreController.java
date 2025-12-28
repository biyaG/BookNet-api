package it.unipi.booknetapi.controller.genre;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.genre.*;
import it.unipi.booknetapi.dto.genre.GenreCreateRequest;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.genre.GenreService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import it.unipi.booknetapi.shared.model.SearchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/genre")
@Tag(name = "Genre", description = "Genre endpoints")
public class GenreController {

    private final AuthService authService;
    private final GenreService genreService;

    public GenreController(AuthService authService, GenreService genreService) {
        this.authService = authService;
        this.genreService = genreService;
    }

    @GetMapping("/{idGenre}")
    @Operation(summary = "Get genre information")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable String idGenre) {
        GenreGetCommand command = GenreGetCommand.builder()
                .id(idGenre)
                .build();

        return ResponseEntity.ok(this.genreService.getGenreById(command));
    }

    @DeleteMapping("/{idGenre}")
    @Operation(summary = "Delete genre information")
    public ResponseEntity<String> deleteGenreById(@PathVariable String idGenre, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreDeleteCommand command = GenreDeleteCommand.builder()
                .id(idGenre)
                .build();
        command.setUserToken(userToken);

        boolean result = this.genreService.deleteGenreById(command);

        return ResponseEntity.ok(result ? "Genre deleted successfully" : "Error deleting genre");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi genre")
    public ResponseEntity<String> deleteAllGenres(@RequestBody List<String> ids, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreIdsDeleteCommand command = GenreIdsDeleteCommand.builder()
                .ids(ids)
                .build();
        command.setUserToken(userToken);

        boolean result = this.genreService.deleteAllGenres(command);

        return ResponseEntity.ok(result ? "Genres deleted successfully" : "Error deleting genres");
    }

    @PostMapping
    @Operation(summary = "Create genre")
    public ResponseEntity<GenreResponse> createGenre(@RequestBody GenreCreateRequest request, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreCreateCommand command = GenreCreateCommand.builder()
                .name(request.getName())
                .build();

        GenreResponse response = this.genreService.saveGenre(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all genres")
    public ResponseEntity<PageResult<GenreResponse>> getAllGenres(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        GenreListCommand command = GenreListCommand.builder()
                .pagination(PaginationRequest.builder()
                        .page(page != null ? page : 0)
                        .size(size != null ? size : 10)
                        .build())
                .build();

        return ResponseEntity.ok(this.genreService.getAllGenres(command));
    }

    @PostMapping("search")
    @Operation(summary = "search genre")
    public ResponseEntity<PageResult<GenreResponse>> searchGenres(
            @RequestBody SearchRequest request,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if(request.getName() == null || request.getName().isBlank()) return ResponseEntity.badRequest().build(); {}

        GenreSearchCommand command = GenreSearchCommand.builder()
                .name(request.getName())
                .pagination(
                        PaginationRequest.builder()
                                .page(page != null ? page : 0)
                                .size(size != null ? size : 10)
                                .build()
                ).build();

        return ResponseEntity.ok(this.genreService.searchGenre(command));
    }

}
