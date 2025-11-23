// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.text.SimpleDateFormat;
import java.util.*;

public class LibIssueBook {
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;
    private static boolean outOfStock = false;

    public static void main(String[] args) {
        testIssueBookUnified();
    }

    public static void testIssueBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType");        
        int studentType = Debug.makeSymbolicInteger("studentType");
        
        int hasStudent = Debug.makeSymbolicInteger("hasStudent"); // 0=no student, 1=has student
        int hasLibraryCard = Debug.makeSymbolicInteger("hasLibraryCard"); // 0=no card, 1=has card
        int bookAlreadyExists = Debug.makeSymbolicInteger("bookAlreadyExists"); // 0=new book, 1=existing book
        int bookAvailable = Debug.makeSymbolicInteger("bookAvailable"); // 0=not available, 1=available
        int withinBorrowingLimit = Debug.makeSymbolicInteger("withinBorrowingLimit"); // 0=exceeds limit, 1=within limit
        int correctBookName = Debug.makeSymbolicInteger("correctBookName"); // 0=wrong book, 1=correct book
        int hasLibraryAccess = Debug.makeSymbolicInteger("hasLibraryAccess"); // 0=no access, 1=has access
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String bookName = Debug.makeSymbolicString("bookName");
        String bookAuthor = Debug.makeSymbolicString("bookAuthor");
        String studentId = Debug.makeSymbolicString("studentId");
        String studentName = Debug.makeSymbolicString("studentName");
        String studentProgram = Debug.makeSymbolicString("studentProgram");

        String bookSubject = Debug.makeSymbolicString("bookSubject");
        String bookGenre = Debug.makeSymbolicString("bookGenre");
        String bookCategory = Debug.makeSymbolicString("bookCategory");
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
        
        // Setup: Add book to library if it should exist
        if (bookAlreadyExists == 1) {
            addBookFunction(library, book);
        }
        
        // Setup: Make book unavailable if needed (mark as pending return)
        if (bookAvailable == 0 && bookAlreadyExists == 1) {
            Book existingBook = checkBookAvailable(library, bookName);
            if (existingBook != null) {
                existingBook.setPendingReturn(true);
                existingBook.setBorrowerId("OTHER_STUDENT");
            }
        }
        
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
            
            // Setup: Issue library card if needed
            if (hasLibraryCard == 1) {
                issueLibraryCard(library, studentId);
            }
            
            // Setup: Pre-issue books to test borrowing limit
            if (withinBorrowingLimit == 0 && hasLibraryCard == 1) {
                // Issue maximum books to exceed limit
                RulesResultSet resultSet = maxBookStudentCanIssue("COMSATS", student.getInstituteName(), student.getProgramEnrolledIn());
                if (resultSet != null) {
                    for (int i = 0; i < resultSet.getNumOfBooks(); i++) {
                        Book tempBook = new Book("TempBook" + i, "TempAuthor" + i);
                        addBookFunction(library, tempBook);
                        issueBook(library, "TempBook" + i, studentId);
                    }
                }
            }
        }
        
        // MAIN TEST: Try to issue book
        String issueBookName = bookName;
        if (correctBookName == 0) {
            issueBookName = Debug.makeSymbolicString("wrongBookName");
        }
        
        // Test issueBook with various scenarios
        issueBook(library, issueBookName, studentId);
        
        // Test Borrowable interface if book implements it
        if (book instanceof Borrowable) {
            Borrowable borrowable = (Borrowable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            if (borrowable.canBorrow(borrowerId)) {
                borrowable.markAsBorrowed(borrowerId);
                int maxDays = borrowable.getMaxBorrowDays();
                int overdueDays = Debug.makeSymbolicInteger("overdueDays");
                double fee = borrowable.calculateLateFee(overdueDays);
                borrowable.markAsReturned();
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
        
        // Test LibraryMember interface
        if (student instanceof LibraryMember) {
            LibraryMember member = (LibraryMember) student;
            String memberId = member.getMemberId();
            String memberName = member.getMemberName();
            boolean hasMembership = member.hasActiveMembership();
            if (!hasMembership) {
                member.activateMembership();
            }
            int limit = member.getBorrowingLimit();
        }
        
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
        
        // Test polymorphic methods
        if (student != null) {
            testStudentPolymorphism(student);
        }
        testLibraryPolymorphism(library);
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
        outOfStock = false;
        for (Books books : booksArrayList) {
            for (Book book : books.getBooks()) {
                if (book.getBookName().equalsIgnoreCase(getBookName) && !book.isPendingReturn()) {
                    return book;
                }
            }
        }
        outOfStock = true;
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

    private static void testStudentPolymorphism(Student student) {
        String instituteName = Debug.makeSymbolicString("instituteName");
        int result = student.issueCard(instituteName);
        boolean hasAccess = student.hasLibraryAccess(instituteName);
        String studentId = student.getStudentId();
        String studentName = student.getStudentName();
    }

    private static void testLibraryPolymorphism(Library library) {
        String instituteName = library.getInstituteName();
        boolean isValid = library.validateMembership("TEST123");
        int limit = library.getMaxBorrowingLimit("UG");
        java.util.List<String> services = library.getAvailableServices();
        double fee = library.calculateMembershipFee("UG");
    }
}

