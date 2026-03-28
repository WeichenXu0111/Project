package org.example.model;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private String fullName;
    private final Role role;
    private String passwordHash;
    private String passwordSalt;
    private String bio;
    private String employeeId;
    private String avatarPath;
    private boolean active = true;

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
        this.avatarPath = "";
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

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
