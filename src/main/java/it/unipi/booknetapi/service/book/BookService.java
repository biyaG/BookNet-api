package it.unipi.booknetapi.service.book;

import it.unipi.booknetapi.repository.book.BookRepository;

public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

}
