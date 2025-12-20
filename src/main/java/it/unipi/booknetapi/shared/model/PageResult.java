package it.unipi.booknetapi.shared.model;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public PageResult(List<T> content, long totalElements, int currentPage, int pageSize) {
        this.content = content;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        // Calculate total pages safely
        this.totalPages = (pageSize == 0) ? 1 : (int) Math.ceil((double) totalElements / pageSize);
    }

}
