package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.author.Author;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookCsvRecordWithAuthor extends BookCsvRecord {

    List<Author> authorList;

    public BookCsvRecordWithAuthor(BookCsvRecord bookCsvRecord, List<Author> authorList) {
        super(bookCsvRecord);
        this.authorList = authorList;
    }

}
