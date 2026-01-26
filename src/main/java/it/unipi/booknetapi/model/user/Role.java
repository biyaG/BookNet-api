package it.unipi.booknetapi.model.user;

import lombok.Getter;

@Getter
public enum Role {
    Admin("Admin", 0, false),
    Reader("Reader", 1, true),
    Reviewer("Reviewer", 2, true),

    ;

    private final String name;
    private final int type;
    private final boolean addInNeo4j;

    Role(String name, int type, boolean addInNeo4j) {
        this.name = name;
        this.type = type;
        this.addInNeo4j = addInNeo4j;
    }

}
