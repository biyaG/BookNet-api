package it.unipi.booknetapi.repository.fetch;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ImportLogRepository implements ImportLogRepositoryInterface {

    Logger logger = LoggerFactory.getLogger(ImportLogRepository.class);

    private final MongoCollection<ImportLog> mongoCollection;

    public ImportLogRepository(MongoDatabase mongoDatabase) {
        this.mongoCollection = mongoDatabase.getCollection("import_logs", ImportLog.class);
    }

    /**
     * @param importLog
     * @return
     */
    @Override
    public ImportLog insert(ImportLog importLog) {
        Objects.requireNonNull(importLog);

        logger.debug("Inserting import log: {}", importLog);

        InsertOneResult insertOneResult = this.mongoCollection.insertOne(importLog);

        if(insertOneResult.wasAcknowledged()) return importLog;

        return null;
    }

    /**
     * @param idImportLog
     * @return
     */
    @Override
    public boolean delete(String idImportLog) {
        Objects.requireNonNull(idImportLog);

        DeleteResult deleteResult = this.mongoCollection.deleteOne(Filters.eq("_id", new ObjectId(idImportLog)));

        return deleteResult.getDeletedCount() > 0;
    }

    /**
     * @param idImportLogs
     * @return
     */
    @Override
    public boolean deleteAll(List<String> idImportLogs) {
        Objects.requireNonNull(idImportLogs);

        DeleteResult deleteResult = this.mongoCollection
                .deleteMany(
                        Filters.in("_id", idImportLogs.stream().map(ObjectId::new).toList())
                );

        return deleteResult.getDeletedCount() > 0;
    }

    /**
     * @param idImportLog
     * @return
     */
    @Override
    public Optional<ImportLog> findById(String idImportLog) {
        Objects.requireNonNull(idImportLog);

        ImportLog importLog = this.mongoCollection
                .find(Filters.eq("_id", new ObjectId(idImportLog)))
                .first();

        if(importLog != null) return Optional.of(importLog);

        return Optional.empty();
    }

    /**
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageResult<ImportLog> findAll(int page, int size) {
        int skip = page * size;

        List<ImportLog> importLogs = this.mongoCollection
                .find()
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments();

        return new PageResult<>(importLogs, total, page, size);
    }

    /**
     * @param entityType
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageResult<ImportLog> findAll(EntityType entityType, int page, int size) {
        Objects.requireNonNull(entityType);

        int skip = page * size;

        List<ImportLog> importLogs = this.mongoCollection
                .find(Filters.eq("entityType", entityType))
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(Filters.eq("entityType", entityType));


        return new PageResult<>(importLogs, total, page, size);
    }

    /**
     * @param success
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageResult<ImportLog> findAll(Boolean success, int page, int size) {
        Objects.requireNonNull(success);

        int skip = page * size;

        List<ImportLog> importLogs = this.mongoCollection
                .find(Filters.eq("success", success))
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(Filters.eq("success", success));

        return new PageResult<>(importLogs, total, page, size);
    }

    /**
     * @param entityType
     * @param success
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageResult<ImportLog> findAll(EntityType entityType, Boolean success, int page, int size) {
        Objects.requireNonNull(entityType);
        Objects.requireNonNull(success);

        int skip = page * size;

        List<ImportLog> importLogs = this.mongoCollection
                .find(Filters.and(
                        Filters.eq("entityType", entityType),
                        Filters.eq("success", success)
                ))
                .skip(skip)
                .limit(size)
                .into(new ArrayList<>());

        long total = this.mongoCollection
                .countDocuments(Filters.and(
                        Filters.eq("entityType", entityType),
                        Filters.eq("success", success)
                ));

        return new PageResult<>(importLogs, total, page, size);
    }
}
