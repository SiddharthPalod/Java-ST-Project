package com.olms.model;

import java.util.Objects;

/**
 * Represents a system user (customer or admin).
 */
public class User {
    private final int id;
    private final String username;
    private final String password;
    private final boolean admin;

    public User(int id, String username, String password, boolean admin) {
        if (id < 0) {
            throw new IllegalArgumentException("User id cannot be negative");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.id = id;
        this.username = username.trim();
        this.password = password;
        this.admin = admin;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return id == user.id && admin == user.admin
                && Objects.equals(username, user.username)
                && Objects.equals(password, user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, admin);
    }

    @Override
    public String toString() {
        return id + "|" + username + "|" + password + "|" + (admin ? "ADMIN" : "CUSTOMER");
    }

    public static User fromStorageLine(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid user line: " + line);
        }
        int parsedId = Integer.parseInt(parts[0]);
        String parsedUsername = parts[1];
        String parsedPassword = parts[2];
        boolean isAdmin = "ADMIN".equalsIgnoreCase(parts[3]);
        return new User(parsedId, parsedUsername, parsedPassword, isAdmin);
    }
}

