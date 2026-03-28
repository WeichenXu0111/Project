package org.example.model;

import java.time.LocalDate;

public class BorrowedBook {
    private String bookId;
    private String username;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // Null if not returned

    public BorrowedBook(String bookId, String username) {
        this.bookId = bookId;
        this.username = username;
        this.borrowDate = LocalDate.now();
        this.dueDate = this.borrowDate.plusDays(30); // Example: 30-day borrowing period
    }

    // Getters and setters
    public String getBookId() {
        return bookId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isReturned() {
        return returnDate != null;
    }
}

