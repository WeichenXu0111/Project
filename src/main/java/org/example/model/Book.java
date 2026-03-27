package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Book implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String title;
    private String authorUsername;
    private String authorFullName;
    private String genre;
    private List<String> genres = new ArrayList<>();
    private String description;
    private String filePath;
    private LocalDate submittedDate;
    private LocalDate approvedDate;
    private LocalDate borrowedDate;
    private LocalDate dueDate;
    private BookStatus status;
    private String borrowedBy;
    private int borrowCount;

    public Book(String title,
                String authorUsername,
                String authorFullName,
                String genre,
                String description,
                String filePath) {
        this(title, authorUsername, authorFullName, genre == null ? List.of() : List.of(genre), description, filePath);
    }

    public Book(String title,
                String authorUsername,
                String authorFullName,
                List<String> genres,
                String description,
                String filePath) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        setGenresInternal(genres);
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
        if (genre != null && !genre.isBlank()) {
            return genre;
        }
        if (genres != null && !genres.isEmpty()) {
            return String.join(" / ", genres);
        }
        return "";
    }

    public List<String> getGenres() {
        if (genres != null && !genres.isEmpty()) {
            return new ArrayList<>(genres);
        }
        if (genre != null && !genre.isBlank()) {
            return List.of(genre);
        }
        return new ArrayList<>();
    }

    public String getGenreDisplay() {
        return getGenre();
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BookStatus getStatus() {
        return status;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public int getBorrowCount() {
        return borrowCount;
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
        dueDate = borrowedDate.plusDays(14);
        borrowCount++;
    }

    public void returnBook() {
        status = BookStatus.APPROVED_AVAILABLE;
        borrowedBy = null;
        borrowedDate = null;
        dueDate = null;
    }

    public void setCustomDueDate(LocalDate customDueDate) {
        this.dueDate = customDueDate;
    }

    private void setGenresInternal(List<String> newGenres) {
        genres = new ArrayList<>();
        if (newGenres != null) {
            for (String item : newGenres) {
                if (item != null && !item.isBlank()) {
                    genres.add(item.trim());
                }
            }
        }
        genre = genres.isEmpty() ? null : String.join(" / ", genres);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGenres(List<String> newGenres) {
        setGenresInternal(newGenres);
    }
}
