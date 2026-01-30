package it.unipi.booknetapi.repository.genre;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import io.micrometer.core.instrument.MeterRegistry;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import it.unipi.booknetapi.shared.lib.database.Neo4jManager;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class GenreRepository implements GenreRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(GenreRepository.class);

    private final Integer batchSize;

    private final MongoClient mongoClient;
    private final MongoCollection<Genre> mongoCollection;
    private final Neo4jManager neo4jManager;
    private final MeterRegistry registry;


    public GenreRepository(
            AppConfig appConfig,
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            Neo4jManager neo4jManager,
            MeterRegistry registry
    ) {
        this.batchSize = appConfig.getBatchSize() != null ? appConfig.getBatchSize() : 100;
        this.mongoClient = mongoClient;
        this.mongoCollection = mongoDatabase.getCollection("genres", Genre.class);
        this.neo4jManager = neo4jManager;
        this.registry = registry;
    }

    /**
     * @param genre the genre to insert
     * @return the inserted genre
     */
    @Override
    public Genre insert(Genre genre) {
        Objects.requireNonNull(genre);

        logger.debug("[REPOSITORY] [GENRE] [INSERT] [ONE] genre: {}", genre);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                InsertOneResult insertOneResult = this.mongoCollection
                        .insertOne(mongoSession, genre);

                if(insertOneResult.getInsertedId() != null) {
                    saveGenreInNeo4j(genre);
                    mongoSession.commitTransaction();
                    return genre;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
            }
        }

        return null;
    }

    private void saveGenreInNeo4j(Genre genre) {
        Objects.requireNonNull(genre);

        this.registry.timer("neo4j.ops", "query", "save_genre").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                String cypher = "CREATE (g:Genre {mid: $id, name: $name})";
                session.executeWrite(
                        tx -> {
                            tx.run(
                                    cypher,
                                    Values.parameters(
                                            "id", genre.getId().toHexString(),
                                            "name", genre.getName()
                                    )
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param genres genre list to insert
     * @return the inserted genre list
     */
    @Override
    public List<Genre> insert(List<Genre> genres) {
        Objects.requireNonNull(genres);

        if(genres.isEmpty()) return List.of();

        List<Genre> distinctInput = genres.stream()
                .filter(g -> g.getName() != null)
                .collect(Collectors.toMap(Genre::getName, g -> g, (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();

        if(distinctInput.isEmpty()) return List.of();

        List<String> names = distinctInput.stream().map(Genre::getName).toList();

        logger.debug("[REPOSITORY] [GENRE] [INSERT] [MANY] genres size: {}", genres.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                List<Genre> existingGenres = this.mongoCollection
                        .find(mongoSession, Filters.in("name", names))
                        .into(new ArrayList<>());

                Set<String> existingNames = existingGenres.stream()
                        .map(Genre::getName)
                        .collect(Collectors.toSet());

                List<Genre> newGenres = distinctInput.stream()
                        .filter(g -> !existingNames.contains(g.getName()))
                        .toList();

                if(newGenres.isEmpty()) return existingGenres;

                InsertManyResult insertManyResult = this.mongoCollection.insertMany(mongoSession, newGenres);

                if(!insertManyResult.getInsertedIds().isEmpty()) {
                    saveGenresInNeo4j(newGenres);
                    mongoSession.commitTransaction();

                    return Stream.concat(existingGenres.stream(), newGenres.stream())
                            .collect(Collectors.toList());
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during transaction: {}", e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    private void saveGenresInNeo4j(List<Genre> genres) {
        Objects.requireNonNull(genres);

        List<Map<String, Object>> neo4jBatch = new ArrayList<>();
        genres.forEach(genre -> neo4jBatch.add(Map.of("id", genre.getId().toHexString(), "name", genre.getName())));

        this.registry.timer("neo4j.ops", "query", "save_genres").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            String query = """
                                    UNWIND $genres as genre
                                    MERGE (g:Genre {name: genre.name})
                                    ON CREATE SET g.mid = genre.id
                                    ON MATCH SET g.mid = genre.id
                                    """;
                            tx.run(
                                    query,
                                    Values.parameters("genres", neo4jBatch)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idGenre genre id to delete
     * @return true if the genre was deleted successfully, false otherwise
     */
    @Override
    public boolean delete(String idGenre) {
        Objects.requireNonNull(idGenre);

        if(!ObjectId.isValid(idGenre)) return false;

        logger.debug("[REPOSITORY] [GENRE] [DELETE] [BY ID] id: {}", idGenre);

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                DeleteResult deleteResult = this.mongoCollection.deleteOne(mongoSession, Filters.eq("_id", new ObjectId(idGenre)));

                if(deleteResult.getDeletedCount() > 0) {
                    deleteGenreFromNeo4j(idGenre);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    public void deleteGenreFromNeo4j(String idGenre) {
        Objects.requireNonNull(idGenre);

        this.registry.timer("neo4j.ops", "query", "delete_genre").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            String query = "MATCH (g:Genre {mid: $id}) DETACH DELETE g";
                            tx.run(
                                    query,
                                    Values.parameters("id", idGenre)
                            );
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idGenres genre ids to delete
     * @return true if all genres were deleted successfully, false otherwise
     */
    @Override
    public boolean delete(List<String> idGenres) {
        Objects.requireNonNull(idGenres);

        List<String> ids = idGenres.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        if(ids.isEmpty()) return true;

        logger.debug("[REPOSITORY] [GENRE] [DELETE] [BY IDS] genres ids: {}", ids.size());

        try (ClientSession mongoSession = this.mongoClient.startSession()) {
            mongoSession.startTransaction();

            try {
                List<ObjectId> oIds = ids.stream().map(ObjectId::new).toList();
                DeleteResult deleteResult = this.mongoCollection.deleteMany(mongoSession, Filters.in("_id", oIds));

                if(deleteResult.getDeletedCount() > 0) {
                    deleteAllGenresFromNeo4j(ids);
                    mongoSession.commitTransaction();
                    return true;
                } else {
                    mongoSession.abortTransaction();
                }
            } catch (Exception e) {
                mongoSession.abortTransaction();
                logger.error("Error during transaction: {}", e.getMessage());
            }
        }

        return false;
    }

    private void deleteAllGenresFromNeo4j(List<String> idGenres) {
        Objects.requireNonNull(idGenres);

        List<String> ids = idGenres.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        if(ids.isEmpty()) return ;

        this.registry.timer("neo4j.ops", "query", "delete_genre").record(() -> {
            try (Session session = this.neo4jManager.getDriver().session()) {
                session.executeWrite(
                        tx -> {
                            String query = "MATCH (g:Genre) WHERE g.mid IN $ids DETACH DELETE g";
                            tx.run(query, Values.parameters("ids", ids));
                            return null;
                        }
                );
            }
        });
    }

    /**
     * @param idGenre genre id to retrieve
     * @return optional genre if found, empty otherwise
     */
    @Override
    public Optional<Genre> findById(String idGenre) {
        Objects.requireNonNull(idGenre);

        if(!ObjectId.isValid(idGenre)) return Optional.empty();

        logger.debug("[REPOSITORY] [GENRE] [FIND] [BY ID] id: {}", idGenre);

        Genre genre = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idGenre)))
                .first();

        return genre != null ? Optional.of(genre) : Optional.empty();
    }


@Override
    public List<GenreEmbed> findAllById(List<String> genreIds) {

        if (genreIds == null || genreIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<ObjectId> objectIds = genreIds.stream()
                .map(ObjectId::new)
                .toList();

        List<GenreEmbed> genres = new ArrayList<>();

        mongoCollection
                .find(Filters.in("_id", objectIds))
                .forEach(genre -> genres.add(new GenreEmbed(genre)
                ));

        return genres;
    }

    /**
     * @param idGenres genre ids to retrieve
     * @return list of genres if found, empty otherwise
     */
    @Override
    public List<Genre> find(List<String> idGenres) {
        Objects.requireNonNull(idGenres);

        List<String> ids = idGenres.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(ObjectId::isValid)
                .toList();

        if(ids.isEmpty()) return List.of();

        logger.debug("[REPOSITORY] [GENRE] [FIND] [BY IDS] ids: {}", ids.size());

        List<ObjectId> oIds = ids.stream().map(ObjectId::new).toList();

        return this.mongoCollection
                .find(Filters.in("_id", oIds))
                .into(new ArrayList<>());
    }

    /**
     * @param page page number
     * @param size size of page
     * @return list of genres with pagination
     */
    @Override
    public PageResult<Genre> findAll(int page, int size) {
        logger.debug("[REPOSITORY] [GENRE] [FIND] [ALL] page: {}, size: {}", page, size);

        int skip = page * size;

        List<Genre> genres = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments();

        return new PageResult<>(genres, total, page, size);
    }

    /**
     * @param GenreNames name's genre
     * @return list of genres where name is in GenreNames
     */
    @Override
    public List<Genre> findByName(List<String> GenreNames) {

        if (GenreNames.isEmpty()) return List.of();

        logger.debug("[REPOSITORY] [GENRE] [FIND] [BY NAME] names: {}", GenreNames.size());

        return this.mongoCollection
                .find(Filters.in("name", GenreNames))
                .into(new ArrayList<>());
    }

    /**
     * @param name name of genre to search
     * @param page page number
     * @param size size of page
     * @return list of genres with pagination
     */
    @Override
    public PageResult<Genre> search(String name, int page, int size) {
        Objects.requireNonNull(name);

        logger.debug("[REPOSITORY] [GENRE] [SEARCH] name: {}, page: {}, size: {}", name, page, size);

        int skip = page * size;

        List<Genre> genres = this.mongoCollection
                .find(
                        Filters.regex("name", "^" + name + "$", "i")
                ).skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(
                        Filters.regex("name", "^" + name + "$", "i")
                );

        return new PageResult<>(genres, total, page, size);
    }

    /**
     *
     */
    @Override
    public void migrate() {
        logger.debug("[REPOSITORY] [GENRE] [MIGRATE]");

        long total = this.mongoCollection
                .countDocuments();

        int totalPages = (int) Math.ceil((double) total / this.batchSize);

        for(int page = 0; page < totalPages; page++) {
            int skip = page * this.batchSize;
            List<Genre> genres = this.mongoCollection
                    .find()
                    .skip(skip)
                    .limit(this.batchSize)
                    .into(new ArrayList<>());

            saveGenresInNeo4j(genres);
        }
    }

}
