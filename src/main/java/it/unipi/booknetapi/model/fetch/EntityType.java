package it.unipi.booknetapi.model.fetch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntityType {

    BOOK("BOOK")
    , AUTHOR("AUTHOR")
    , GENRE("GENRE")
    , REVIEW("REVIEW")
    , USER("USER")

    ;

    private final String type;

}
