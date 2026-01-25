package it.unipi.booknetapi.model.user;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ADMIN", 0),
    READER("READER", 1),
    REVIEWER("REVIEWER", 2),

    ;

    private final String name;
    private final int type;

    Role(String name, int type) {
        this.name = name;
        this.type = type;
    }

}
