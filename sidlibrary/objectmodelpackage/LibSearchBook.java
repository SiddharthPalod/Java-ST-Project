// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.util.*;

public class LibSearchBook {
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;

    public static void main(String[] args) {
        testSearchBookUnified();
    }

    public static void testSearchBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType");
        
        int bookExists = Debug.makeSymbolicInteger("bookExists"); // 0=book doesn't exist, 1=book exists
        int searchBy = Debug.makeSymbolicInteger("searchBy"); // 0=name, 1=author, 2=genre/subject/category, 3=partial match
        int exactMatch = Debug.makeSymbolicInteger("exactMatch"); // 0=partial match, 1=exact match
        int multipleBooks = Debug.makeSymbolicInteger("multipleBooks"); // 0=single book, 1=multiple books
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String bookName = Debug.makeSymbolicString("bookName");
        String bookAuthor = Debug.makeSymbolicString("bookAuthor");
        String searchQuery = Debug.makeSymbolicString("searchQuery");

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
        
        // Setup: Add book(s) to library if they should exist
        if (bookExists == 1) {
            addBookFunction(library, book);
            
            // Add multiple books if needed for testing
            if (multipleBooks == 1) {
                Book book2 = new Book(bookName + "2", bookAuthor + "2");
                addBookFunction(library, book2);
                
                if (bookType == 2) { // Novel
                    Book book3 = new Novel(bookName + "3", bookAuthor + "3", bookGenre);
                    addBookFunction(library, book3);
                }
            }
        }
        
        // MAIN TEST: Search for books using findBooksCollection
        String searchBookName = bookName;
        if (searchBy == 0) {
            // Search by exact name
            if (exactMatch == 1) {
                searchBookName = bookName;
            } else {
                // Partial match - use first few characters if available
                if (bookName != null && bookName.length() > 0) {
                    int len = Math.min(bookName.length(), 3);
                    searchBookName = bookName.substring(0, len);
                } else {
                    searchBookName = searchQuery;
                }
            }
        } else if (searchBy == 1) {
            // Search by author (will use bookName as search term)
            searchBookName = bookAuthor;
        } else if (searchBy == 2) {
            // Search by genre/subject/category
            if (bookType == 1) {
                searchBookName = bookSubject; // Textbook
            } else if (bookType == 2) {
                searchBookName = bookGenre; // Novel
            } else if (bookType == 3) {
                searchBookName = bookCategory; // ReferenceBook
            } else {
                searchBookName = bookName;
            }
        } else {
            // Use symbolic query
            searchBookName = searchQuery;
        }
        
        Books foundBooks = library.findBooksCollection(searchBookName);
        
        // Test Searchable interface if book implements it
        if (book instanceof Searchable) {
            Searchable searchable = (Searchable) book;
            
            // Test with different query types
            String query = searchQuery;
            if (searchBy == 0) {
                query = bookName;
            } else if (searchBy == 1) {
                query = bookAuthor;
            } else if (searchBy == 2) {
                if (bookType == 2) {
                    query = bookGenre; // Novel
                } else if (bookType == 1) {
                    query = bookSubject; // Textbook
                } else if (bookType == 3) {
                    query = bookCategory; // ReferenceBook
                }
            }
            
            boolean matches = searchable.matches(query);
            double score = searchable.getRelevanceScore(query);
            String content = searchable.getSearchableContent();
            
            // Test with null query
            boolean matchesNull = searchable.matches(null);
            double scoreNull = searchable.getRelevanceScore(null);
            
            // Test with empty query
            boolean matchesEmpty = searchable.matches("");
            double scoreEmpty = searchable.getRelevanceScore("");
        }
        
        // Test checkBookAvailable method
        Book availableBook = checkBookAvailable(library, searchBookName);
        
        // Test with different search scenarios
        if (bookExists == 1) {
            // Search for existing book
            Books existingBooks = library.findBooksCollection(bookName);
            
            // Search for book that might be borrowed
            Book borrowedBook = checkBookAvailable(library, bookName);
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

    private static void testLibraryPolymorphism(Library library) {
        String instituteName = library.getInstituteName();
        boolean isValid = library.validateMembership("TEST123");
        int limit = library.getMaxBorrowingLimit("UG");
        java.util.List<String> services = library.getAvailableServices();
        double fee = library.calculateMembershipFee("UG");
    }
}

