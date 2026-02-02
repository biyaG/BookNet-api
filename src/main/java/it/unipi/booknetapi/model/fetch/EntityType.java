package it.unipi.booknetapi.model.fetch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityType {

    AUTHOR("AUTHOR")
    , BOOK("BOOK")
    , BOOK_AUTHOR("BOOK_AUTHOR")
    , BOOK_GENRE("BOOK_GENRE")
    , GENRE("GENRE")
    , IMPORT_LOG("IMPORT_LOG")
    , NOTIFICATION("NOTIFICATION")
    , REVIEW("REVIEW")
    , USER("USER")
    , SHELF("SHELF")

    ;

    private final String type;

}
