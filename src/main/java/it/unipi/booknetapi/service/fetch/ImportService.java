package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
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
    private final ObjectMapper objectMapper;

    public ImportService(ImportLogRepository importLogRepository, AuthorRepository authorRepository, BookRepository bookRepository) {
        this.importLogRepository = importLogRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
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
            default -> {
                return "Unknown entity type";
            }
        }
    }

    private String importGoodReadsBooks(Source source, MultipartFile file) {
        logger.info("Importing GoodReads books");

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }

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

            // TODO: Pass 'books' to your service to save them in MongoDB/Neo4j
            // bookService.saveAll(books);

            return "Successfully processed " + books.size() + " books.";
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Error processing file: " + e.getMessage();
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
        logger.info("Importing GoodReads authors");

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }

        List<AuthorGoodReads> authors = new ArrayList<>();

        // Reading line by line because it is NDJSON
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines if any
                if (line.trim().isEmpty()) continue;

                // Parse the single line into the DTO
                AuthorGoodReads author = objectMapper.readValue(line, AuthorGoodReads.class);
                authors.add(author);
            }

            this.authorRepository.importAuthors(authors);

            return "Successfully processed " + authors.size() + " authors.";
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Error processing file: " + e.getMessage();
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
