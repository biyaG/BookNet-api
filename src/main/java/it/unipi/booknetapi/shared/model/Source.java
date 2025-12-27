package it.unipi.booknetapi.shared.model;

import lombok.Getter;

@Getter
public enum Source {
    GOOD_READS(1)
    , AMAZON(2)
    , GOOGLE_BOOKS(3)
    , KAGGLE(4)

    ;

    private final int code;

    Source(int code){
        this.code = code;
    }
}
