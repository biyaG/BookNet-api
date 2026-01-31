package it.unipi.booknetapi.controller.book;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.book.*;
import it.unipi.booknetapi.command.fetch.ImportDataCommand;
import it.unipi.booknetapi.command.review.ReviewByBookListCommand;
import it.unipi.booknetapi.command.review.ReviewCreateCommand;
import it.unipi.booknetapi.command.stat.AnalyticsGetListCommand;
import it.unipi.booknetapi.command.user.ReaderAddBookToShelfCommand;
import it.unipi.booknetapi.command.user.ReaderRemoveBookInShelfCommand;
import it.unipi.booknetapi.command.user.ReaderUpdateBookStatusInShelfCommand;
import it.unipi.booknetapi.dto.book.*;
import it.unipi.booknetapi.dto.review.ReviewCreateRequest;
import it.unipi.booknetapi.dto.review.ReviewResponse;
import it.unipi.booknetapi.dto.stat.ChartDataPointResponse;
import it.unipi.booknetapi.dto.user.ReaderBookShelfUpdateStatusRequest;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.book.BookService;
import it.unipi.booknetapi.service.fetch.ImportEntityType;
import it.unipi.booknetapi.service.fetch.ImportService;
import it.unipi.booknetapi.service.review.ReviewService;
import it.unipi.booknetapi.service.source.SourceService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import it.unipi.booknetapi.shared.model.Source;

import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;


@RestController
@Tag(name = "Book", description = "Book endpoints")
@RequestMapping("/book")
public class BookController {

    private final AuthService authService;
    private final BookService bookService;
    private final ImportService importService;
    private final ReviewService reviewService;
    private final SourceService sourceService;


    public BookController(
            AuthService authService,
            BookService bookService,
            ImportService importService,
            ReviewService reviewService,
            SourceService sourceService
    ) {
        this.bookService = bookService;
        this.importService = importService;
        this.authService = authService;
        this.reviewService = reviewService;
        this.sourceService = sourceService;
    }

