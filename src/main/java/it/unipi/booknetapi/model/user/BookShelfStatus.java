package it.unipi.booknetapi.model.user;

public enum BookShelfStatus {
    ADDED, READING, FINISHED;



    public static final BookShelfStatus DEFAULT = ADDED;

    public static BookShelfStatus nextStatus(BookShelfStatus currentStatus) {
        if(currentStatus == null) return DEFAULT;

        return switch (currentStatus) {
            case ADDED -> READING;
            case READING, FINISHED -> FINISHED;
            default -> DEFAULT;
        };
    }
}
