package it.unipi.booknetapi.dto.book;

import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.FormatTypeEnum;
import it.unipi.booknetapi.model.book.SourceFromEnum;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.shared.model.ExternalId;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateRequest {
    @NotBlank(message = "Title is mandatory")
    private String title;
    private String subtitle;
    private String description;
    private String isbn;
    private String isbn13;
    private Integer num_pages;
    private Date publication_date;

    @NotBlank(message = "message = \"At least one language must be specified\"")

    private List<String> language;
    private List<String> images;
    private List<String> preview;
    private List<String> publishers;

    // Direct embedding from the request
    private List<AuthorEmbed> authors;
    private List<GenreEmbed> genres;

    private FormatTypeEnum formats;
    private SourceFromEnum source;
    private ExternalId externalId;
}
