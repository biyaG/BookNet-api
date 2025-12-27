package it.unipi.booknetapi.service.fetch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.booknetapi.dto.book.BookGoodReads;
import it.unipi.booknetapi.model.fetch.EntityType;
import it.unipi.booknetapi.model.fetch.ImportLog;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.shared.model.Source;
import it.unipi.booknetapi.repository.fetch.ImportLogRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class ImportService {

    private final ImportLogRepository importLogRepository;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;

    public ImportService(ImportLogRepository importLogRepository, BookRepository bookRepository) {
        this.importLogRepository = importLogRepository;
        this.bookRepository = bookRepository;
        this.objectMapper = new ObjectMapper();
    }


    public String importData(Source source, ImportEntityType importEntityType, MultipartFile file) {
        if(file.isEmpty()) return "File is empty";

        String fileUrl;
        try {
            fileUrl = file.getResource().getURI().toString();
        } catch (Exception e) {
            fileUrl = file.getOriginalFilename();
        }

        try {
            String message;
            switch (importEntityType) {
                case GOOD_READS_BOOK: {
                    List<BookGoodReads> books = objectMapper.readValue(
                            file.getInputStream(),
                            new TypeReference<List<BookGoodReads>>(){}
                    );

                    List<ObjectId> ids = importGoodReadsBooks(books);
                    message =  "Successfully processed " + books.size() + " books";

                    ImportLog importLog = ImportLog.builder()
                            .operationDate(new Date())
                            .source(source)
                            .entityType(EntityType.BOOK)
                            .numberOfEntities((long) books.size())
                            .numberOfImportedEntities((long) ids.size())
                            .ids(ids)
                            .success(true)
                            .message(message)
                            .fileName(file.getOriginalFilename())
                            .fileType(file.getContentType())
                            .fileUrl(fileUrl)
                            .build();
                    importLogRepository.insert(importLog);
                }
                default: {
                    message = "Unknown entity type";
                }
            }


            return message;
        } catch (IOException e) {
            String message = "Error during import: " + e.getMessage();
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

    private List<ObjectId> importGoodReadsBooks(List<BookGoodReads> books) {

        return List.of();
    }

}
