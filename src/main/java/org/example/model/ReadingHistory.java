package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ReadingHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private final String bookId;
    private final String bookTitle;
    private final String author;
    private final String genre;
    private final LocalDate borrowDate;
    private LocalDate returnDate;
    private LocalDate lastReadDate;
    private int lastPage;
    private int totalPages;

    public ReadingHistory(String username, Book book) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.bookId = book.getId();
        this.bookTitle = book.getTitle();
        this.author = book.getAuthorFullName();
        this.genre = book.getGenre();
        this.borrowDate = book.getBorrowedDate() == null ? LocalDate.now() : book.getBorrowedDate();
        this.lastReadDate = null;
        this.lastPage = 0;
        this.totalPages = 0;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LocalDate getLastReadDate() { return lastReadDate; }
    public int getLastPage() { return lastPage; }
    public int getTotalPages() { return totalPages; }

    public long getReadingDurationDays() {
        LocalDate end = returnDate == null ? LocalDate.now() : returnDate;
        return Math.max(1, ChronoUnit.DAYS.between(borrowDate, end) + 1);
    }

    public int getProgressPercent() {
        if (totalPages <= 0 || lastPage <= 0) return 0;
        return Math.min(100, Math.max(0, (int) Math.round((lastPage * 100.0) / totalPages)));
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date == null ? LocalDate.now() : date;
    }

    public void updateProgress(int page, int totalPages) {
        this.lastReadDate = LocalDate.now();
        this.totalPages = Math.max(this.totalPages, totalPages);
        this.lastPage = Math.max(this.lastPage, page);
    }
}
