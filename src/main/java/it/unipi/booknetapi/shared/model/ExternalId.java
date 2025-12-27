package it.unipi.booknetapi.shared.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalId {

    private String goodReads;
    private String amazon;
    private String googleBooks;
    private String kaggle;

}
