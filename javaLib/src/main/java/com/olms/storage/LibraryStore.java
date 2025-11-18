package com.olms.storage;

import com.olms.model.Book;
import com.olms.model.Rental;
import com.olms.model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-backed storage layer that keeps state consistent across server restarts.
 */
public class LibraryStore {
    private final boolean persistent;
    private final Path booksFile;
    private final Path usersFile;
    private final Path rentalsFile;

    private final Map<Integer, Book> books = new HashMap<>();
    private final Map<Integer, User> users = new HashMap<>();
    private final List<Rental> rentals = new ArrayList<>();

    private final ReadWriteLock bookLock;
    private final ReadWriteLock userLock;
    private final ReadWriteLock rentalLock;

    public LibraryStore(Path dataDirectory) throws IOException {
        this(true, dataDirectory);
    }

    public static LibraryStore inMemory() throws IOException {
        return new LibraryStore(false, null);
    }

    private LibraryStore(boolean persistent, Path dataDirectory) throws IOException {
        this.persistent = persistent;
        this.bookLock = persistent ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
        this.userLock = persistent ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
        this.rentalLock = persistent ? new ReentrantReadWriteLock() : new NoOpReadWriteLock();
        if (persistent) {
            Files.createDirectories(dataDirectory);
            this.booksFile = ensureFile(dataDirectory.resolve("books.db"));
            this.usersFile = ensureFile(dataDirectory.resolve("users.db"));
            this.rentalsFile = ensureFile(dataDirectory.resolve("rentals.db"));
        } else {
            this.booksFile = null;
            this.usersFile = null;
            this.rentalsFile = null;
        }

        loadBooks();
        loadUsers();
        loadRentals();
        ensureDefaultAdmin();
    }

