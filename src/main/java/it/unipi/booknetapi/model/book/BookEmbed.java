package it.unipi.booknetapi.model.book;

import lombok.Data;
import org.bson.types.ObjectId;

import java.util.List;

@Data
public class BookEmbed {

    private ObjectId _id;
    private String title;
    private String description;
    private List<String> images;

}