    @PostMapping(value = "upload/{idSource}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import book (Admin only)", description= "Uploads a file containing books in NDJSON format.")
    public ResponseEntity<String> importBooks(
            @PathVariable String idSource,
            @RequestHeader("Authorization") String token,
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ){
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(file.isEmpty()){
            return ResponseEntity.badRequest().body("File is empty");
        }

        Source source = this.sourceService.getEnumSource(idSource);

        if(source == null) return ResponseEntity.badRequest().body("Invalid source");

        ImportDataCommand command = ImportDataCommand.builder()
                .source(source)
                .importEntityType(ImportEntityType.BOOK)
                .file(file)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.importService.importData(command));
    }

    @PostMapping(value = "upload/similarity/{idSource}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import book (Admin only)", description= "Uploads a file containing books in NDJSON format.")
    public ResponseEntity<String> importBooksSimilarity(
            @PathVariable String idSource,
            @RequestHeader("Authorization") String token,
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ){
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(file.isEmpty()){
            return ResponseEntity.badRequest().body("File is empty");
        }

        Source source = this.sourceService.getEnumSource(idSource);

        if(source == null) return ResponseEntity.badRequest().body("Invalid source");

        ImportDataCommand command = ImportDataCommand.builder()
                .source(source)
                .importEntityType(ImportEntityType.BOOK_SIMILARITY)
                .file(file)
                .userToken(userToken)
                .build();

        return ResponseEntity.ok(this.importService.importData(command));
    }

    @PostMapping(value = "upload/genre/{idSource}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import book genre (Admin only)", description= "Uploads a file containing books genre in NDJSON format.")
    public ResponseEntity<String> importBooksGenre(
            @PathVariable String idSource,
            @RequestHeader("Authorization") String token,
            @Parameter(
                    description = "The NDJSON file to upload",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ){
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(file.isEmpty()){
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

    @GetMapping("/{idBook}")
    @Operation(summary = "Get book Information")
    @SecurityRequirements(value = {})
    public ResponseEntity<BookResponse> getBookById(@PathVariable String idBook){
        BookGetCommand command = BookGetCommand.builder()
                .id(idBook)
                .build();

        return ResponseEntity.ok(this.bookService.getBookById(command));
    }

    @GetMapping("/{idBook}/reviews")
    @Operation(summary = "Get book reviews")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<ReviewResponse>> getBookReviews(
            @PathVariable String idBook,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ){
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        ReviewByBookListCommand command = ReviewByBookListCommand.builder()
                .bookId(idBook)
                .pagination(paginationRequest)
                .build();

        return ResponseEntity.ok(this.reviewService.getReviews(command));
    }

    @PostMapping("/{idBook}/reviews")
    @Operation(summary = "add a review to a book (Reader only)", description = "Add a review to a book.")
    public ResponseEntity<ReviewResponse> addBookReview(
            @PathVariable String idBook,
            @RequestBody ReviewCreateRequest request,
            @RequestHeader("Authorization") String token
    ){
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReviewCreateCommand command = ReviewCreateCommand.builder()
                .bookId(idBook)
                .rating(request.getRating())
                .comment(request.getComment())
                .userToken(userToken)
                .build();

        ReviewResponse result = this.reviewService.saveReview(command);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{idBook}/shelf")
    @Operation(summary = "add a book to a shelf (Reader only)", description = "Add a book to a shelf.")
    public ResponseEntity<String> addBookInShelf(
            @PathVariable String idBook,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReaderAddBookToShelfCommand command = ReaderAddBookToShelfCommand.builder()
                .idBook(idBook)
                .userToken(userToken)
                .build();

        boolean added = this.bookService.addBookInShelf(command);

        return ResponseEntity.ok(added ? "Book added successfully" : "Error adding book");
    }

    @PutMapping("/{idBook}/shelf")
    @Operation(summary = "update a book status to a shelf (Reader only)", description = "Update a book status in shelf.")
    public ResponseEntity<String> updateBookStatusInShelf(
            @PathVariable String idBook,
            @RequestBody ReaderBookShelfUpdateStatusRequest request,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReaderUpdateBookStatusInShelfCommand command = ReaderUpdateBookStatusInShelfCommand.builder()
                .idBook(idBook)
                .status(request.getStatus())
                .userToken(userToken)
                .build();

        boolean updated = this.bookService.updateBookStatusInShelf(command);

        return ResponseEntity.ok(updated ? "Book updated successfully" : "Error updating book");
    }

    @DeleteMapping("/{idBook}/shelf")
    @Operation(summary = "remove a book from a shelf (Reader only)", description = "Remove a book from a shelf.")
    public ResponseEntity<String> removeBookFromShelf(
            @PathVariable String idBook,
            @RequestHeader("Authorization") String token
    ) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ReaderRemoveBookInShelfCommand command = ReaderRemoveBookInShelfCommand.builder()
                .idBook(idBook)
                .userToken(userToken)
                .build();

        boolean updated = this.bookService.removeBookFromShelf(command);

        return ResponseEntity.ok(updated ? "Book removed successfully" : "Error removing book");
    }

    @DeleteMapping("/{idBook}")
    @Operation(summary = "Delete Book (Admin only)")
    public ResponseEntity<String> deleteBookById(@PathVariable String idBook, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookDeleteCommand command = BookDeleteCommand.builder()
                .id(idBook)
                .userToken(userToken)
                .build();
        boolean result = this.bookService.deleteBookById(command);

        return ResponseEntity.ok(result ? "Book deleted sucessfully" : "Error deleting book");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete Multiple Book (Admin only)")
    public ResponseEntity<String> deleteMultipleBook(@RequestBody List<ObjectId> ids, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookDeleteManyCommand command = BookDeleteManyCommand.builder()
                .ids(ids)
                .userToken(userToken)
                .build();

        boolean result = this.bookService.deleteManyBooks(command);

        return ResponseEntity.ok(result ? "Books deleted successfully" : "Error deleting books");
    }

    @PostMapping
    @Operation(summary = "Create book (Admin only)")
    public ResponseEntity<BookResponse> createBook(@RequestBody BookCreateRequest request, @RequestHeader("Authorization") String token){
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookCreateCommand command = new BookCreateCommand(request);
        command.setUserToken(userToken);

        BookResponse result = this.bookService.saveBook(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "Get all Books")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<BookSimpleResponse>> getAllBooks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name
    ){

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        if(name == null || name.isBlank()) {
            BookListCommand command = BookListCommand.builder()
                    .pagination(paginationRequest)
                    .build();
            PageResult<BookSimpleResponse> result = this.bookService.getAllBooks(command);
            return ResponseEntity.ok(result);
        } else {
            BookSearchCommand command = BookSearchCommand.builder()
                    .pagination(paginationRequest)
                    .title(name)
                    .build();
            PageResult<BookSimpleResponse> result = this.bookService.searchBooks(command);
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("by/genre/{idGenre}")
    @Operation(summary = "Get all Books by Genre")
    @SecurityRequirements(value = {})
    public ResponseEntity<PageResult<BookEmbedResponse>> booksByGenre(
            @PathVariable String idGenre,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ){

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        BookGetByGenreCommand command = BookGetByGenreCommand.builder()
                .idGenre(idGenre)
                .pagination(paginationRequest)
                .build();

        return ResponseEntity.ok(this.bookService.getBooksByGenre(command));
    }


    @GetMapping("/{idBook}/analytic/chart")
    @Operation(summary = "Get analytics chart data point", description = "Get list of chart data point relative to this book.")
    public ResponseEntity<List<ChartDataPointResponse>> getAnalyticsChartData(
            @PathVariable String idBook,
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
                .id(idBook)
                .start(startDate)
                .end(endDate)
                .granularity(granularity)
                .userToken(userToken)
                .build();

        List<ChartDataPointResponse> response = this.bookService.getAnalytics(command);
        if(response == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/random")
    @Operation(summary = "Get random books")
    @SecurityRequirements(value = {})
    public ResponseEntity<List<BookRecommendationResponse>> randomBooks(@RequestParam(required = false) Integer size) {
        BookRandomCommand command = BookRandomCommand.builder()
                .limit(size)
                .build();

        return ResponseEntity.ok(this.bookService.getRandomBooks(command));
    }

    @GetMapping("/popular/rating")
    @Operation(summary = "Get popular rating books")
    @SecurityRequirements(value = {})
    public ResponseEntity<List<BookRecommendationResponse>> popularRatingBooks(@RequestParam(required = false) Integer size, @RequestParam(required = false) Long dayAgo) {
        BookPopularByRatingCommand command = BookPopularByRatingCommand.builder()
                .limit(size)
                .dayAgo(dayAgo)
                .build();

        return ResponseEntity.ok(this.bookService.getPopularBooksRating(command));
    }

    @GetMapping("/popular/shelf")
    @Operation(summary = "Get popular books in shelf")
    @SecurityRequirements(value = {})
    public ResponseEntity<List<BookRecommendationResponse>> popularBooksInShelf(@RequestParam(required = false) Integer size) {
        BookPopularByShelfCommand command = BookPopularByShelfCommand.builder()
                .limit(size)
                .build();

        return ResponseEntity.ok(this.bookService.getPopularBooksShelf(command));
    }

    @GetMapping("/recommendation")
    @Operation(summary = "Get recommendation books (Reader only)")
    public ResponseEntity<List<BookRecommendationResponse>> recommendationBooks(@RequestHeader("Authorization") String token, @RequestParam(required = false) Integer size) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Reader){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookRecommendationCommand command = BookRecommendationCommand.builder()
                .idUser(userToken.getIdUser())
                .limit(size)
                .build();

        return ResponseEntity.ok(this.bookService.getCollaborativeBooks(command));
    }


}
