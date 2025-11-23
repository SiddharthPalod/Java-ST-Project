// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.text.SimpleDateFormat;
import java.util.*;

public class LibRenewBook {
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;

    public static void main(String[] args) {
        testRenewBookUnified();
    }

    public static void testRenewBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType"); // 2=Novel, 4=EBook (both implement Renewable)
        int studentType = Debug.makeSymbolicInteger("studentType");
        
        int hasStudent = Debug.makeSymbolicInteger("hasStudent"); // 0=no student, 1=has student
        int hasLibraryCard = Debug.makeSymbolicInteger("hasLibraryCard"); // 0=no card, 1=has card
        int hasBookIssued = Debug.makeSymbolicInteger("hasBookIssued"); // 0=not issued, 1=issued
        int correctBorrowerId = Debug.makeSymbolicInteger("correctBorrowerId"); // 0=wrong borrower, 1=correct borrower
        int renewalAttempts = Debug.makeSymbolicInteger("renewalAttempts"); // 0-5 to test multiple renewals
        int bookAlreadyRenewed = Debug.makeSymbolicInteger("bookAlreadyRenewed"); // 0=not renewed, 1=already renewed
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String bookName = Debug.makeSymbolicString("bookName");
        String bookAuthor = Debug.makeSymbolicString("bookAuthor");
        String studentId = Debug.makeSymbolicString("studentId");
        String studentName = Debug.makeSymbolicString("studentName");
        String studentProgram = Debug.makeSymbolicString("studentProgram");

        String bookGenre = Debug.makeSymbolicString("bookGenre");
        String bookFormat = Debug.makeSymbolicString("bookFormat");
        int studentYear = Debug.makeSymbolicInteger("studentYear");
        String studentMajor = Debug.makeSymbolicString("studentMajor");
        String researchArea = Debug.makeSymbolicString("researchArea");
        String advisor = Debug.makeSymbolicString("advisor");
        String dissertation = Debug.makeSymbolicString("dissertation");
        String homeUni = Debug.makeSymbolicString("homeUni");
        String exchangeProgram = Debug.makeSymbolicString("exchangeProgram");
        int duration = Debug.makeSymbolicInteger("duration");
        int bookSize = Debug.makeSymbolicInteger("bookSize");
        int currentDays = Debug.makeSymbolicInteger("currentDays");
        
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
        // Only test books that implement Renewable: Novel (type 2) and EBook (type 4)
        if (bookType == 2) {
            book = new Novel(bookName, bookAuthor, bookGenre);
        } else if (bookType == 4) {
            book = new EBook(bookName, bookAuthor, bookFormat, bookSize);
        } else {
            // Default to Novel for testing
            book = new Novel(bookName, bookAuthor, bookGenre);
        }
        addBookFunction(library, book);
        
        students = new ArrayList<>();
        Student student = null;
        
        if (hasStudent == 1) {
            if (studentType == 0) {
                student = new Student(studentName, studentId, studentProgram);
            } else if (studentType == 1) {
                student = new Undergraduate(studentName, studentId, studentProgram, studentYear, studentMajor);
            } else if (studentType == 2) {
                student = new Graduate(studentName, studentId, studentProgram, researchArea, advisor);
            } else if (studentType == 3) {
                student = new PhDStudent(studentName, studentId, studentProgram, researchArea, advisor, dissertation);
            } else if (studentType == 4) {
                student = new ExchangeStudent(studentName, studentId, studentProgram, homeUni, exchangeProgram, duration);
            } else {
                student = new Student(studentName, studentId, studentProgram); // default
            }
            addStudents(student);
            
            if (hasLibraryCard == 1) {
                issueLibraryCard(library, studentId);
            }
            
            if (hasBookIssued == 1) {
                issueBook(library, bookName, studentId);
                
                // Setup: Pre-renew book if needed
                if (bookAlreadyRenewed == 1 && book instanceof Renewable) {
                    Renewable renewable = (Renewable) book;
                    Book issuedBook = checkBookAvailable(library, bookName);
                    if (issuedBook != null && issuedBook.getBorrowerId() != null && 
                        issuedBook.getBorrowerId().equals(studentId)) {
                        // Pre-renew once
                        if (renewable.canRenew(studentId)) {
                            renewable.renew(10);
                        }
                    }
                }
            }
        }
        
