package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.command.fetch.ImportDataCommand;
import it.unipi.booknetapi.dto.author.AuthorGoodReads;
import it.unipi.booknetapi.dto.book.*;
import it.unipi.booknetapi.dto.fetch.ParameterFetch;
import it.unipi.booknetapi.dto.review.InteractionGoodReads;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.notification.Notification;
import it.unipi.booknetapi.model.notification.NotificationEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.repository.notification.NotificationRepository;
import it.unipi.booknetapi.repository.review.ReviewRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.ExternalId;
import it.unipi.booknetapi.shared.model.Source;
import it.unipi.booknetapi.repository.fetch.ImportLogRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final ImportLogRepository importLogRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    private final ObjectMapper objectMapper;

    public ImportService(
            ImportLogRepository importLogRepository,
            AuthorRepository authorRepository,
            BookRepository bookRepository,
            GenreRepository genreRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.importLogRepository = importLogRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;

        this.objectMapper = new ObjectMapper();
    }


    public String importData(ImportDataCommand command) {
        if(command == null || !command.isValid()) return "Invalid command";

        if(command.getFile().isEmpty()) return "File is empty";

        String fileUrl;
        try {
            fileUrl = command.getFile().getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = command.getFile().getOriginalFilename();
        }
        // String finalFileUrl = fileUrl;
        String fileName = command.getFile().getOriginalFilename();
        String fileContentType = command.getFile().getContentType();

        switch (command.getSource()) {
            case GOOD_READS -> {
                switch (command.getImportEntityType()) {
                    case BOOK -> {
                        List<BookGoodReads> books = extractDataFromFile(command.getSource(), command.getFile(), BookGoodReads.class);

                        ParameterFetch<BookGoodReads> parameterFetch = ParameterFetch.<BookGoodReads>builder()
                                .idUser(command.getUserToken().getIdUser())
                                .source(command.getSource())
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
                    case AUTHOR -> {
                        List<AuthorGoodReads> authors = extractDataFromFile(command.getSource(), command.getFile(), AuthorGoodReads.class);

                        ParameterFetch<AuthorGoodReads> parameterFetch = ParameterFetch.<AuthorGoodReads>builder()
                                .idUser(command.getUserToken().getIdUser())
                                .source(command.getSource())
                                .entityType(EntityType.AUTHOR)
                                .fileUrl(fileUrl)
                                .fileName(fileName)
                                .fileContentType(fileContentType)
                                .data(authors)
                                .build();
                        processSaveImport(parameterFetch, this::importGoodReadsAuthors);

                        return "Successfully processed import authors.";
                    }
                    case BOOK_GENRE -> {

                        List<BookGenreGoodReads> bookGenres = extractDataFromFile(command.getSource(), command.getFile(), BookGenreGoodReads.class);
                        if(bookGenres == null) return "Error during read file";

                        ParameterFetch<BookGenreGoodReads> parameterFetch = ParameterFetch.<BookGenreGoodReads>builder()
                                .idUser(command.getUserToken().getIdUser())
                                .source(command.getSource())
                                .entityType(EntityType.GENRE)
                                .fileUrl(fileUrl)
                                .fileName(fileName)
                                .fileContentType(fileContentType)
                                .data(bookGenres)
                                .build();
                        processSaveImport(parameterFetch, this::importGoodReadsGenre);

                        return "Successfully processed import Genre";
                    }

                    case BOOK_SIMILARITY -> {
                        List<BookGoodReads> bookSimilarity = extractDataFromFile(command.getSource(), command.getFile(), BookGoodReads.class);
                        if(bookSimilarity == null) return "Error during read file";

                        // 1. Create the parameter fetch with the correct type
                        ParameterFetch<BookGoodReads> parameterFetch = ParameterFetch.<BookGoodReads>builder()
                                .idUser(command.getUserToken().getIdUser())
                                .source(command.getSource())
                                .entityType(EntityType.BOOK)
                                .fileUrl(fileUrl)
                                .fileName(fileName)
                                .fileContentType(fileContentType)
                                .data(bookSimilarity)
                                .build();

                        // 2. Only call the similarity import method
                        processSaveImport(parameterFetch, this::importGoodReadsSimilarBooks);

                        return "Successfully processed import book similarity";
                    }

                    case REVIEW -> {
                        List<InteractionGoodReads> interactionGoodReads =  extractDataFromFile(command.getSource(), command.getFile(), InteractionGoodReads.class);
                        if(interactionGoodReads == null) return "Error during read file";

                        ParameterFetch<InteractionGoodReads> parameterFetch = ParameterFetch.<InteractionGoodReads>builder()
                                .idUser(command.getUserToken().getIdUser())
                                .source(command.getSource())
                                .entityType(EntityType.REVIEW)
                                .fileUrl(fileUrl)
                                .fileName(fileName)
                                .fileContentType(fileContentType)
                                .data(interactionGoodReads)
                                .build();

                        processSaveImport(parameterFetch, this::importGoodReadsReviews);

                        return "Successfully processed import reviews";
                    }

                    default -> {
                        return "Unknown entity type";
                    }
                }
            }

            default -> {
                return "Unknown source";
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
            String message = "[SERVICE] [IMPORT] [EXTRACT DATA FROM FILE] Error processing file: " + e.getMessage();
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

        try {
            if(parameterFetch.getIdUser() != null) {
                Notification newNotification = Notification.builder()
                        .title("Import " + parameterFetch.getEntityType() + " from " + parameterFetch.getSource())
                        .message(message)
                        .userId(new ObjectId(parameterFetch.getIdUser()))
                        .entityId(importLog.getId())
                        .entityType(EntityType.IMPORT_LOG)
                        .createdAt(new Date())
                        .read(false)
                        .build();
                Notification notification = this.notificationRepository.insert(newNotification);
                if(notification != null) {
                    this.userRepository.addNotification(parameterFetch.getIdUser(), new NotificationEmbed(notification));
                }
            }
        } catch (Exception ignored) {}
    }

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

        parameterFetch.setEntityType(EntityType.BOOK_AUTHOR);
        logFetch(
                parameterFetch,
                (long) goodBooks.size(),
                (long) bookAuthors.size(),
                bookAuthors.keySet().stream().toList(),
                true,
                "Successfully processed " + bookAuthors.size() + " books authors."
        );

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [BOOK] Importing GoodReads books completed.");
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

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [AUTHORS] Importing GoodReads authors completed.");
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

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [GENRE] Importing GoodReads genres completed.");

        parameterFetch.setEntityType(EntityType.BOOK_GENRE);
        processSaveImport(parameterFetch, this::importGoodReadsBookGenre);
    }

    private void importGoodReadsBookGenre(ParameterFetch<BookGenreGoodReads> parameterFetch) {
        List<String> externBookIds = parameterFetch.getData().stream().map(BookGenreGoodReads::getBookId).toList();
        List<Book> books = this.bookRepository.findByGoodReadsExternIds(externBookIds);

        Map<String, Book> mapExternIdBook = books.stream()
                .filter(b -> b.getExternalId() != null && b.getExternalId().getGoodReads() != null)
                .collect(Collectors.toMap(
                        b -> b.getExternalId().getGoodReads(),
                        b -> b
                ));


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

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [BOOK GENRE] Importing GoodReads book genres completed.");
    }

    private void importGoodReadsSimilarBooks(ParameterFetch<BookGoodReads> parameterFetch) {

        // 1. Collect all Goodreads IDs
        List<String> allBookIds = parameterFetch.getData()
                .stream()
                .map(BookGoodReads::getBookId)
                .toList();

        // 2. Load all books from DB
        List<Book> books = bookRepository.findByGoodReadsExternIds(allBookIds);

        // 3. Map Goodreads ID → Book
        Map<String, Book> mapExternIdBook = books.stream()
                .filter(b -> b.getExternalId() != null && b.getExternalId().getGoodReads() != null)
                .collect(Collectors.toMap(
                        b -> b.getExternalId().getGoodReads(),
                        b -> b
                ));

        long updated = 0L;

        Map<String, List<BookEmbed>> mapBooks = new HashMap<>();
        for (BookGoodReads item : parameterFetch.getData()) {

            if (item.getBookId() == null || item.getSimilarBooks() == null || item.getSimilarBooks().isEmpty())
                continue;

            // 4. Find main book
            Book mainBook = mapExternIdBook.get(item.getBookId());
            if (mainBook == null) continue;

            // 5. Map similar books
            List<BookEmbed> similarEmbeds = item.getSimilarBooks()
                    .stream()
                    .map(mapExternIdBook::get)
                    .filter(Objects::nonNull)
                    .map(book -> {
                        BookEmbed embed = new BookEmbed();
                        embed.setId(book.getId());
                        embed.setTitle(book.getTitle());
                        embed.setDescription(book.getDescription());
                        embed.setImages(book.getImages());
                        return embed;
                    })
                    .toList();

            if (similarEmbeds.isEmpty()) continue;

            mapBooks.put(mainBook.getId().toHexString(), similarEmbeds);

            updated++;
        }

        boolean success = this.bookRepository.updateSimilarBooks(mapBooks);

        logFetch(
                parameterFetch,
                (long) allBookIds.size(),
                success ? updated : 0,
                new ArrayList<>(),
                true,
                "Successfully processed similar books."
        );

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [BOOK SIMILARITY] Importing GoodReads similar books completed.");
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

    private Reviewer generateUserReviewerFromGoodReads(String goodReadsUserId) {
        Faker faker = new Faker();

        ExternalId externalId = new ExternalId();
        externalId.setGoodReads(goodReadsUserId);

        Reviewer reader = new Reviewer();
        reader.setExternalId(externalId);
        reader.setName(faker.name().fullName());

        reader.setRole(Role.Reviewer);

        return reader;
    }

    private Map<String, Reviewer> findOrGenerateReviewers(List<String> externUserIds) {
        if(externUserIds.isEmpty()) return new HashMap<>();

        List<Reviewer> users = this.userRepository.findByGoodReadsExternIds(externUserIds);

        List<String> existingUsersExternIds = users.stream()
                .filter(u -> u.getExternalId() != null)
                .map(u -> u.getExternalId().getGoodReads())
                .toList();

        List<String> usersToCreate = new ArrayList<>(externUserIds);
        usersToCreate.removeAll(existingUsersExternIds);

        List<Reviewer> newUsers = usersToCreate.stream()
                .map(this::generateUserReviewerFromGoodReads)
                .toList();

        List<Reviewer> insertedUsers = this.userRepository.insert(newUsers).reversed();

        Map<String, Reviewer> mapExternIdUser = users.stream()
                .filter(u -> u.getExternalId() != null && u.getExternalId().getGoodReads() != null)
                .collect(Collectors.toMap(
                        u -> u.getExternalId().getGoodReads(),
                        u -> u,
                        (existing, replacement) -> existing
                ));
        mapExternIdUser.putAll(
                insertedUsers.stream()
                        .filter(u -> u.getExternalId() != null && u.getExternalId().getGoodReads() != null)
                        .collect(Collectors.toMap(
                                u -> u.getExternalId().getGoodReads(),
                                u -> u,
                                (existing, replacement) -> existing
                        ))
        );

        return mapExternIdUser;
    }

    private void importGoodReadsReviews(ParameterFetch<InteractionGoodReads> parameterFetch) {
        // List<String> externReviewIds = parameterFetch.getData().stream().map(InteractionGoodReads::getReviewId).toList();
        List<String> externBookIds = parameterFetch.getData().stream().map(InteractionGoodReads::getBookId).toList();
        List<String> externUserIds = parameterFetch.getData().stream().map(InteractionGoodReads::getUserId).toList();

        List<Book> books = this.bookRepository.findByGoodReadsExternIds(externBookIds);

        Map<String, Book> mapExternIdBook = books.stream()
                .filter(b -> b.getExternalId() != null && b.getExternalId().getGoodReads() != null)
                .collect(Collectors.toMap(
                        b -> b.getExternalId().getGoodReads(),
                        b -> b
                ));

        Map<String, Reviewer> mapExternIdUser = findOrGenerateReviewers(externUserIds);

        List<Review> reviews = new ArrayList<>(parameterFetch.getData().size());
        List<ReviewerRead> reviewersRead = new ArrayList<>(parameterFetch.getData().size());
        for(InteractionGoodReads interactionGoodReads : parameterFetch.getData()) {
            if(mapExternIdBook.containsKey(interactionGoodReads.getBookId()) && mapExternIdUser.containsKey(interactionGoodReads.getUserId())) {
                ExternalId externalId = ExternalId.builder()
                        .goodReads(interactionGoodReads.getReviewId())
                        .build();

                Review review = Review.builder()
                        .bookId(mapExternIdBook.get(interactionGoodReads.getBookId()).getId())
                        .user(mapExternIdUser.get(interactionGoodReads.getUserId()).toEmbed())
                        .rating(interactionGoodReads.getRating())
                        .comment(interactionGoodReads.getReviewTextIncomplete())
                        .dateAdded(interactionGoodReads.getDateAdded())
                        .dateUpdated(interactionGoodReads.getDateUpdated())
                        .source(Source.GOOD_READS)
                        .externalId(externalId)
                        .build();

                reviews.add(review);

                ReviewerRead read = ReviewerRead.builder()
                        .userId(mapExternIdUser.get(interactionGoodReads.getUserId()).getId())
                        .bookId(mapExternIdBook.get(interactionGoodReads.getBookId()).getId())
                        .isRead(interactionGoodReads.getIsRead())
                        .readAt(interactionGoodReads.getReadAt())
                        .startedAt(interactionGoodReads.getStartedAt())
                        .build();

                reviewersRead.add(read);
            }
        }

        List<Review> reviewsSaved = this.reviewRepository.insertFromGoodReads(reviews);

        this.reviewRepository.importGoodReadsReviewsRead(reviewersRead);

        logFetch(
                parameterFetch,
                (long) reviews.size(),
                (long) reviewsSaved.size(),
                reviewsSaved.stream().map(Review::getId).toList(),
                true,
                "Successfully processed " + reviews.size() + " reviews."
        );

        logger.debug("[SERVICE] [IMPORT] [GOOD READS] [REVIEWS] Importing GoodReads reviews completed.");
    }


}
