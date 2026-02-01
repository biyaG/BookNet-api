package it.unipi.booknetapi.dto.book;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleBookCsvRecord {

    private String title;
    private String description;
    private List<String> authors;
    private String image;
    private String previewLink;
    private String publisher;
    private String publishedDate;    // keep raw (e.g., "1996", "2005-01-01", "2005-02")
    private String infoLink;
    private List<String> categories;
    private double ratingsCount;

}