        // MAIN TEST: Test renewal functionality
        if (book instanceof Renewable) {
            Renewable renewable = (Renewable) book;
            String borrowerId = studentId;
            if (correctBorrowerId == 0 && hasStudent == 1) {
                borrowerId = Debug.makeSymbolicString("wrongBorrowerId");
            }
            
            // Test canRenew with various conditions
            boolean canRenew = renewable.canRenew(borrowerId);
            int maxRenewals = renewable.getMaxRenewals();
            int renewalCount = renewable.getRenewalCount();
            
            // Test multiple renewal attempts
            if (canRenew && hasBookIssued == 1 && correctBorrowerId == 1) {
                // Attempt renewals based on renewalAttempts value
                if (renewalAttempts <= 2) {
                    int newDays1 = renewable.renew(currentDays);
                    int count1 = renewable.getRenewalCount();
                }
                if (renewalAttempts > 2 && renewalAttempts <= 4) {
                    int newDays2 = renewable.renew(currentDays);
                    int count2 = renewable.getRenewalCount();
                }
                if (renewalAttempts > 4) {
                    int newDays3 = renewable.renew(currentDays);
                    int count3 = renewable.getRenewalCount();
                }
            }
            
            // Test renewal after max renewals reached
            int finalRenewalCount = renewable.getRenewalCount();
            boolean canStillRenew = renewable.canRenew(borrowerId);
            
            // Test with null borrower ID
            boolean canRenewNull = renewable.canRenew(null);
            
            // Test with empty borrower ID
            boolean canRenewEmpty = renewable.canRenew("");
        }
        
        // Test Borrowable interface
        if (book instanceof Borrowable) {
            Borrowable borrowable = (Borrowable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            if (borrowable.canBorrow(borrowerId)) {
                borrowable.markAsBorrowed(borrowerId);
                int maxDays = borrowable.getMaxBorrowDays();
            }
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

    private static void issueBook(Library lib, String bookName, String studentId) {
        Book bookFound = checkBookAvailable(lib, bookName);
        if (bookFound == null) return;

        Student student = findStudent(studentId);
        if (student != null && student.hasLibraryAccess("COMSATS")) {
            RulesResultSet resultSet = maxBookStudentCanIssue("COMSATS", student.getInstituteName(), student.getProgramEnrolledIn());
            if (resultSet != null && student.getIssuedBooks().size() < resultSet.getNumOfBooks()) {
                bookFound.setBorrowerId(student.getStudentId());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Calendar c = Calendar.getInstance();
                bookFound.setBorrowDate(sdf.format(c.getTime()));
                c.add(Calendar.DATE, resultSet.getNumOfDays());
                bookFound.setReturnDate(sdf.format(c.getTime()));
                bookFound.setPendingReturn(true);
                student.addBookToIssueList(bookFound);
            }
        }
    }

    private static RulesResultSet maxBookStudentCanIssue(String bookLocation, String instituteName, String programEnrolledIn) {
        if (bookLocation.equals("COMSATS")) {
            return comsatsLibrary.comsatsRules(instituteName, programEnrolledIn);
        }
        return null;
    }

    public static Student findStudent(String studentId) {
        for (Student student : students) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
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

    private static void addStudents(Student student) {
        student.addStudent();
        students.add(student);
    }

    private static void issueLibraryCard(Library library, String studentId) {
        Student student = findStudent(studentId);
        if (student != null) {
            student.issueCard(library.getInstituteName());
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

