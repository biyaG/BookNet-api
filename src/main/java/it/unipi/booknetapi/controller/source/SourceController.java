package it.unipi.booknetapi.controller.source;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.dto.source.SourceCreateRequest;
import it.unipi.booknetapi.dto.source.SourceResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.source.SourceService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/source")
@Tag(name = "Source", description = "Source endpoints")
public class SourceController {

    private final AuthService authService;
    private final SourceService sourceService;

    public SourceController(AuthService authService, SourceService sourceService) {
        this.authService = authService;
        this.sourceService = sourceService;
    }


    @GetMapping
    @Operation(summary = "List of sources")
    @SecurityRequirements(value = {})
    public ResponseEntity<List<SourceResponse>> getSources() {
        return ResponseEntity.ok(this.sourceService.getSources());
    }

    @PostMapping
    @Operation(summary = "Add new Source")
    public ResponseEntity<SourceResponse> postSource(@RequestBody SourceCreateRequest source, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                SourceResponse.builder()
                        .idSource(source.getName().toLowerCase(Locale.ROOT))
                        .name(source.getName())
                        .description(source.getDescription())
                        .build()
        );
    }

    @GetMapping("/{idSource}")
    @Operation(summary = "Get source")
    @SecurityRequirements(value = {})
    public ResponseEntity<SourceResponse> getSource(@PathVariable String idSource) {
        SourceResponse source =  this.sourceService.getSource(idSource);
        if(source == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(source);
    }

    @DeleteMapping("/{idSource}")
    @Operation(summary = "Delete source")
    public ResponseEntity<String> deleteSource(@PathVariable String idSource, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok("Source " + idSource + " deleted successfully.");
    }

}
