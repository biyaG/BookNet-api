package it.unipi.booknetapi.dto.user;

import it.unipi.booknetapi.model.user.BookShelfStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderBookShelfUpdateStatusRequest {

    private BookShelfStatus status;

}
