package it.unipi.booknetapi.repository.book;


import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import it.unipi.booknetapi.shared.lib.cache.CacheService;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.book.BookEmbed;
import it.unipi.booknetapi.model.review.ReviewEmbed;
import it.unipi.booknetapi.shared.lib.database.MongoManager;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.neo4j.driver.Values.parameters;

public class BookRepository implements BookRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(BookRepository.class);

    private final MongoManager mongoManager;
    private final MongoCollection<Book> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "book:";
    private static final int CACHE_TTL = 3600; // 1 hour

    public BookRepository(MongoManager mongoManager, MongoCollection<Book> mongoCollection, Neo4jManager neo4jManager, CacheService cacheService) {
        this.mongoManager = mongoManager;
        this.mongoCollection = mongoCollection;
        this.neo4jManager = neo4jManager;
        this.cacheService = cacheService;
    }

    private static String generateCacheKey(String idBook) {
        return CACHE_PREFIX + idBook;
    }

    private void cacheBook(Book book) {
        this.cacheService.save(generateCacheKey(book.get_id().toHexString()), book, CACHE_TTL);
    }

    private void cacheBook(List<Book> books) {
        books.forEach(this::cacheBook);
    }

    private void cacheUserInThread(List<Book> books) {
        Thread thread = new Thread(() -> cacheBook(books));
        thread.start();
    }

    private void deleteCache(String idBook) {
        this.cacheService.delete(generateCacheKey(idBook));
    }


    @Override
    public Book save(Book book) {
        Objects.requireNonNull(book);
        logger.debug("Saving book: {}", book);

        try(com.mongodb.client.ClientSession session = this.mongoManager.getMongoClient().startSession()){
            session.startTransaction();

            try{
                boolean success = false;
                if(book.get_id() == null){
                    InsertOneResult insertOneResult = this.mongoCollection.insertOne(book);
                    success = insertOneResult.wasAcknowledged();
                    if(success){
                        book.set_id(Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId().getValue());
                    }
                } else{
                    UpdateResult updateResult = this.mongoCollection.replaceOne(
                            Filters.eq("_id", book.get_id()),
                            book
                    );
                    success = updateResult.getModifiedCount() > 0;
                }
                if(success){
                    if(book instanceof Book){
                        saveBookToNeo4j(book);
                    }
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

    private void saveBookToNeo4j(Book book) {
    }

    @Override
    public boolean deleteBook(String idBook) {
        Objects.requireNonNull(idBook);

        try(ClientSession session = this.mongoManager.getMongoClient().startSession()){
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
        try(Session session = this.neo4jManager.getSession()){
            session.executeWrite(
                    tx->tx.run(
                            "OPTIONAL MATCH (b:Book {mid: $bookId}) DETACH DELETE b",
                            parameters("bookId", idBook)
                    )
            );
        }
    }

    @Override
    public boolean addReview(String idBook, String idUser, ReviewEmbed reviewEmbed) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(reviewEmbed);


        return false;
    }

    @Override
    public boolean removeReview(String idBook, String idUser, String idReview) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(idUser);
        Objects.requireNonNull(idReview);

        return false;
    }

    @Override
    public boolean updateImage(String idBook, String newImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.set("imageUrl", newImageUrl)
        );
        if(updateResult.getModifiedCount() > 0){
            this.deleteCache(idBook);
            return true;
        }
        return false;
    }

    @Override
    public boolean updatePreview(String idBook, String newPreviewImageUrl) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(newPreviewImageUrl);

        UpdateResult updateResult = this.mongoCollection.updateOne(
                Filters.eq("_id", new ObjectId(idBook)),
                Updates.push("previewUrl", newPreviewImageUrl)
        );
        if(updateResult.getModifiedCount() > 0){
            this.deleteCache(idBook);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateListSimilarBooks(String idBook, List<BookEmbed> similar_books) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(similar_books);

        return false;
    }

    @Override
    public boolean updateSimilarBooks(String idBook, BookEmbed book) {
        Objects.requireNonNull(idBook);
        Objects.requireNonNull(book);

        return false;
    }

    @Override
    public boolean addGenre(String idGenre, String name) {
        Objects.requireNonNull(idGenre);
        Objects.requireNonNull(name);


        return false;
    }

    @Override
    public PageResult<Book> findAll(int page, int size) {
        int skip = page * size;

        List<Book> books = this.mongoCollection.find().skip(skip).limit(size).into(List.of());

        long total = this.mongoCollection.countDocuments();
        cacheBookInThread(books);
        return new PageResult<>(books, total, page, size);
    }

    private void cacheBookInThread(List<Book> books) {
        Thread thread = new Thread(() -> cacheBook(books));
        thread.start();
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
    public Optional<Book> findByTitle(String title) {
        Objects.requireNonNull(title);

        Book book = this.mongoCollection.find(Filters.eq("title", title)).first(); ///////////////
        if(book != null){
            this.cacheBook(book);
            return Optional.of(book);
        }

        return Optional.empty();
    }
}


