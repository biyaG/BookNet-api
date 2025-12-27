package it.unipi.booknetapi.repository.fetch;

import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface ImportLogRepositoryInterface {

    ImportLog insert(ImportLog importLog);

    boolean delete(String idImportLog);
    boolean deleteAll(List<String> idImportLogs);

    Optional<ImportLog> findById(String idImportLog);

    PageResult<ImportLog> findAll(int page, int size);
    PageResult<ImportLog> findAll(EntityType entityType, int page, int size);
    PageResult<ImportLog> findAll(Boolean success, int page, int size);
    PageResult<ImportLog> findAll(EntityType entityType, Boolean success, int page, int size);

}
