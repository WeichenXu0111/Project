package org.example.model;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String fullName;
    private final Role role;
    private final String passwordHash;
    private final String passwordSalt;
    private final String bio;
    private final String employeeId;

    public User(String username,
                String fullName,
                Role role,
                String passwordHash,
                String passwordSalt,
                String bio,
                String employeeId) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.bio = bio;
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getBio() {
        return bio;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}

