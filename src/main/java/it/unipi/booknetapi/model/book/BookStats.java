package it.unipi.booknetapi.model.book;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStats {

    private String id;
    private String title;
    private Long totalScore;

}
