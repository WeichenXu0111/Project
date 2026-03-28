package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String message;
    private final LocalDateTime timestamp;
    private final String recipientUsername;
    private String category;
    private String urgency;
    private boolean isRead;
    private boolean archived;

    public Notification(String message, String recipientUsername) {
        this(message, recipientUsername, "General", "Normal");
    }

    public Notification(String message, String recipientUsername, String category, String urgency) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.recipientUsername = recipientUsername;
        this.category = category == null || category.isBlank() ? "General" : category;
        this.urgency = urgency == null || urgency.isBlank() ? "Normal" : urgency;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.archived = false;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getCategory() {
        return category;
    }

    public String getUrgency() {
        return urgency;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    @Override
    public String toString() {
        return String.format("[%s][%s] %s", timestamp, category, message);
    }
}

