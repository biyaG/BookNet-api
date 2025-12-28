package it.unipi.booknetapi.controller.book;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import it.unipi.booknetapi.command.book.*;
import it.unipi.booknetapi.dto.book.BookCreateRequest;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.dto.book.BookSimpleResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.book.BookService;
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


@RestController
@RequestMapping("/book")
public class BookController {

    private final BookService bookService;
    private final ImportService importService;
    private final AuthService authService;

    public BookController(BookService bookService, ImportService importService, AuthService authService) {
        this.bookService = bookService;
        this.importService = importService;
        this.authService = authService;
    }

    @PostMapping(value = "upload/goodreads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import book", description= "Uploads a file containing books in NDJSON format.")
    public ResponseEntity<String> importBooksFromGoodreads(
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file")MultipartFile file
            ){
        if(file.isEmpty()){
            return ResponseEntity.badRequest().body("File is empty");
        }
        return ResponseEntity.ok(this.importService.importData(Source.GOOD_READS, ImportEntityType.GOOD_READS_BOOK,file));
    }

    @GetMapping("/{idBook}")
    @Operation(summary = "Get book Information")
    public ResponseEntity<BookResponse> getBookById(@PathVariable String idBook){
        BookGetCommand command = BookGetCommand.builder()
                .id(idBook)
                .build();

        return ResponseEntity.ok(this.bookService.getBookById(command));
    }

    @DeleteMapping("/{idBook}")
    @Operation(summary = "Delete Book")
    public ResponseEntity<String> deleteBookById(@PathVariable String idBook, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.ADMIN){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookDeleteCommand command = BookDeleteCommand.builder()
                .id(idBook)
                .build();
        command.setUserToken(userToken);
        boolean result = this.bookService.deleteBookById(command);
        return ResponseEntity.ok(result ? "Book deleted sucessfully" : "Error deleting book");

    }

    /*@PostMapping("/delete")
    @Operation(summary = "Delete Multiple Book")
    public ResponseEntity<String> deleteMultipleBook(@RequestBody List<String> ids, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);
        if(userToken == null || userToken.getRole() != Role.ADMIN){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookDeleteManyCommand command = BookDeleteManyCommand.builder()
                .ids(ids)
                .build();
        command.setUserToken(userToken);

        boolean result = this.bookService.deleteBookById(command);

        return ResponseEntity.ok(result ? "Books deleted successfully" : "Error deleting books");
    }*/

    @PostMapping
    @Operation(summary = "Create book")
    public ResponseEntity<BookResponse> createBook(@RequestBody BookCreateRequest request, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);
        if(userToken == null || userToken.getRole() != Role.ADMIN){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookCreateCommand command = new BookCreateCommand(request);
        command.setUserToken(userToken);

        BookResponse result = this.bookService.saveBook(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "Get all Books")
    public ResponseEntity<PageResult<BookSimpleResponse>> getAllBooks(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size){

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();
        BookListCommand command = BookListCommand.builder()
                .pagination(paginationRequest)
                .build();
        PageResult<BookSimpleResponse> result = this.bookService.getAllBooks(command);
        return ResponseEntity.ok(result);
    }
}