package it.unipi.booknetapi.model.book;

import lombok.Getter;

@Getter
public enum FormatTypeEnum {

    PAPERBACK( 1),
    HARDCOVER( 2);

    private final int code;

    FormatTypeEnum(int code){
        this.code = code;
    }

    public static boolean isHardCover(int code) {
        return HARDCOVER.code == code;
    }


}