    private Path ensureFile(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createFile(file);
        }
        return file;
    }

    private void loadBooks() throws IOException {
        if (!persistent) {
            return;
        }
        bookLock.writeLock().lock();
        try {
            books.clear();
            List<String> lines = Files.readAllLines(booksFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Book book = Book.fromStorageLine(line.trim());
                books.put(book.getId(), book);
            }
        } finally {
            bookLock.writeLock().unlock();
        }
    }

    private void loadUsers() throws IOException {
        if (!persistent) {
            return;
        }
        userLock.writeLock().lock();
        try {
            users.clear();
            List<String> lines = Files.readAllLines(usersFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                User user = User.fromStorageLine(line.trim());
                users.put(user.getId(), user);
            }
        } finally {
            userLock.writeLock().unlock();
        }
    }

    private void loadRentals() throws IOException {
        if (!persistent) {
            return;
        }
        rentalLock.writeLock().lock();
        try {
            rentals.clear();
            List<String> lines = Files.readAllLines(rentalsFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                rentals.add(Rental.fromStorageLine(line.trim()));
            }
        } finally {
            rentalLock.writeLock().unlock();
        }
    }

    private void ensureDefaultAdmin() throws IOException {
        userLock.writeLock().lock();
        try {
            boolean hasAdmin = users.values().stream().anyMatch(User::isAdmin);
            if (!hasAdmin) {
                User admin = new User(1, "admin", "admin", true);
                users.put(admin.getId(), admin);
                persistUsers();
            }
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public List<Book> getAllBooks() {
        bookLock.readLock().lock();
        try {
            return new ArrayList<>(
                    books.values().stream()
                            .sorted(Comparator.comparing(Book::getId))
                            .collect(java.util.stream.Collectors.toList()));
        } finally {
            bookLock.readLock().unlock();
        }
    }

    public Optional<Book> findBookByTitle(String title) {
        bookLock.readLock().lock();
        try {
            return books.values().stream()
                    .filter(book -> book.getTitle().equalsIgnoreCase(title.trim()))
                    .findFirst();
        } finally {
            bookLock.readLock().unlock();
        }
    }

    public Optional<Book> findBookById(int id) {
        bookLock.readLock().lock();
        try {
            return Optional.ofNullable(books.get(id));
        } finally {
            bookLock.readLock().unlock();
        }
    }

    public void addBook(Book book) throws IOException {
        bookLock.writeLock().lock();
        try {
            if (books.containsKey(book.getId())) {
                throw new IllegalStateException("Book id already exists");
            }
            books.put(book.getId(), book);
            persistBooks();
        } finally {
            bookLock.writeLock().unlock();
        }
    }

    public boolean deleteBook(int id) throws IOException {
        bookLock.writeLock().lock();
        try {
            Book removed = books.remove(id);
            if (removed != null) {
                persistBooks();
                removeRentalsForBook(id);
                return true;
            }
            return false;
        } finally {
            bookLock.writeLock().unlock();
        }
    }

    public boolean updateQuantity(int id, int quantity) throws IOException {
        bookLock.writeLock().lock();
        try {
            Book existing = books.get(id);
            if (existing == null) {
                return false;
            }
            books.put(id, existing.withQuantity(quantity));
            persistBooks();
            return true;
        } finally {
            bookLock.writeLock().unlock();
        }
    }

    public List<User> getAllCustomers() {
        userLock.readLock().lock();
        try {
            return new ArrayList<>(
                    users.values().stream()
                            .sorted(Comparator.comparing(User::getId))
                            .collect(java.util.stream.Collectors.toList()));
        } finally {
            userLock.readLock().unlock();
        }
    }

    public Optional<User> authenticate(String username, String password, boolean adminLogin) {
        userLock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(user -> user.isAdmin() == adminLogin)
                    .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                    .filter(user -> user.getPassword().equals(password))
                    .findFirst();
        } finally {
            userLock.readLock().unlock();
        }
    }

    public User register(String username, String password, boolean admin) throws IOException {
        userLock.writeLock().lock();
        try {
            boolean exists = users.values().stream()
                    .filter(u -> u.isAdmin() == admin)
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
            if (exists) {
                throw new IllegalStateException("Username already exists");
            }
            int nextId = users.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
            User user = new User(nextId, username, password, admin);
            users.put(user.getId(), user);
            persistUsers();
            return user;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public String rentBook(int bookId, int customerId) throws IOException {
        bookLock.writeLock().lock();
        rentalLock.writeLock().lock();
        try {
            Book book = books.get(bookId);
            if (book == null || book.getQuantity() <= 0) {
                return "Book not available";
            }
            boolean alreadyRented = rentals.stream()
                    .anyMatch(rental -> rental.getBookId() == bookId && rental.getCustomerId() == customerId);
            if (alreadyRented) {
                return "Book already rented by this customer";
            }
            rentals.add(new Rental(customerId, bookId));
            books.put(bookId, book.withQuantity(book.getQuantity() - 1));
            persistRentals();
            persistBooks();
            return "Book rented successfully";
        } finally {
            rentalLock.writeLock().unlock();
            bookLock.writeLock().unlock();
        }
    }

    public String returnBook(int bookId, int customerId) throws IOException {
        bookLock.writeLock().lock();
        rentalLock.writeLock().lock();
        try {
            Optional<Rental> rentalOpt = rentals.stream()
                    .filter(rental -> rental.getBookId() == bookId && rental.getCustomerId() == customerId)
                    .findFirst();
            if (!rentalOpt.isPresent()) {
                return "Book not rented by this customer";
            }
            rentals.remove(rentalOpt.get());
            Book book = books.get(bookId);
            if (book == null) {
                return "Book deleted from catalog";
            }
            books.put(bookId, book.withQuantity(book.getQuantity() + 1));
            persistRentals();
            persistBooks();
            return "Book returned successfully";
        } finally {
            rentalLock.writeLock().unlock();
            bookLock.writeLock().unlock();
        }
    }

    private void removeRentalsForBook(int bookId) throws IOException {
        rentalLock.writeLock().lock();
        try {
            rentals.removeIf(rental -> rental.getBookId() == bookId);
            persistRentals();
        } finally {
            rentalLock.writeLock().unlock();
        }
    }

    private void persistBooks() throws IOException {
        if (!persistent) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                booksFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Book book : books.values().stream()
                    .sorted(Comparator.comparing(Book::getId))
                    .collect(java.util.stream.Collectors.toList())) {
                writer.write(book.toString());
                writer.newLine();
            }
        }
    }

    private void persistUsers() throws IOException {
        if (!persistent) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                usersFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (User user : users.values().stream()
                    .sorted(Comparator.comparing(User::getId))
                    .collect(java.util.stream.Collectors.toList())) {
                writer.write(user.toString());
                writer.newLine();
            }
        }
    }

    private void persistRentals() throws IOException {
        if (!persistent) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                rentalsFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Rental rental : rentals) {
                writer.write(rental.toString());
                writer.newLine();
            }
        }
    }

    private static final class NoOpReadWriteLock implements ReadWriteLock {
        private static final Lock SHARED_LOCK = new NoOpLock();

        @Override
        public Lock readLock() {
            return SHARED_LOCK;
        }

        @Override
        public Lock writeLock() {
            return SHARED_LOCK;
        }
    }

    private static final class NoOpLock implements Lock {
        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock() {
        }

        @Override
        public Condition newCondition() {
            return NoOpCondition.INSTANCE;
        }
    }

    private static final class NoOpCondition implements Condition {
        private static final NoOpCondition INSTANCE = new NoOpCondition();

        @Override
        public void await() {
        }

        @Override
        public void awaitUninterruptibly() {
        }

        @Override
        public long awaitNanos(long nanosTimeout) {
            return nanosTimeout;
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public boolean awaitUntil(java.util.Date deadline) throws InterruptedException {
            return true;
        }

        @Override
        public void signal() {
        }

        @Override
        public void signalAll() {
        }
    }
}

