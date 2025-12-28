package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.dto.book.BookGenreGoodReads;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.shared.model.Source;
import it.unipi.booknetapi.repository.fetch.ImportLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            GenreRepository genreRepository
    ) {
        this.importLogRepository = importLogRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.objectMapper = new ObjectMapper();
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

    private String importGoodReadsBooks(Source source, MultipartFile file) {
        logger.debug("Importing GoodReads books");

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();

        List<BookGoodReads> books = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines if any
                if (line.trim().isEmpty()) continue;

                // Parse the single line into the DTO
                BookGoodReads book = objectMapper.readValue(line, BookGoodReads.class);
                books.add(book);
            }

            int bookCount = books.size();


            String finalFileUrl = fileUrl;
            Runnable task = () -> {
                try {
                    // TODO: Pass 'books' to your service to save them in MongoDB/Neo4j
                    // bookService.saveAll(books);

                    ImportLog importLog = ImportLog.builder()
                            .operationDate(new Date())
                            .source(source)
                            .entityType(EntityType.BOOK)
                            .numberOfEntities((long) books.size())
                            .numberOfImportedEntities((long) 0) // TODO: replace with correct number
                            .ids(List.of()) // TODO: replace with correct ids
                            .success(true)
                            .message("Successfully processed " + books.size() + " books.")
                            .fileName(fileName)
                            .fileType(fileContentType)
                            .fileUrl(finalFileUrl)
                            .build();
                    importLogRepository.insert(importLog);

                    logger.debug("Importing GoodReads books completed.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            new Thread(task).start();

            return "Successfully processed " + bookCount + " books.";
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
                    e.printStackTrace();
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
                    e.printStackTrace();
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
