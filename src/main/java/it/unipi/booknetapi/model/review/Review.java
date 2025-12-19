package it.unipi.booknetapi.model.review;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Getter
@Setter
public class Review {
    @Id
    private long id;
    private float rating;
    private int count;
    private String comment;
    private Date dateAdded;

}
