package it.unipi.booknetapi.command.book;

import it.unipi.booknetapi.dto.book.BookCreateRequest;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.FormatTypeEnum;
import it.unipi.booknetapi.model.book.SourceFromEnum;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.shared.command.BaseCommand;
import it.unipi.booknetapi.shared.model.ExternalId;
import lombok.*;

import java.util.List;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class BookCreateCommand extends BaseCommand {

    private String title;
    private String description;
    private List<String> images;
    private List<String> language;
    private List<String> preview;
    private List<String> publishers;

    private List<AuthorEmbed> authors;
    private List<GenreEmbed> genres;

    private String isbn;
    private String isbn13;
    private String subtitle;
    private Integer num_pages;
    private Date publication_date;
    private FormatTypeEnum formats;
    private SourceFromEnum source;
    private ExternalId externalId;

    public BookCreateCommand(BookCreateRequest request){
        this.title = request.getTitle();
        this.description = request.getDescription();
        this.images = request.getImages();
        this.language = request.getLanguage();
        this.preview = request.getPreview();
        this.publishers = request.getPublishers();

        this.isbn = request.getIsbn();
        this.isbn13 = request.getIsbn13();
        this.subtitle = request.getSubtitle();
        this.num_pages = request.getNum_pages();
        this.publication_date = request.getPublication_date();
        this.formats = request.getFormats();
        this.source = request.getSource();
        this.externalId = request.getExternalId();

        this.genres = request.getGenres();
        this.authors = request.getAuthors();
    }

}
