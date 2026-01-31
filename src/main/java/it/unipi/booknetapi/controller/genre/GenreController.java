package it.unipi.booknetapi.controller.genre;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.fetch.ImportDataCommand;
import it.unipi.booknetapi.command.genre.*;
import it.unipi.booknetapi.command.stat.AnalyticsGetListCommand;
import it.unipi.booknetapi.dto.genre.GenreCreateRequest;
import it.unipi.booknetapi.dto.genre.GenreResponse;
import it.unipi.booknetapi.dto.stat.ChartDataPointResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.fetch.ImportEntityType;
import it.unipi.booknetapi.service.fetch.ImportService;
import it.unipi.booknetapi.service.genre.GenreService;
import it.unipi.booknetapi.service.source.SourceService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import it.unipi.booknetapi.shared.model.Source;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/genre")
@Tag(name = "Genre", description = "Genre endpoints")
public class GenreController {

    private final AuthService authService;
    private final GenreService genreService;
    private final ImportService importService;
    private final SourceService sourceService;

    public GenreController(
            AuthService authService,
            GenreService genreService,
            ImportService importService,
            SourceService sourceService
    ) {
        this.authService = authService;
        this.genreService = genreService;
        this.importService = importService;
        this.sourceService = sourceService;
    }


    @PostMapping(value = "upload/{idSource}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import genre (Admin only)", description = "Uploads a file containing genres in NDJSON format.")
    public ResponseEntity<String> importGenres(
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
                .importEntityType(ImportEntityType.BOOK_GENRE)
                .file(file)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.importService.importData(command));
    }


    @GetMapping("/{idGenre}")
    @Operation(summary = "Get genre information")
    @SecurityRequirements(value = {})
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable String idGenre) {
        GenreGetCommand command = GenreGetCommand.builder()
                .id(idGenre)
                .build();

        return ResponseEntity.ok(this.genreService.getGenreById(command));
    }

    @DeleteMapping("/{idGenre}")
    @Operation(summary = "Delete genre information (Admin only)")
    public ResponseEntity<String> deleteGenreById(@PathVariable String idGenre, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreDeleteCommand command = GenreDeleteCommand.builder()
                .id(idGenre)
                .userToken(userToken)
                .build();

        boolean result = this.genreService.deleteGenreById(command);

        return ResponseEntity.ok(result ? "Genre deleted successfully" : "Error deleting genre");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi genre (Admin only)")
    public ResponseEntity<String> deleteAllGenres(@RequestBody List<String> ids, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreIdsDeleteCommand command = GenreIdsDeleteCommand.builder()
                .ids(ids)
                .userToken(userToken)
                .build();

        boolean result = this.genreService.deleteAllGenres(command);

        return ResponseEntity.ok(result ? "Genres deleted successfully" : "Error deleting genres");
    }

    @PostMapping
    @Operation(summary = "Create genre (Admin only)")
    public ResponseEntity<GenreResponse> createGenre(@RequestBody GenreCreateRequest request, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GenreCreateCommand command = GenreCreateCommand.builder()
                .name(request.getName())
                .userToken(userToken)
                .build();

        GenreResponse response = this.genreService.saveGenre(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all genres")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<GenreResponse>> getAllGenres(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name
    ) {
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        if(name == null || name.isBlank()) {
            GenreListCommand command = GenreListCommand.builder()
                    .pagination(paginationRequest)
                    .build();

            return ResponseEntity.ok(this.genreService.getAllGenres(command));
        } else {
            GenreSearchCommand command = GenreSearchCommand.builder()
                    .name(name)
                    .pagination(
                            PaginationRequest.builder()
                                    .page(page != null ? page : 0)
                                    .size(size != null ? size : 10)
                                    .build()
                    ).build();

            return ResponseEntity.ok(this.genreService.searchGenre(command));
        }
    }



    @GetMapping("/{idGenre}/analytic/chart")
    @Operation(summary = "Get analytics chart data point", description = "Get list of chart data point relative to this genre.")
    public ResponseEntity<List<ChartDataPointResponse>> getAnalyticsChartData(
            @PathVariable String idGenre,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Accepts "yyyy-MM-dd"
            Date startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Accepts "yyyy-MM-dd"
            Date endDate,
            @RequestParam(required = false) String granularity,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AnalyticsGetListCommand command = AnalyticsGetListCommand.builder()
                .id(idGenre)
                .start(startDate)
                .end(endDate)
                .granularity(granularity)
                .userToken(userToken)
                .build();

        List<ChartDataPointResponse> response = this.genreService.getAnalytics(command);
        if(response == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(response);
    }



}
