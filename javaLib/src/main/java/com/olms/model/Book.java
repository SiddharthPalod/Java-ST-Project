package com.olms.model;

import java.util.Objects;

/**
 * Represents a book in the library inventory.
 */
public class Book {
    private final int id;
    private final String title;
    private final int quantity;

    public Book(int id, String title, int quantity) {
        if (id < 0) {
            throw new IllegalArgumentException("Book id cannot be negative");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.id = id;
        this.title = title.trim();
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getQuantity() {
        return quantity;
    }

    public Book withQuantity(int newQuantity) {
        return new Book(id, title, newQuantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Book book = (Book) o;
        return id == book.id && quantity == book.quantity && Objects.equals(title, book.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, quantity);
    }

    @Override
    public String toString() {
        return id + "|" + title + "|" + quantity;
    }

    public static Book fromStorageLine(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid book line: " + line);
        }
        int parsedId = Integer.parseInt(parts[0]);
        String parsedTitle = parts[1];
        int parsedQty = Integer.parseInt(parts[2]);
        return new Book(parsedId, parsedTitle, parsedQty);
    }
}

