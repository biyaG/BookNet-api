package it.unipi.booknetapi.repository.user;

import it.unipi.booknetapi.model.review.Review;
import it.unipi.booknetapi.model.user.Reader;

public interface ReaderRepositoryInterface {

    boolean addReview(String idReader, Review review);
    boolean deleteReview(String idReader, String idReview);
    boolean addBookInShelf(String idReader, String idBook);

}
