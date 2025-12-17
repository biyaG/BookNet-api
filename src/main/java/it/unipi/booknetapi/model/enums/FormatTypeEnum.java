package it.unipi.booknetapi.model.enums;

import lombok.Getter;

public enum FormatTypeEnum {

    PAPERBACK( 1),
    HARDCOVER( 2);

    int code;

    public int getCode() {
        return code;
    }

    FormatTypeEnum(int code){
        this.code = code;
    }

    public static boolean isHardCover(int code) {
        return HARDCOVER.code == code;
    }


}
