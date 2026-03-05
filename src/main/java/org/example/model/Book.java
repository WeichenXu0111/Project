package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class Book implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String title;
    private String authorUsername;
    private String authorFullName;
    private String genre;
    private String description;
    private String filePath;
    private LocalDate submittedDate;
    private LocalDate approvedDate;
    private LocalDate borrowedDate;
    private BookStatus status;
    private String borrowedBy;

    public Book(String title,
                String authorUsername,
                String authorFullName,
                String genre,
                String description,
                String filePath) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genre = genre;
        this.description = description;
        this.filePath = filePath;
        this.submittedDate = LocalDate.now();
        this.status = BookStatus.PENDING_APPROVAL;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public String getGenre() {
        return genre;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public LocalDate getBorrowedDate() {
        return borrowedDate;
    }

    public BookStatus getStatus() {
        return status;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public void approve() {
        approve(LocalDate.now());
    }

    public void approve(LocalDate date) {
        status = BookStatus.APPROVED_AVAILABLE;
        approvedDate = date;
    }

    public void reject() {
        status = BookStatus.REJECTED;
    }

    public boolean isAvailable() {
        return status == BookStatus.APPROVED_AVAILABLE;
    }

    public void borrow(String username) {
        status = BookStatus.BORROWED;
        borrowedBy = username;
        borrowedDate = LocalDate.now();
    }
}
