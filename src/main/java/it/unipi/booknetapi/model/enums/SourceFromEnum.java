package it.unipi.booknetapi.model.enums;

public enum SourceFromEnum {

    AMAZON( 1),
    GOODREADS( 2),
    GOOGLEBOOKS(3);

    int code;

    public int getCode() {
        return code;
    }

    SourceFromEnum(int code){
        this.code = code;
    }

}
