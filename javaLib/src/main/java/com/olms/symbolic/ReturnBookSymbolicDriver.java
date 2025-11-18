package com.olms.symbolic;

import com.olms.model.Book;
import com.olms.model.User;
import com.olms.storage.LibraryStore;
import gov.nasa.jpf.symbc.Debug;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Symbolic driver that exercises {@link LibraryStore#returnBook(int, int)} from two concurrent threads.
 * <p>
 * The goal is to keep the API-level calls concrete (LibraryStore behaves exactly like in production)
 * while exploring symbolic combinations of customer identities, book identifiers, and catalog state.
 */
public class ReturnBookSymbolicDriver {

    static {
        ensureSystemProperty("os.name", "Linux");
        ensureSystemProperty("java.io.tmpdir", ".");
    }

    private static final int PRIMARY_BOOK_ID = 9001;
    private static final int UNUSED_BOOK_ID = 9015;

    private final LibraryStore store;
    private final int validCustomerId;
    private final int otherCustomerId;
    private int syntheticUserCounter = 0;

    public ReturnBookSymbolicDriver() throws IOException {
        this.store = LibraryStore.inMemory();
        initializeCatalog();
        this.validCustomerId = registerCustomer("returner-primary");
        this.otherCustomerId = registerCustomer("returner-secondary");
        store.rentBook(PRIMARY_BOOK_ID, validCustomerId);
    }

    public static void main(String[] args) throws IOException {
        new ReturnBookSymbolicDriver().runSymbolicReturnScenario();
    }

    /**
     * Entry-point for SPF. Both thread behaviours and the catalog tampering flag are symbolic.
     */
    public void runSymbolicReturnScenario() {
        int threadOneMode = Debug.makeSymbolicInteger("threadOneMode");
        int threadTwoMode = Debug.makeSymbolicInteger("threadTwoMode");
        boolean removeBookFromCatalog = Debug.makeSymbolicBoolean("removeBookFromCatalog");

        if (removeBookFromCatalog) {
            detachCatalogEntry();
        }

        ReturnOutcome firstOutcome = new ReturnOutcome("returner-1");
        ReturnOutcome secondOutcome = new ReturnOutcome("returner-2");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(2);

        Thread first = new Thread(new ReturnTask(startLatch, completeLatch, firstOutcome,
                resolveBookId(threadOneMode), resolveCustomerId(threadOneMode)), "return-thread-1");
        Thread second = new Thread(new ReturnTask(startLatch, completeLatch, secondOutcome,
                resolveBookId(threadTwoMode), resolveCustomerId(threadTwoMode)), "return-thread-2");

        first.start();
        second.start();
        startLatch.countDown();

        try {
            completeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for symbolic return threads", e);
        }

        Debug.printPC(String.format("%s=%s | %s=%s",
                firstOutcome.threadLabel, firstOutcome.result,
                secondOutcome.threadLabel, secondOutcome.result));
    }

    private void initializeCatalog() throws IOException {
        store.addBook(new Book(PRIMARY_BOOK_ID, "Symbolic Concurrency", 1));
        store.addBook(new Book(UNUSED_BOOK_ID, "Idle Inventory", 0));
    }

    private int registerCustomer(String username) throws IOException {
        User user = store.register(username, "pw", false);
        return user.getId();
    }

    private int resolveBookId(int rawMode) {
        int choice = categorizeChoice(rawMode);
        switch (choice) {
            case 0:
                return PRIMARY_BOOK_ID; // valid rental
            case 1:
                return UNUSED_BOOK_ID; // known book but never rented
            default:
                return PRIMARY_BOOK_ID + 100; // unknown id
        }
    }

    private int resolveCustomerId(int rawMode) {
        int choice = categorizeChoice(rawMode);
        switch (choice) {
            case 0:
                return validCustomerId;
            case 1:
                return otherCustomerId;
            default:
                try {
                    return registerCustomer("symbolic-" + (++syntheticUserCounter));
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to register symbolic customer", e);
                }
        }
    }

    private int categorizeChoice(int rawValue) {
        if (rawValue <= 0) {
            return 0;
        } else if (rawValue == 1) {
            return 1;
        }
        return 2;
    }

    @SuppressWarnings("unchecked")
    private void detachCatalogEntry() {
        try {
            Field booksField = LibraryStore.class.getDeclaredField("books");
            booksField.setAccessible(true);
            Map<Integer, Book> books = (Map<Integer, Book>) booksField.get(store);
            books.remove(PRIMARY_BOOK_ID);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to detach catalog entry for symbolic test", e);
        }
    }

    private final class ReturnTask implements Runnable {
        private final CountDownLatch startLatch;
        private final CountDownLatch completeLatch;
        private final ReturnOutcome outcome;
        private final int bookId;
        private final int customerId;

        private ReturnTask(CountDownLatch startLatch, CountDownLatch completeLatch,
                           ReturnOutcome outcome, int bookId, int customerId) {
            this.startLatch = startLatch;
            this.completeLatch = completeLatch;
            this.outcome = outcome;
            this.bookId = bookId;
            this.customerId = customerId;
        }

        @Override
        public void run() {
            try {
                startLatch.await();
                outcome.result = store.returnBook(bookId, customerId);
            } catch (IOException e) {
                outcome.result = "IO:" + e.getMessage();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                outcome.result = "INTERRUPTED";
            } finally {
                completeLatch.countDown();
            }
        }
    }

    private static final class ReturnOutcome {
        private final String threadLabel;
        private volatile String result = "NOT_EXECUTED";

        private ReturnOutcome(String threadLabel) {
            this.threadLabel = threadLabel;
        }
    }

    private static void ensureSystemProperty(String key, String fallbackValue) {
        if (System.getProperty(key) == null || System.getProperty(key).trim().isEmpty()) {
            System.setProperty(key, fallbackValue);
        }
    }
}

