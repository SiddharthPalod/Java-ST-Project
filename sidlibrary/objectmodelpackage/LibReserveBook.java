// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.util.*;

public class LibReserveBook {
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;

    public static void main(String[] args) {
        testReserveBookUnified();
    }

    public static void testReserveBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType"); // 3=ReferenceBook (implements Reservable)
        
        int bookExists = Debug.makeSymbolicInteger("bookExists"); // 0=book doesn't exist, 1=book exists
        int hasReservations = Debug.makeSymbolicInteger("hasReservations"); // 0=no reservations, 1=has reservations
        int numReservations = Debug.makeSymbolicInteger("numReservations"); // Number of existing reservations
        int cancelReservation = Debug.makeSymbolicInteger("cancelReservation"); // 0=don't cancel, 1=cancel
        int duplicateReservation = Debug.makeSymbolicInteger("duplicateReservation"); // 0=new reserver, 1=duplicate
        int validReserverId = Debug.makeSymbolicInteger("validReserverId"); // 0=null/empty, 1=valid
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String bookName = Debug.makeSymbolicString("bookName");
        String bookAuthor = Debug.makeSymbolicString("bookAuthor");
        String reserverId = Debug.makeSymbolicString("reserverId");
        String reserverId2 = Debug.makeSymbolicString("reserverId2");
        String reserverId3 = Debug.makeSymbolicString("reserverId3");

        String bookCategory = Debug.makeSymbolicString("bookCategory");
        
        Library library = null;
        if (libraryType == 0) {
            library = new Library(libraryName);
        } else if (libraryType == 1) {
            library = new UniversityLibrary(libraryName);
        } else if (libraryType == 2) {
            library = new PublicLibrary(libraryName);
        } else if (libraryType == 3) {
            library = new DigitalLibrary(libraryName);
        } else {
            library = new Library(libraryName); // default
        }
        comsatsLibrary = library;
        
        Books books = new Books();
        books.setBooks(new Book().displayBooks());
        library.addBookToLibrary(books);
        booksArrayList = library.getBooksArrayList();
        
        Book book = null;
        // Only test books that implement Reservable: ReferenceBook (type 3)
        if (bookType == 3) {
            book = new ReferenceBook(bookName, bookAuthor, bookCategory);
        } else {
            // Default to ReferenceBook for testing
            book = new ReferenceBook(bookName, bookAuthor, bookCategory);
        }
        
        if (bookExists == 1) {
            addBookFunction(library, book);
            
            // Setup: Add existing reservations if needed
            if (hasReservations == 1 && book instanceof Reservable) {
                Reservable reservable = (Reservable) book;
                // Add 1-3 reservations based on numReservations
                if (numReservations <= 10) {
                    reservable.reserve(reserverId2);
                }
                if (numReservations > 10 && numReservations <= 20) {
                    reservable.reserve(reserverId2);
                    reservable.reserve(reserverId3);
                }
                if (numReservations > 20) {
                    reservable.reserve(reserverId2);
                    reservable.reserve(reserverId3);
                    reservable.reserve("RESERVER4");
                }
            }
        }
        
        // MAIN TEST: Test reservation functionality
        if (book instanceof Reservable) {
            Reservable reservable = (Reservable) book;
            
            // Test reservation with various conditions
            String testReserverId = reserverId;
            if (validReserverId == 0) {
                // Test with null or empty (will be handled by reserve method)
                if (duplicateReservation == 0) {
                    testReserverId = null;
                } else {
                    testReserverId = "";
                }
            }
            
            // Test reserve method
            boolean reserveResult = reservable.reserve(testReserverId);
            
            // Test duplicate reservation
            if (duplicateReservation == 1 && validReserverId == 1) {
                boolean duplicateResult = reservable.reserve(reserverId); // Try to reserve again
            }
            
            // Test isReserved
            boolean isReserved = reservable.isReserved();
            
            // Test getNextReserver
            String nextReserver = reservable.getNextReserver();
            
            // Test getReservationQueue
            java.util.List<String> queue = reservable.getReservationQueue();
            int queueSize = queue != null ? queue.size() : 0;
            
            // Test cancelReservation
            if (cancelReservation == 1 && validReserverId == 1) {
                boolean cancelResult = reservable.cancelReservation(reserverId);
                boolean stillReserved = reservable.isReserved();
                String nextAfterCancel = reservable.getNextReserver();
            }
            
            // Test cancel non-existent reservation
            boolean cancelNonExistent = reservable.cancelReservation("NON_EXISTENT");
            
            // Test with multiple reservations and cancellations
            if (hasReservations == 1) {
                // Cancel first reservation
                boolean cancelFirst = reservable.cancelReservation(reserverId2);
                String nextAfterFirstCancel = reservable.getNextReserver();
            }
        }
        
        // Test Borrowable interface (ReferenceBook also implements Borrowable)
        if (book instanceof Borrowable) {
            Borrowable borrowable = (Borrowable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            boolean canBorrow = borrowable.canBorrow(borrowerId);
            if (canBorrow) {
                borrowable.markAsBorrowed(borrowerId);
                int maxDays = borrowable.getMaxBorrowDays();
            }
        }
        
        // Test Searchable interface
        if (book instanceof Searchable) {
            Searchable searchable = (Searchable) book;
            String query = Debug.makeSymbolicString("query");
            boolean matches = searchable.matches(query);
            double score = searchable.getRelevanceScore(query);
            String content = searchable.getSearchableContent();
        }
        
        // Test polymorphic library methods
        testLibraryPolymorphism(library);
        
        // Test Institute interface
        if (library instanceof Institute) {
            Institute institute = (Institute) library;
            String memberId = Debug.makeSymbolicString("memberId");
            String memberType = Debug.makeSymbolicString("memberType");
            boolean valid = institute.validateMembership(memberId);
            int limit = institute.getMaxBorrowingLimit(memberType);
            java.util.List<String> services = institute.getAvailableServices();
            double fee = institute.calculateMembershipFee(memberType);
            boolean active = institute.isActive();
            int age = institute.getAge();
        }
    }

    public static void addBookFunction(Library lib, Book book) {
        book.addBook();
        book.setPendingReturn(false);

        Book foundBook = checkBookAvailable(lib, book.getBookName());

        if (foundBook != null) {
            Books books = lib.findBooksCollection(book.getBookName());
            if (books != null) {
                Book newBook = new Book(foundBook);
                books.addBookToList(newBook);
                books.setNumOfCopies(books.getNumOfCopies() + 1);
            }
        } else {
            Books books = new Books();
            books.setNumOfCopies(1);
            books.addBookToList(book);
            lib.addBookToLibrary(books);
        }
    }

    private static Book checkBookAvailable(Library library, String getBookName) {
        for (Books books : booksArrayList) {
            for (Book book : books.getBooks()) {
                if (book.getBookName().equalsIgnoreCase(getBookName) && !book.isPendingReturn()) {
                    return book;
                }
            }
        }
        return null;
    }

    private static void testLibraryPolymorphism(Library library) {
        String instituteName = library.getInstituteName();
        boolean isValid = library.validateMembership("TEST123");
        int limit = library.getMaxBorrowingLimit("UG");
        java.util.List<String> services = library.getAvailableServices();
        double fee = library.calculateMembershipFee("UG");
    }
}

