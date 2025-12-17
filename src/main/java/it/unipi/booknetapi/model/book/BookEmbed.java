package it.unipi.booknetapi.model.book;

import lombok.Data;

import java.util.List;

@Data
public class BookEmbed {

    private String id;
    private String title;
    private String description;
    private List<String> images;

}
