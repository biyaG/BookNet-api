package it.unipi.booknetapi.model.fetch;

import it.unipi.booknetapi.shared.model.Source;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportLog {

    @Id
    @BsonId
    private ObjectId id;

    private Date operationDate;

    private Source source;

    private EntityType entityType;

    private Long numberOfEntities;

    private Long numberOfImportedEntities;

    private List<ObjectId> ids;

    private Boolean success;

    private String message;


    private String fileName;
    private String fileType;
    private String fileUrl;


}
