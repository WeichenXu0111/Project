package org.example.model;

public enum BookStatus {
    PENDING_APPROVAL("Pending Approval"),
    APPROVED_AVAILABLE("Approved - Available"),
    BORROWED("Borrowed"),
    REJECTED("Rejected");

    private final String displayName;

    BookStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

