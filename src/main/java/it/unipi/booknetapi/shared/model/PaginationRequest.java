package it.unipi.booknetapi.shared.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationRequest {

    private int page;
    private int size;
    private String sort;
    private String search;

}
