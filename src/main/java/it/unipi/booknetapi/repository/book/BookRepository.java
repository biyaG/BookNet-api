package it.unipi.booknetapi.repository.book;


import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.ReviewSummary;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class BookRepository implements BookRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(BookRepository.class);

    private final MongoCollection<Book> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final CacheService cacheService;
    private final MeterRegistry registry;
    private final MongoClient mongoClient;

    private static final String CACHE_PREFIX = "book:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public BookRepository(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            CacheService cacheService,
            MeterRegistry registry
    ) {
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("books", Book.class);
        this.neo4jManager = neo4jManager;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    private boolean handleUpdateResult(UpdateResult result, String idBook) {
        if (result.getModifiedCount() > 0) {
            deleteCache(idBook);
            return true;
        }
        return false;
    }

    private static String generateCacheKey(String idBook) {
        return CACHE_PREFIX + idBook;
    }

    private void cacheBook(Book book) {
        this.cacheService.save(generateCacheKey(book.getId().toHexString()), book, CACHE_TTL);
    }

    private void cacheBook(List<Book> books) {
        books.forEach(this::cacheBook);
    }

    private void cacheBookInThread(List<Book> books) {
        Thread thread = new Thread(() -> cacheBook(books));
        thread.start();
    }

    private void deleteCache(String idBook) {
        this.cacheService.delete(generateCacheKey(idBook));
    }

    private void deleteCache(List<String> idBooks) {
        idBooks.forEach(this::deleteCache);
    }

    private void deleteCacheInThread(List<String> idBooks) {
        Thread thread = new Thread(() -> deleteCache(idBooks));
        thread.start();
    }

    @Override
    public Book save(Book book) {
        Objects.requireNonNull(book);
        logger.debug("Saving book: {}", book);

        if(book.getRatingReview() == null) book.setRatingReview(new ReviewSummary(0, 0));

        try(com.mongodb.client.ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();

            try{
                boolean success = false;
                if(book.getId() == null){
                    InsertOneResult insertOneResult = this.mongoCollection.insertOne(book); //Upsert Logic (Update or Insert):
                    success = insertOneResult.wasAcknowledged();
                    if(success){
                        book.setId(Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId().getValue());
                    }
                } else{
                    UpdateResult updateResult = this.mongoCollection.replaceOne(
                            Filters.eq("_id", book.getId()), book
                    );
                    success = updateResult.getModifiedCount() > 0;
                }
                if(success){
                    saveBookToNeo4j(book);
                    session.commitTransaction();

                    this.cacheBook(book);
                    logger.info("Book inserted successfully: {}", book);
                    return book;
                }
            } catch(Exception e){
                session.abortTransaction();
                logger.error("Error during inserting book in neo4j: {}",e.getMessage());
                return null;
            }
        }
        return null;
    }

    public Book insertWithThread(Book book) {
        Objects.requireNonNull(book);

        InsertOneResult insertOneResult = this.mongoCollection.insertOne(book);
        if(insertOneResult.wasAcknowledged()){
            Thread thread = new Thread(() -> saveBookToNeo4j(book));
            thread.start();
        }
        return insertOneResult.wasAcknowledged() ? book : null;
    }

    private void saveBookToNeo4j(Book book) {
        Objects.requireNonNull(book);

        this.registry.timer("neo4j.ops", "query", "save_book").record(() -> {
            try(Session session = this.neo4jManager.getDriver().session()){
                String cypher = "CREATE (b:Book {mid: $bookId, title : $title, ratingAvg: $ratingAvg})";
                session.executeWrite(tx -> {
                    tx.run(
                            cypher,
                            Values.parameters(
                                    "bookId", book.getId().toHexString(),
                                    "title", book.getTitle(),
                                    "ratingAvg", book.getRatingReview()//getRatingReview actually gives you the document so think about this latter
                            )
                    );
                    return null;
                });
            }
        });
 }


    @Override
    public List<Book> saveAll(List<Book> books){
        Objects.requireNonNull(books);

        if(books.isEmpty()) return List.of();

        logger.debug("Inserting many books: {}", books.size());

        try(ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                this.mongoCollection.insertMany(session, books);
                saveBooksToNeo4j(books);
                session.commitTransaction();
                this.cacheBookInThread(books);

                logger.info("Books saved successfully : {}",books.size());
                return books;
            }catch (Exception e){
                session.abortTransaction();
                logger.error("Error occured while inserting in Neo4j: {}",e.getMessage());
            }
        }
        return List.of();
    }

    private void saveBooksToNeo4j(List<Book> books){
        List<Map<String, Object>> neo4jBatch = new ArrayList<>();
        for(Book book : books){
            neo4jBatch.add(Map.of("bookId", book.getId().toHexString(),"title", book.getTitle(), "ratingAvg", book.getRatingReview()));
        }

        if(!neo4jBatch.isEmpty()){
            this.registry.timer("neo4j.ops", "query", "save_reader").record(() -> {
                try(Session session = this.neo4jManager.getDriver().session()){
                    session.executeWrite(
                            tx -> {
                                tx.run(
                                        "UNWIND $books as book CREATE (b:Book {mid:book.bookId, title: book.title, ratingAvg: book.ratingAvg})",
                                        Values.parameters("books", neo4jBatch)
                                );
                                return null;
                            }
                    );
                }
            });
        }

    }

    private Book BookWrittenByAuthorRelationship(Book book){
        Objects.requireNonNull(book);

        this.registry.timer("neo4j.ops", "query", "BookWrittenByAuthorRelationship").record(() -> {
//            String cypher = ""
        });

        return Objects.requireNonNull(book); ///
    }

    @Override
    public boolean deleteBook(String idBook) {
        Objects.requireNonNull(idBook);

        try(ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                DeleteResult deleteResult = this.mongoCollection.deleteOne(Filters.eq("_id", new ObjectId(idBook)));
                if(deleteResult.getDeletedCount() > 0){
                    deleteBookFromNeo4J(idBook);
                    session.commitTransaction();
                    this.deleteCache(idBook);
                    return true;
                }
            }catch(Exception e){
                session.abortTransaction();
            }
        }
        return false;
    }

    private void deleteBookFromNeo4J(String idBook) {
        Objects.requireNonNull(idBook);

        this.registry.timer("neo4j.ops", "query", "delete_book").record(()->{
            try(Session session = this.neo4jManager.getDriver().session()){
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH(b:Book {mid: $bookId}) DETACH DELETE r", //CHANGE
                                    Values.parameters("bookId", idBook)
                            );
                            return null;
                        }
                );
            }
        });
    }

    @Override
    public boolean addReview(Review review) {
        Objects.requireNonNull(review);
        Objects.requireNonNull(review.getBookId());
        Objects.requireNonNull(review.getUser());
        Objects.requireNonNull(review.getUser().getId());

        Book book = this.mongoCollection.find(Filters.eq("_id", review.getBookId())).first();
        if(book == null) return false;
        ReviewSummary oldSummary = book.getRatingReview();

        int newCount = oldSummary.getCount() + 1;
        float newAvg = ((oldSummary.getRating() * oldSummary.getCount()) + review.getRating()) / newCount;

        ReviewSummary updatedSummary = new ReviewSummary(newAvg, newCount);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", review.getBookId()),
                Updates.combine(
                        Updates.set("ratingReview", updatedSummary),
                        Updates.push("reviews", review.getBookId())
                )
        );

        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean removeReview(String idBook, String idUser, String idReview) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idReview);

       UpdateResult updateResult = this.mongoCollection.updateOne(
               Filters.eq("_id", new ObjectId(idBook)),
               Updates.pull("reviews", Filters.eq("_id", new ObjectId(idReview)))
        );

        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateImage(String idBook, String newImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("images", newImageUrl)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean updatePreview(String idBook, String newPreviewImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newPreviewImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("preview", newPreviewImageUrl)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean deletePreview(String idBook, String deletePreviewImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(deletePreviewImageUrl);

        UpdateResult updateResult = mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.pull("preview", deletePreviewImageUrl)
        );

        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean updateSimilarBooks(String idBook, BookEmbed book) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(book);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.set("similar_books", book)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean addGenre(String idBook, GenreEmbed genre) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genre);

        UpdateResult updateResult = this.mongoCollection.updateMany(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("genres", genre)
        );
        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean removeGenre(String idBook, GenreEmbed genre) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(genre);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.pull("_id ", Filters.eq("_id", genre.getId()))
        );

        return handleUpdateResult(updateResult, idBook);
    }

    @Override
    public boolean deleteAllBooks(List<String> idBooks){
        Objects.requireNonNull(idBooks);

        try (ClientSession session = this.mongoClient.startSession()){
            session.startTransaction();
            try{
                DeleteResult deleteResult = this.mongoCollection.deleteMany(
                        Filters.in("_id",idBooks.stream().map(ObjectId::new).toList())
                );
                if(deleteResult.getDeletedCount() > 0){
                    deleteBookBatchFromNeo4j(idBooks);
                    session.commitTransaction();
                    this.deleteCacheInThread(idBooks);
                    return true;
                }
            }catch(Exception e){
                session.abortTransaction();
                return false;
            }
        }
        return false;
    }

    private void deleteBookBatchFromNeo4j(List<String> idBooks){
        this.registry.timer("neo4j.ops", "query", "delete_books").record(() -> {
            try(Session session = this.neo4jManager.getDriver().session()){
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    "OPTIONAL MATCH (b: Book) WHERE b.mid IN $bookIds DETACH DELETE b", /// why bookIds
                                    Values.parameters("bookIds",idBooks)
                            );
                            return null;
                        }
                );
            }
        });
    }

    @Override
    public Optional<Book> findById(String idBook) {
        Objects.requireNonNull(idBook);

        Book cachedBook = this.cacheService.get(generateCacheKey(idBook), Book.class);
        if(cachedBook != null){
            return Optional.of(cachedBook);
        }
        Book book = this.mongoCollection.find(Filters.eq("_id", new ObjectId(idBook))).first();
        if (book != null){
           this.cacheBook(book);
           return Optional.of(book);
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<Book>> findByTitle(String title) {
        Objects.requireNonNull(title);

        List<Book> books = this.mongoCollection
                .find(Filters.eq("title", title))
                .into(new ArrayList<>());

        if(books != null){
            books.forEach(this::cacheBook);
            return Optional.of(books);
        }
        return Optional.empty();
    }

    @Override
    public PageResult<Book> findAll(int page, int size) {
        int skip = page * size;

        List<Book> books = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection.countDocuments();
        cacheBookInThread(books); ////
        return new PageResult<>(books, total, page, size);
    }

}


