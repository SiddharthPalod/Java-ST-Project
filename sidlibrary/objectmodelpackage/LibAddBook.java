// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.util.*;

public class LibAddBook {
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;

    public static void main(String[] args) {
        testAddBookUnified();
    }

    public static void testAddBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType");
        
        int bookAlreadyExists = Debug.makeSymbolicInteger("bookAlreadyExists"); // 0=new book, 1=existing book
        int addMultipleCopies = Debug.makeSymbolicInteger("addMultipleCopies"); // 0=single copy, 1=multiple copies
        int bookPendingReturn = Debug.makeSymbolicInteger("bookPendingReturn"); // 0=available, 1=pending return
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String bookName = Debug.makeSymbolicString("bookName");
        String bookAuthor = Debug.makeSymbolicString("bookAuthor");

        String bookSubject = Debug.makeSymbolicString("bookSubject");
        String bookGenre = Debug.makeSymbolicString("bookGenre");
        String bookCategory = Debug.makeSymbolicString("bookCategory");
        String bookFormat = Debug.makeSymbolicString("bookFormat");
        int bookSize = Debug.makeSymbolicInteger("bookSize");
        
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
        if (bookType == 0) {
            book = new Book(bookName, bookAuthor);
        } else if (bookType == 1) {
            String edition = Debug.makeSymbolicString("edition");
            book = new Textbook(bookName, bookAuthor, bookSubject, edition);
        } else if (bookType == 2) {
            book = new Novel(bookName, bookAuthor, bookGenre);
        } else if (bookType == 3) {
            book = new ReferenceBook(bookName, bookAuthor, bookCategory);
        } else if (bookType == 4) {
            book = new EBook(bookName, bookAuthor, bookFormat, bookSize);
        } else {
            book = new Book(bookName, bookAuthor); // default
        }
        
        // Setup: Add book if it should already exist
        if (bookAlreadyExists == 1) {
            addBookFunction(library, book);
            
            // Mark as pending return if needed
            if (bookPendingReturn == 1) {
                Book existingBook = checkBookAvailable(library, bookName);
                if (existingBook != null) {
                    existingBook.setPendingReturn(true);
                    existingBook.setBorrowerId("STUDENT123");
                }
            }
        }
        
        // MAIN TEST: Add book(s) to library
        addBookFunction(library, book);
        
        // Test adding multiple copies
        if (addMultipleCopies == 1) {
            // Add a fixed number of additional copies to test multiple copy scenarios
            // Using fixed numbers avoids non-linear constraints from modulo operations
            addBookFunction(library, book); // Add 2nd copy
            addBookFunction(library, book); // Add 3rd copy
        }
        
        // Verify book was added
        Books foundBooks = library.findBooksCollection(bookName);
        if (foundBooks != null) {
            int numCopies = foundBooks.getNumOfCopies();
            ArrayList<Book> bookList = foundBooks.getBooks();
        }
        
        // Test book properties after adding
        Book addedBook = checkBookAvailable(library, bookName);
        if (addedBook != null) {
            String name = addedBook.getBookName();
            String author = addedBook.getBookAuthor();
            boolean pending = addedBook.isPendingReturn();
        }
        
        // Test Borrowable interface if book implements it
        if (book instanceof Borrowable) {
            Borrowable borrowable = (Borrowable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            if (borrowable.canBorrow(borrowerId)) {
                borrowable.markAsBorrowed(borrowerId);
                int maxDays = borrowable.getMaxBorrowDays();
                int overdueDays = Debug.makeSymbolicInteger("overdueDays");
                double fee = borrowable.calculateLateFee(overdueDays);
            }
        }
        
        // Test Renewable interface
        if (book instanceof Renewable) {
            Renewable renewable = (Renewable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            if (renewable.canRenew(borrowerId)) {
                int newDays = renewable.renew(10);
            }
        }
        
        // Test Reservable interface
        if (book instanceof Reservable) {
            Reservable reservable = (Reservable) book;
            String reserverId = Debug.makeSymbolicString("reserverId");
            reservable.reserve(reserverId);
            boolean isReserved = reservable.isReserved();
            String nextReserver = reservable.getNextReserver();
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
        
        // Test adding different book types
        testAddDifferentBookTypes(library);
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

    private static void testAddDifferentBookTypes(Library library) {
        // Test adding a regular Book
        Book regularBook = new Book("RegularBook", "RegularAuthor");
        addBookFunction(library, regularBook);
        
        // Test adding a Textbook
        Textbook textbook = new Textbook("Textbook", "TextAuthor", "Math", "1st");
        addBookFunction(library, textbook);
        
        // Test adding a Novel
        Novel novel = new Novel("Novel", "NovelAuthor", "Fiction");
        addBookFunction(library, novel);
        
        // Test adding a ReferenceBook
        ReferenceBook refBook = new ReferenceBook("RefBook", "RefAuthor", "Encyclopedia");
        addBookFunction(library, refBook);
        
        // Test adding an EBook
        EBook eBook = new EBook("EBook", "EAuthor", "PDF", 1024);
        addBookFunction(library, eBook);
    }
}

