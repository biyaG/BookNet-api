package it.unipi.booknetapi.model.review;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
public class Review {
    @Id
    private long id;
    private float rating;
    private int count;
    private String comment;
    private Date dateAdded;

}
