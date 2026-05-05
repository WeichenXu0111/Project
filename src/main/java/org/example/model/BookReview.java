package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BookReview implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String bookId;
    private final String username;
    private final String reviewerName;
    private int rating;
    private String reviewText;
    private boolean anonymous;
    private LocalDateTime submittedAt;
    private Set<String> helpfulVoters = new HashSet<>();
    private String authorReply = "";
    private LocalDateTime repliedAt;
    private boolean flagged;
    private String flagReason = "";

    public BookReview(String bookId, String username, String reviewerName, int rating, String reviewText, boolean anonymous) {
        this.id = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.username = username;
        this.reviewerName = reviewerName;
        update(rating, reviewText, anonymous);
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getUsername() { return username; }
    public String getReviewerName() { return reviewerName; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public boolean isAnonymous() { return anonymous; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public int getHelpfulCount() { return helpfulVoters == null ? 0 : helpfulVoters.size(); }
    public String getAuthorReply() { return authorReply == null ? "" : authorReply; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public boolean isFlagged() { return flagged; }
    public String getFlagReason() { return flagReason == null ? "" : flagReason; }

    public String getDisplayName() {
        return anonymous ? "Anonymous reader" : reviewerName;
    }

    public void update(int rating, String reviewText, boolean anonymous) {
        this.rating = rating;
        this.reviewText = reviewText == null ? "" : reviewText.trim();
        this.anonymous = anonymous;
        this.submittedAt = LocalDateTime.now();
        if (this.helpfulVoters == null) {
            this.helpfulVoters = new HashSet<>();
        }
    }

    public boolean markHelpful(String username) {
        if (username == null || username.isBlank()) return false;
        if (helpfulVoters == null) helpfulVoters = new HashSet<>();
        return helpfulVoters.add(username);
    }

    public void reply(String replyText) {
        this.authorReply = replyText == null ? "" : replyText.trim();
        this.repliedAt = LocalDateTime.now();
    }

    public void flag(String reason) {
        this.flagged = true;
        this.flagReason = reason == null ? "" : reason.trim();
    }

    public String getFlagDisplay() {
        return flagged ? "Flagged" : "OK";
    }
}
