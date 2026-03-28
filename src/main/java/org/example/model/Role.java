package org.example.model;

public enum Role {
    STUDENT("Student"),
    STAFF("Staff"),
    AUTHOR("Author"),
    LIBRARIAN("Librarian");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
