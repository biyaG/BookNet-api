package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.command.book.BookCreateCommand;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.dto.book.BookGenreGoodReads;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.book.SourceFromEnum;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.review.ReviewSummary;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.service.book.BookService;
import it.unipi.booknetapi.shared.model.Source;
import it.unipi.booknetapi.repository.fetch.ImportLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ImportService {

    private final BookService bookService;
    Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final ImportLogRepository importLogRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final ObjectMapper objectMapper;

    public ImportService(
            ImportLogRepository importLogRepository,
            AuthorRepository authorRepository,
            BookRepository bookRepository,
            GenreRepository genreRepository,
            BookService bookService) {
        this.importLogRepository = importLogRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.objectMapper = new ObjectMapper();
        this.bookService = bookService;
    }


    public String importData(Source source, ImportEntityType importEntityType, MultipartFile file) {
        if(file.isEmpty()) return "File is empty";

        switch (importEntityType) {
            case GOOD_READS_BOOK -> {
                return importGoodReadsBooks(source, file);
            }
            case GOOD_READS_AUTHOR -> {
                return importGoodReadsAuthors(source, file);
            }
            case GOOD_READS_BOOK_GENRE -> {
                return importGoodReadsGenre(source, file);
            }
            default -> {
                return "Unknown entity type";
            }
        }
    }

    /**
     * Entry point: This part is SYNCHRONOUS.
     * It starts the thread and returns a message to the user immediately.
     */
    public String importGoodReadsBooks(Source source, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();
        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }

        // Kick off the BACKGROUND (Async) process
        // We pass the file stream directly to the background worker
        this.startStreamingImport(source, file, fileName, fileContentType, fileUrl);

        return "Import process started for " + fileName + ". You can check the status in the Import Logs.";
    }

    /**
     * Background Worker: This part is ASYNCHRONOUS.
     * It reads, parses, and saves in chunks.
     */
    @Async
    protected void startStreamingImport(Source source, MultipartFile file, String fileName, String contentType, String url) {
        List<String> allSavedIds = new ArrayList<>();
        int totalProcessed = 0;
        int batchSize = 100;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<BookGoodReads> currentBatch = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    // 1. Parse ONE line (Sync within the thread)
                    BookGoodReads rawBook = objectMapper.readValue(line, BookGoodReads.class);
                    currentBatch.add(rawBook);
                    totalProcessed++;

                    // 2. If batch is full, process and save it
                    if (currentBatch.size() >= batchSize) {
                        List<String> batchIds = processAndSaveBatch(currentBatch);
                        allSavedIds.addAll(batchIds);
                        currentBatch.clear(); // Free up memory immediately
                    }
                } catch (Exception e) {
                    logger.error("Skipping a corrupted line in file: {}", e.getMessage());
                }
            }

            // 3. Save any remaining books (last batch)
            if (!currentBatch.isEmpty()) {
                allSavedIds.addAll(processAndSaveBatch(currentBatch));
            }

            // 4. Record Final Success Log
            createImportLog(source, totalProcessed, allSavedIds, fileName, contentType, url, true, "Completed successfully");

        } catch (Exception e) {
            logger.error("Critical error during streaming import: {}", e.getMessage());
            createImportLog(source, totalProcessed, allSavedIds, fileName, contentType, url, false, "Failed: " + e.getMessage());
        }
    }

    /**
     * Helper to map DTOs to Commands and call the BookService
     */
    private List<String> processAndSaveBatch(List<BookGoodReads> batch) {
        List<BookCreateCommand> commands = batch.stream().map(raw -> {
            BookCreateCommand cmd = new BookCreateCommand();
            cmd.setTitle(raw.getTitle());
            cmd.setDescription(raw.getDescription());
            cmd.setIsbn(raw.getIsbn());
            cmd.setIsbn13(raw.getIsbn13());
            cmd.setSource(SourceFromEnum.GOODREADS);
            // Default to empty list if null to prevent NullPointerException
            cmd.setImages(raw.getImageUrl() != null ? List.of(raw.getImageUrl()) : new ArrayList<>());
            cmd.setLanguage(raw.getLanguage_code() != null ? List.of(raw.getLanguage_code()) : List.of(raw.getCountryCode()));
            cmd.setPreview(raw.getUrl());

            //cmd.setExternalId(raw.getExternalId());

            ReviewSummary reviewRating = new ReviewSummary();
            reviewRating.setRating(Float.parseFloat(raw.getAverageRating()));
            reviewRating.setCount(Integer.parseInt(raw.getRating_count()));
            cmd.setRatingReview(reviewRating);

            //cmd.setSimilar_booksIds(raw.getSimilarBooks().externalBookId);
            //I didn't add the above code because we need to add another field on the book-embed or on the book model so that it would be easier for us to link the similar books
            return cmd;
        }).toList();

        // Call your existing service method
        List<BookResponse> responses = bookService.importBooks(commands);
        return responses.stream().map(BookResponse::getId).toList();
    }

    private void createImportLog(Source source, int total, List<String> ids, String name, String type, String url, boolean success, String msg) {
        ImportLog log = ImportLog.builder()
                .operationDate(new Date())
                .source(source)
                .entityType(EntityType.BOOK)
                .numberOfEntities((long) total)
                .numberOfImportedEntities((long) ids.size())
                .ids(List.of())
                .success(success)
                .message(msg)
                .fileName(name)
                .fileType(type)
                .fileUrl(url)
                .build();
        importLogRepository.insert(log);
    }

    private String importGoodReadsAuthors(Source source, MultipartFile file) {
        logger.debug("Importing GoodReads authors");

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();

        // Reading line by line because it is NDJSON
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<AuthorGoodReads> authors = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines if any
                if (line.trim().isEmpty()) continue;

                // Parse the single line into the DTO
                AuthorGoodReads author = objectMapper.readValue(line, AuthorGoodReads.class);
                authors.add(author);
            }

            int authorCount = authors.size();
            String finalFileUrl = fileUrl;
            Runnable task = () -> {
                try {
                    List<Author> authorList = this.authorRepository.importAuthors(authors);

                    ImportLog importLog = ImportLog.builder()
                            .operationDate(new Date())
                            .source(source)
                            .entityType(EntityType.AUTHOR)
                            .numberOfEntities((long) authors.size())
                            .numberOfImportedEntities((long) authorList.size())
                            .ids(authorList.stream().map(Author::getId).toList())
                            .success(true)
                            .message("Successfully processed " + authors.size() + " authors.")
                            .fileName(fileName)
                            .fileType(fileContentType)
                            .fileUrl(finalFileUrl)
                            .build();
                    importLogRepository.insert(importLog);

                    logger.debug("Importing GoodReads authors completed.");
                } catch (Exception e) {
                //e.printStackTrace();
                    String message = "Error processing file: " + e.getMessage();
                    logger.error(message);
                }
            };
            new Thread(task).start();

            return "Successfully processed " + authorCount + " authors.";
        } catch (Exception e) {
            // e.printStackTrace();
            String message = "Error processing file: " + e.getMessage();
            logger.error(message);
            ImportLog importLog = ImportLog.builder()
                    .operationDate(new Date())
                    .source(source)
                    .success(false)
                    .message(message)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl(fileUrl)
                    .build();
            importLogRepository.insert(importLog);
            return message;
        }
    }

    private String importGoodReadsGenre(Source source, MultipartFile file) {
        logger.debug("Importing GoodReads genres");

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();

        // Reading line by line because it is NDJSON
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<BookGenreGoodReads> genreGoodReads = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines if any
                if (line.trim().isEmpty()) continue;

                // Parse the single line into the DTO
                BookGenreGoodReads genre = objectMapper.readValue(line, BookGenreGoodReads.class);
                genreGoodReads.add(genre);
            }

            List<String> genreStrings = new ArrayList<>(genreGoodReads.size());
            for(BookGenreGoodReads genre : genreGoodReads){
                genreStrings.addAll(genre.getGenres().keySet());
            }

            List<Genre> genres = genreStrings.stream()
                    .distinct()
                    .map(genreString -> Genre.builder().name(genreString).build())
                    .toList();

            int genreCount = genres.size();
            String finalFileUrl = fileUrl;
            Runnable task = () -> {
                try {
                    List<Genre> genreList = this.genreRepository.insert(genres);

                    ImportLog importLog = ImportLog.builder()
                            .operationDate(new Date())
                            .source(source)
                            .entityType(EntityType.GENRE)
                            .numberOfEntities((long) genres.size())
                            .numberOfImportedEntities((long) genreList.size())
                            .ids(genreList.stream().map(Genre::getId).toList())
                            .success(true)
                            .message("Successfully processed " + genres.size() + " genres.")
                            .fileName(fileName)
                            .fileType(fileContentType)
                            .fileUrl(finalFileUrl)
                            .build();
                    importLogRepository.insert(importLog);

                    logger.debug("Importing GoodReads genres completed.");
                } catch (Exception e) {
//                    e.printStackTrace();
                    String message = "Error processing file: " + e.getMessage();
                    logger.error(message);
                }
            };
            new Thread(task).start();

            return "Successfully processed " + genreCount + " genres.";
        } catch (Exception e) {
            // e.printStackTrace();
            String message = "Error processing file: " + e.getMessage();
            logger.error(message);
            ImportLog importLog = ImportLog.builder()
                    .operationDate(new Date())
                    .source(source)
                    .success(false)
                    .message(message)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl(fileUrl)
                    .build();
            importLogRepository.insert(importLog);
            return message;
        }
    }

}
