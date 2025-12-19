package it.unipi.booknetapi.model.book;

import lombok.Getter;

@Getter
public enum SourceFromEnum {

    AMAZON( 1),
    GOODREADS( 2),
    GOOGLEBOOKS(3);

    private final int code;

    SourceFromEnum(int code){
        this.code = code;
    }

}
