package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.command.book.BookCreateCommand;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.dto.book.BookAuthorGoodReads;
import it.unipi.booknetapi.dto.book.BookGenreGoodReads;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.dto.book.BookResponse;
import it.unipi.booknetapi.dto.fetch.ParameterFetch;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.SourceFromEnum;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.ReviewSummary;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.service.book.BookService;
import it.unipi.booknetapi.shared.model.Source;
import it.unipi.booknetapi.repository.fetch.ImportLogRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }
        // String finalFileUrl = fileUrl;
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();

        switch (importEntityType) {
            case GOOD_READS_BOOK -> {
                List<BookGoodReads> books = extractDataFromFile(source, file, BookGoodReads.class);

                ParameterFetch<BookGoodReads> parameterFetch = ParameterFetch.<BookGoodReads>builder()
                        .source(source)
                        .entityType(EntityType.BOOK)
                        .fileUrl(fileUrl)
                        .fileName(fileName)
                        .fileContentType(fileContentType)
                        .data(books)
                        .build();
                processSaveImport(parameterFetch, this::importGoodReadsBooks);

                // return importGoodReadsBooks(source, file);
                return "Successfully processed import books.";
            }
            case GOOD_READS_AUTHOR -> {
                List<AuthorGoodReads> authors = extractDataFromFile(source, file, AuthorGoodReads.class);

                ParameterFetch<AuthorGoodReads> parameterFetch = ParameterFetch.<AuthorGoodReads>builder()
                        .source(source)
                        .entityType(EntityType.AUTHOR)
                        .fileUrl(fileUrl)
                        .fileName(fileName)
                        .fileContentType(fileContentType)
                        .data(authors)
                        .build();
                processSaveImport(parameterFetch, this::importGoodReadsAuthors);

                return "Successfully processed import authors.";
            }
            case GOOD_READS_BOOK_GENRE -> {

                List<BookGenreGoodReads> bookGenres = extractDataFromFile(source, file, BookGenreGoodReads.class);
                if(bookGenres == null) return "Error during read file";

                ParameterFetch<BookGenreGoodReads> parameterFetch = ParameterFetch.<BookGenreGoodReads>builder()
                        .source(source)
                        .entityType(EntityType.GENRE)
                        .fileUrl(fileUrl)
                        .fileName(fileName)
                        .fileContentType(fileContentType)
                        .data(bookGenres)
                        .build();
                processSaveImport(parameterFetch, this::importGoodReadsGenre);

                parameterFetch.setEntityType(EntityType.BOOK_GENRE);
                processSaveImport(parameterFetch, this::importGoodReadsBookGenre);

                return "Successfully processed import Genre";
            }
            default -> {
                return "Unknown entity type";
            }
        }
    }

    private <T> void processSaveImport(ParameterFetch<T> parameterFetch, Consumer<ParameterFetch<T>> consumer) {
        Thread thread = new Thread(() -> {consumer.accept(parameterFetch);});
        thread.start();
    }

    private <T> List<T> extractDataFromFile(Source source, MultipartFile file, Class<T> clazz) {
        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }
        String fileName = file.getOriginalFilename();
        String fileContentType = file.getContentType();


        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<T> result = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                T item = objectMapper.readValue(line, clazz);
                result.add(item);
            }

            return result;
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
            return null;
        }
    }

    private <T> void logFetch(
            ParameterFetch<T>  parameterFetch,
            Long numberOfEntities,
            Long numberOfImportedEntities,
            List<ObjectId> ids,
            Boolean success,
            String message
    ) {
        ImportLog importLog = ImportLog.builder()
                .operationDate(new Date())
                .source(parameterFetch.getSource())
                .entityType(parameterFetch.getEntityType())
                .numberOfEntities(numberOfEntities)
                .numberOfImportedEntities(numberOfImportedEntities)
                .ids(ids)
                .success(success)
                .message(message)
                .fileName(parameterFetch.getFileName())
                .fileType(parameterFetch.getFileContentType())
                .fileUrl(parameterFetch.getFileUrl())
                .build();
        importLogRepository.insert(importLog);
    }


    // ---

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
            cmd.setLanguage(raw.getLanguageCode() != null ? List.of(raw.getLanguageCode()) : List.of(raw.getCountryCode()));
            cmd.setPreview(List.of(raw.getUrl()));

            //cmd.setExternalId(raw.getExternalId());

            ReviewSummary reviewRating = new ReviewSummary();
            reviewRating.setRating(Float.parseFloat(raw.getAverageRating()));
            reviewRating.setCount(Integer.parseInt(raw.getRatingCount()));
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



    // ---

    private void importGoodReadsBooks(ParameterFetch<BookGoodReads> parameterFetch) {
        List<BookGoodReads> goodBooks = parameterFetch.getData()
                .stream()
                .filter(
                        b ->
                                b.getIsbn() != null && !b.getIsbn().isBlank()
                                        && b.getIsbn13() != null && !b.getIsbn13().isBlank()
                                        && b.getBookId() != null && !b.getBookId().isBlank()
                ).toList();

        List<Book> bookList = this.bookRepository.importBooks(goodBooks);

        logFetch(
                parameterFetch,
                (long) parameterFetch.getData().size(),
                (long) bookList.size(),
                bookList.stream().map(Book::getId).toList(),
                true,
                "Successfully processed " + parameterFetch.getData().size() + " books."
        );

        Map<String, ObjectId> bookIdMap = new HashMap<>(bookList.size());
        for(Book book : bookList) {
            bookIdMap.put(book.getExternalId().getGoodReads(), book.getId());
        }

        List<String> authorExternIds = new ArrayList<>();
        for(BookGoodReads goodBook : goodBooks) {
            if(goodBook.getAuthors() != null) {
                List<String> externIds = goodBook.getAuthors()
                        .stream()
                        .map(BookAuthorGoodReads::getAuthorId)
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .toList();
                authorExternIds.addAll(externIds);
            }
        }

        List<Author> authors = this.authorRepository.findByExternGoodReadIds(authorExternIds);
        Map<String, AuthorEmbed> authorEmbedMap = new HashMap<>();
        for(Author author : authors) {
            authorEmbedMap.put(author.getExternalId().getGoodReads(), new AuthorEmbed(author));
        }

        Map<ObjectId, List<AuthorEmbed>> bookAuthors = new HashMap<>();
        for(BookGoodReads goodBook : goodBooks) {
            if(bookIdMap.containsKey(goodBook.getBookId()) && goodBook.getAuthors() != null && !goodBook.getAuthors().isEmpty()) {
                List<AuthorEmbed> authorEmbeds = new ArrayList<>(goodBook.getAuthors().size());
                for(BookAuthorGoodReads author: goodBook.getAuthors()) {
                    if(author.getAuthorId() != null && !author.getAuthorId().isBlank() && authorEmbedMap.containsKey(author.getAuthorId())) {
                        authorEmbeds.add(authorEmbedMap.get(author.getAuthorId()));
                    }
                }
                bookAuthors.put(bookIdMap.get(goodBook.getBookId()), authorEmbeds);
            }
        }

        this.bookRepository.importBooksAuthors(bookAuthors);

        logger.debug("Importing GoodReads books completed.");
    }

    private void importGoodReadsAuthors(ParameterFetch<AuthorGoodReads> parameterFetch) {
        List<Author> authorList = this.authorRepository.importAuthors(parameterFetch.getData());

        logFetch(
                parameterFetch,
                (long) parameterFetch.getData().size(),
                (long) authorList.size(),
                authorList.stream().map(Author::getId).toList(),
                true,
                "Successfully processed " + parameterFetch.getData().size() + " authors."
        );

        logger.debug("Importing GoodReads authors completed.");
    }

    private void importGoodReadsGenre(ParameterFetch<BookGenreGoodReads> parameterFetch) {
        List<String> genreStrings = new ArrayList<>(parameterFetch.getData().size());
        for(BookGenreGoodReads genre : parameterFetch.getData()){
            genreStrings.addAll(genre.getGenres().keySet());
        }

        List<Genre> genres = genreStrings.stream()
                .distinct()
                .map(genreString -> Genre.builder().name(genreString).build())
                .toList();

        List<Genre> genreList = this.genreRepository.insert(genres);

        logFetch(
                parameterFetch,
                (long) genres.size(),
                (long) genreList.size(),
                genreList.stream().map(Genre::getId).toList(),
                true,
                "Successfully processed " + genres.size() + " genres."
        );

        logger.debug("Importing GoodReads genres completed.");
    }

    private void importGoodReadsBookGenre(ParameterFetch<BookGenreGoodReads> parameterFetch) {
        List<String> externBookIds = parameterFetch.getData().stream().map(BookGenreGoodReads::getBookId).toList();
        List<Book> books = this.bookRepository.findByGoodReadsExternIds(externBookIds);
        Map<String, Book> mapExternIdBook = books.stream()
                .filter(b -> b.getExternalId() != null && b.getExternalId().getGoodReads() != null)
                .collect(Collectors.toMap(Book::getTitle, book -> book));

        List<String> allGenreNames = parameterFetch.getData().stream()
                .filter(item -> item.getGenres() != null)
                .flatMap(item -> item.getGenres().keySet().stream())
                .toList();
        Map<String, Genre> mapNameGenre = resolveGenres(allGenreNames);

        Long updated = 0L;
        for(BookGenreGoodReads item : parameterFetch.getData()){
            if(item.getBookId() == null || item.getGenres() == null || item.getGenres().isEmpty()) continue;

            Book book = mapExternIdBook.get(item.getBookId());
            if(book == null) continue;

            List<GenreEmbed> genreEmbeds = item.getGenres()
                    .keySet()
                    .stream()
                    .map(mapNameGenre::get)
                    .filter(Objects::nonNull)
                    .map(GenreEmbed::new)
                    .toList();

            if(genreEmbeds.isEmpty()) continue;

            if(this.bookRepository.updateGenres(book.getId().toHexString(), genreEmbeds)) updated++;
        }

        logFetch(
                parameterFetch,
                (long) externBookIds.size(),
                updated,
                new ArrayList<>(),
                true,
                "Successfully processed " + externBookIds.size() + " book genres."
        );

        logger.debug("Importing GoodReads book genres completed.");
    }

    private Map<String, Genre> resolveGenres(List<String> names) {
        // Find existing genres
        List<Genre> existingGenres = this.genreRepository.findByName(names);

        Map<String, Genre> result = existingGenres.stream()
                .collect(Collectors.toMap(Genre::getName, g -> g));

        // Add missing genres
        List<String> newGeneNames = names.stream()
                .filter( name -> !result.containsKey(name))
                .toList();
        List<Genre> newGenres = newGeneNames.stream()
                .map(name -> Genre.builder().name(name).build())
                .toList();
        Map<String, Genre> resultNewGenres = newGenres.stream()
                .collect(Collectors.toMap(Genre::getName, g -> g));

        result.putAll(resultNewGenres);

        return result;
    }

}
