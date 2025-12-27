package it.unipi.booknetapi.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookShelfGoodReads {

    private String name;

    // Kept as String because your JSON has it as "922" (string),
    // but you can change to Integer if you want Jackson to auto-convert it.
    private String count;

}
