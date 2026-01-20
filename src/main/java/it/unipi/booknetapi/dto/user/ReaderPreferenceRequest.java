package it.unipi.booknetapi.dto.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderPreferenceRequest {


    private List<String> authorIds;
    private List<String> genreIds;
//    private List<String> languageIds;

}
