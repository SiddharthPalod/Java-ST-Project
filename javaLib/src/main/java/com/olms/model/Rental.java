package com.olms.model;

import java.util.Objects;

/**
 * Maps a customer to a rented book.
 */
public class Rental {
    private final int customerId;
    private final int bookId;

    public Rental(int customerId, int bookId) {
        if (customerId < 0 || bookId < 0) {
            throw new IllegalArgumentException("Ids cannot be negative");
        }
        this.customerId = customerId;
        this.bookId = bookId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getBookId() {
        return bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rental rental = (Rental) o;
        return customerId == rental.customerId && bookId == rental.bookId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, bookId);
    }

    @Override
    public String toString() {
        return customerId + "|" + bookId;
    }

    public static Rental fromStorageLine(String line) {
        String[] parts = line.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rental line: " + line);
        }
        int parsedCustomer = Integer.parseInt(parts[0]);
        int parsedBook = Integer.parseInt(parts[1]);
        return new Rental(parsedCustomer, parsedBook);
    }
}

