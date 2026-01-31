package it.unipi.booknetapi.service.user;

import it.unipi.booknetapi.command.stat.UserMonthlyStatGetCommand;
import it.unipi.booknetapi.command.stat.UserMonthlyStatListCommand;
import it.unipi.booknetapi.command.stat.UserYearlyStatGetCommand;
import it.unipi.booknetapi.command.user.*;
import it.unipi.booknetapi.dto.book.BookEmbedResponse;
import it.unipi.booknetapi.dto.stat.UserMonthlyStatResponse;
import it.unipi.booknetapi.dto.stat.UserYearlyStatResponse;
import it.unipi.booknetapi.dto.user.*;
import it.unipi.booknetapi.model.author.Author;
import it.unipi.booknetapi.model.author.AuthorEmbed;
import it.unipi.booknetapi.model.book.Book;
import it.unipi.booknetapi.model.genre.Genre;
import it.unipi.booknetapi.model.genre.GenreEmbed;
import it.unipi.booknetapi.model.stat.ReadEvent;
import it.unipi.booknetapi.model.stat.UserMonthlyStat;
import it.unipi.booknetapi.model.stat.UserYearlyStat;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.repository.author.AuthorRepository;
import it.unipi.booknetapi.repository.book.BookRepository;
import it.unipi.booknetapi.repository.genre.GenreRepository;
import it.unipi.booknetapi.repository.stat.UserMonthlyStatRepository;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final UserRepository userRepository;


    public UserService(
            AuthorRepository authorRepository,
            BookRepository bookRepository,
            GenreRepository genreRepository,
            UserMonthlyStatRepository userMonthlyStatRepository,
            UserRepository userRepository
    ) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.userMonthlyStatRepository = userMonthlyStatRepository;
        this.userRepository = userRepository;
    }


    public ReaderComplexResponse getReaderById(String idUser) {
        User user =  userRepository.findById(idUser)
                .orElse(null);

        if(user == null || user.getRole() == null || user.getRole() != Role.Reader) return null;

        return new ReaderComplexResponse(user);
    }


    public UserResponse get(UserGetCommand command) {
        User user =  userRepository.findById(command.getId()).orElse(null);

        if(user == null) {
            return null;
        }

        return switch (user.getRole()) {
            case Admin -> new AdminResponse(user);
            case Reader -> new ReaderResponse(user);
            case Reviewer -> new ReviewerResponse(user);
            default -> new UserResponse(user);
        };
    }

    public UserResponse update(UserUpdateCommand command) {
        if(command.getUserToken() == null || command.getUserToken().getIdUser() == null) return null;

        if(command.getName() == null) return null;

        boolean updated = this.userRepository.updateName(command.getUserToken().getIdUser(), command.getName());
        if(!updated) return null;

        return this.get(UserGetCommand.builder().id(command.getUserToken().getIdUser()).userToken(command.getUserToken()).build());
    }

    public ReaderResponse update(ReaderUpdatePreferenceCommand command) {
        if(command.getUserToken() == null || command.getUserToken().getIdUser() == null) return null;

        List<Author> authors = this.authorRepository.findAllById(command.getAuthors());
        List<Genre> genres = this.genreRepository.find(command.getGenres());
        List<String> languages = command.getLanguages() != null ? command.getLanguages() : List.of();

        ReaderPreference readerPreference = ReaderPreference.builder()
                .authors(authors.stream().map(AuthorEmbed::new).toList())
                .genres(genres.stream().map(GenreEmbed::new).toList())
                .languages(languages)
                .build();

        boolean updated = this.userRepository.updatePreference(command.getUserToken().getIdUser(), readerPreference);
        if(!updated) return null;

        User user =  this.userRepository.findById(command.getUserToken().getIdUser())
                .orElse(null);

        return new ReaderResponse(user);
    }


    public PageResult<AdminResponse> list(AdminListCommand command) {
        PageResult<Admin> result = this.userRepository.findAllAdmin(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(AdminResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReaderResponse> list(ReaderListCommand command) {
        PageResult<Reader> result = this.userRepository.findAllReader(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReaderResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReviewerResponse> list(ReviewerListCommand command) {
        PageResult<Reviewer> result = this.userRepository.findAllReviewer(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewerResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public void migrate() {
        migrateReaders();
        migrateReviewers();
    }

    public void migrateReaders() {
        Thread thread = new Thread(this.userRepository::migrateReaders);
        thread.start();
    }

    public void migrateReviewers() {
        Thread thread = new Thread(this.userRepository::migrateReviewers);
        thread.start();
    }



    // stats

    private boolean isValidYear(Integer year) {
        return year != null && year >= 2010;
    }

    private boolean isValidMonth(Integer month) {
        return month != null && month >= 1 && month <= 12;
    }


    public UserMonthlyStatResponse getStat(UserMonthlyStatGetCommand command) {
        if(command.getIdUser() == null) return null;

        LocalDate now = LocalDate.now();
        int year = isValidYear(command.getYear()) ? command.getYear() : now.getYear();
        int month = isValidMonth(command.getMonth()) ? command.getMonth() : now.getMonthValue();

        UserMonthlyStat stat = this.userMonthlyStatRepository.getMonthlyStats(command.getIdUser(), year, month);

        if(stat != null) {
            UserMonthlyStatResponse response = new UserMonthlyStatResponse(stat);
            if(stat.getReadingLog() != null) {
                List<ObjectId> idBooks = stat.getReadingLog().stream()
                        .map(ReadEvent::getBookId)
                        .toList();

                if(!idBooks.isEmpty()) {
                    List<Book> books = this.bookRepository.find(idBooks);
                    Map<String, BookEmbedResponse> bookEmbedMap = books.stream()
                            .collect(Collectors.toMap(
                                    b -> b.getId().toHexString(),
                                    BookEmbedResponse::new
                            ));

                    response.getReadingLog().forEach(event -> {
                        event.setBook(bookEmbedMap.get(event.getIdBook()));
                    });
                }
            }
            return response;
        }

        UserMonthlyStatResponse response = new UserMonthlyStatResponse();
        response.setYear(year);
        response.setMonth(month);
        return response;
    }


    public List<UserMonthlyStatResponse> getStats(UserMonthlyStatListCommand command) {
        if(command.getIdUser() == null) return null;

        LocalDate now = LocalDate.now();
        int year = isValidYear(command.getYear()) ? command.getYear() : now.getYear();

        List<UserMonthlyStat> stats = this.userMonthlyStatRepository.getMonthlyStats(command.getIdUser(), year);

        if(stats == null || stats.isEmpty()) return List.of();

        List<ObjectId> idBooks = stats.stream()
                .map(UserMonthlyStat::getReadingLog)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(ReadEvent::getBookId)
                .toList();

        List<Book> books = this.bookRepository.find(idBooks);
        Map<String, BookEmbedResponse> bookEmbedMap = books.stream()
                .collect(Collectors.toMap(
                        b -> b.getId().toHexString(),
                        BookEmbedResponse::new
                ));

        List<UserMonthlyStatResponse> response = new ArrayList<>(stats.size());
        for(UserMonthlyStat stat : stats) {
            UserMonthlyStatResponse statResponse = new UserMonthlyStatResponse(stat);
            if(statResponse.getReadingLog() != null) {
                statResponse.getReadingLog().forEach(event -> {
                    event.setBook(bookEmbedMap.get(event.getIdBook()));
                });
            }
        }

        return response;
    }


    public UserYearlyStatResponse getYearlyStat(UserYearlyStatGetCommand command) {
        if(command.getIdUser() == null) return null;

        LocalDate now = LocalDate.now();
        int year = isValidYear(command.getYear()) ? command.getYear() : now.getYear();

        UserYearlyStat userYearlyStat = this.userMonthlyStatRepository.getYearlyStats(command.getIdUser(), year);
        if(userYearlyStat != null) return new UserYearlyStatResponse(userYearlyStat);

        UserYearlyStatResponse response = new UserYearlyStatResponse();
        response.setUserId(command.getIdUser());
        return response;
    }

}
