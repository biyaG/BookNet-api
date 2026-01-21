package it.unipi.booknetapi.dto.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderPreferenceRequest {


    private List<String> authors;
    private List<String> genres;
    private List<String> languages; //String or From the dataset

}
