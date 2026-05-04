package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String id;
    private final String requesterUsername;
    private final String requesterName;
    private final String title;
    private final String author;
    private final String genre;
    private final String reason;
    private final LocalDateTime submittedAt;
    private Status status;
    private String librarianNote;
    private LocalDateTime processedAt;
    private boolean priority;

    public BookRequest(String requesterUsername, String requesterName, String title, String author, String genre, String reason) {
        this.id = UUID.randomUUID().toString();
        this.requesterUsername = requesterUsername;
        this.requesterName = requesterName;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.reason = reason;
        this.submittedAt = LocalDateTime.now();
        this.status = Status.PENDING;
        this.librarianNote = "";
        this.priority = false;
    }

    public String getId() { return id; }
    public String getRequesterUsername() { return requesterUsername; }
    public String getRequesterName() { return requesterName; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public String getReason() { return reason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Status getStatus() { return status; }
    public String getLibrarianNote() { return librarianNote; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public boolean isPriority() { return priority; }

    public String getPriorityDisplay() {
        return priority ? "Urgent" : "Normal";
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public void approve(String note) {
        this.status = Status.APPROVED;
        this.librarianNote = note == null ? "" : note.trim();
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String note) {
        this.status = Status.REJECTED;
        this.librarianNote = note == null ? "" : note.trim();
        this.processedAt = LocalDateTime.now();
    }
}
