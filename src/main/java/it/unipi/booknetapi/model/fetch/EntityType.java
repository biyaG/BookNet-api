package it.unipi.booknetapi.model.fetch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityType {

    AUTHOR("AUTHOR")
    , BOOK("BOOK")
    , BOOK_GENRE("BOOK_GENRE")
    , GENRE("GENRE")
    , IMPORT_LOG("IMPORT_LOG")
    , NOTIFICATION("NOTIFICATION")
    , REVIEW("REVIEW")
    , USER("USER")

    ;

    private final String type;

}
