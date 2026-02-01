package it.unipi.booknetapi.service.fetch;

import it.unipi.booknetapi.dto.book.BookCsvRecord;
import it.unipi.booknetapi.dto.book.GoogleBookCsvRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BookCsvReader {

    // Publication date format in your CSV: e.g. 9/16/2006, 11/1/2003
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    private static final CSVFormat GOODREADS_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setQuote(null)
            .get();;


    public static List<BookCsvRecord> readBooks(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return readBooks(reader);
        }
    }

    public static List<BookCsvRecord> readBooks(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return readBooks(reader);
        }
    }

    public static List<BookCsvRecord> readBooks(Reader reader) throws IOException {
        List<BookCsvRecord> result = new ArrayList<>();

        try (CSVParser parser = GOODREADS_FORMAT.parse(reader)) {

            for (CSVRecord record : parser) {
                BookCsvRecord book = new BookCsvRecord();

                book.setBookId(parseInt(record.get("bookID")));
                book.setTitle(record.get("title"));
                book.setAuthors(getAuthorNames(record.get("authors")));
                book.setAverageRating(parseDouble(record.get("average_rating")));
                book.setIsbn(record.get("isbn"));
                book.setIsbn13(record.get("isbn13"));
                book.setLanguageCode(record.get("language_code").trim());
                book.setNumPages(parseInt(record.get("num_pages").trim())); // if header has extra spaces, trim helps
                book.setRatingsCount(parseLong(record.get("ratings_count").trim()));
                book.setTextReviewsCount(parseLong(record.get("text_reviews_count").trim()));
                book.setPublicationDate(parseDate(record.get("publication_date")));
                book.setPublisher(record.get("publisher"));

                result.add(book);
            }
        }

        return result;
    }

    private static List<String> getAuthorNames(String authors) {
        if(authors == null) return Collections.emptyList();
        return Arrays.stream(authors.split("/"))
                .map(String::trim)
                .toList();
    }

    private static final CSVFormat GOOGLE_BOOKS_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)   // first row is header line
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .get();


    public static List<GoogleBookCsvRecord> readGoogleBooks(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return readGoogleBooks(reader);
        }
    }

    public static List<GoogleBookCsvRecord> readGoogleBooks(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return readGoogleBooks(reader);
        }
    }

    public static List<GoogleBookCsvRecord> readGoogleBooks(Reader reader) throws IOException {
        List<GoogleBookCsvRecord> result = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(reader, GOOGLE_BOOKS_FORMAT)) {

            for (CSVRecord record : parser) {
                GoogleBookCsvRecord book = new GoogleBookCsvRecord();

                book.setTitle(record.get("Title"));
                book.setDescription(record.get("description"));
                book.setAuthors(parseListField(record.get("authors")));
                book.setImage(record.get("image"));
                book.setPreviewLink(record.get("previewLink"));
                book.setPublisher(record.get("publisher"));
                book.setPublishedDate(record.get("publishedDate")); // keep raw
                book.setInfoLink(record.get("infoLink"));
                book.setCategories(parseListField(record.get("categories")));
                book.setRatingsCount(parseDouble(record.get("ratingsCount")));

                result.add(book);
            }
        }

        return result;
    }


    private static int parseInt(String s) {
        return (s == null || s.isBlank()) ? 0 : Integer.parseInt(s.trim());
    }

    private static long parseLong(String s) {
        return (s == null || s.isBlank()) ? 0L : Long.parseLong(s.trim());
    }

    private static double parseDouble(String s) {
        return (s == null || s.isBlank()) ? 0.0 : Double.parseDouble(s.trim());
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDate.parse(s.trim(), DATE_FORMATTER);
    }

    /**
     * Parse fields like "['Julie Strain']" or "['Fiction','Romance']"
     * into a List<String>.
     */
    private static List<String> parseListField(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String s = raw.trim();
        if (s.isEmpty() || "[]".equals(s)) {
            return Collections.emptyList();
        }

        // Remove leading/trailing [ ]
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        if (s.isEmpty()) {
            return Collections.emptyList();
        }

        // Now s should look like:  'Fiction','Romance'   or   'Kotoyama,'
        // Split on "','", not on plain comma
        String[] parts = s.split("','");

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim();

            // Remove leading/trailing single quotes if present
            if (item.startsWith("'")) {
                item = item.substring(1);
            }
            if (item.endsWith("'")) {
                item = item.substring(0, item.length() - 1);
            }

            // Also handle double quotes if they show up (just in case)
            if (item.startsWith("\"")) {
                item = item.substring(1);
            }
            if (item.endsWith("\"")) {
                item = item.substring(0, item.length() - 1);
            }

            item = item.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }

        return result;
    }


    private static void testBookCsv() throws IOException {
        Path path = Path.of("/Users/adrienkt/DataspellProjects/BookCompass/dataset/books.csv"); // change path if needed
        List<BookCsvRecord> books = readBooks(path);

        books.forEach(System.out::println);
    }

    private static void testGoogleBookCsv() throws IOException {
        Path path = Path.of("/Users/adrienkt/DataspellProjects/BookCompass/dataset/books_data.csv"); // adjust file name/path
        List<GoogleBookCsvRecord> books = readGoogleBooks(path);
        books.forEach(System.out::println);
    }

    // Simple main to test
    public static void main(String[] args) throws IOException {
        // testBookCsv();
        testGoogleBookCsv();
    }
}

