// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.text.SimpleDateFormat;
import java.util.*;

public class LibReturnBook {
    private static final int perDayFine = 10;
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;
    private static boolean outOfStock = false;

    public static void main(String[] args) {
        testReturnBookUnified();
    }

    public static void testReturnBookUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int bookType = Debug.makeSymbolicInteger("bookType");        
        int studentType = Debug.makeSymbolicInteger("studentType");
        
        int hasStudent = Debug.makeSymbolicInteger("hasStudent"); // 0=no student, 1=has student
        int hasLibraryCard = Debug.makeSymbolicInteger("hasLibraryCard"); // 0=no card, 1=has card
        int hasBookIssued = Debug.makeSymbolicInteger("hasBookIssued"); // 0=not issued, 1=issued
        int correctBookName = Debug.makeSymbolicInteger("correctBookName"); // 0=wrong book, 1=correct book
        
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
                Book issuedBook = checkBookAvailable(library, bookName);
                if (issuedBook != null && student != null) {
                    issuedBook.setBorrowDate("01/06/2021");
                    issuedBook.setReturnDate("15/06/2021");
                    issuedBook.setPendingReturn(true);
                    issuedBook.setBorrowerId(studentId);
                    student.addBookToIssueList(issuedBook);
                }
            }
        }        
        String returnBookName = bookName;
        if (correctBookName == 0 && hasStudent == 1 && student != null) {
            returnBookName = Debug.makeSymbolicString("wrongBookName");
        }
        returnBook(library, studentId, returnBookName);        
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
        if (book instanceof Renewable) {
            Renewable renewable = (Renewable) book;
            String borrowerId = Debug.makeSymbolicString("borrowerId");
            if (renewable.canRenew(borrowerId)) {
                int newDays = renewable.renew(10);
            }
        }        
        if (book instanceof Reservable) {
            Reservable reservable = (Reservable) book;
            String reserverId = Debug.makeSymbolicString("reserverId");
            reservable.reserve(reserverId);
            boolean isReserved = reservable.isReserved();
            String nextReserver = reservable.getNextReserver();
        }        
        if (book instanceof Searchable) {
            Searchable searchable = (Searchable) book;
            String query = Debug.makeSymbolicString("query");
            boolean matches = searchable.matches(query);
            double score = searchable.getRelevanceScore(query);
            String content = searchable.getSearchableContent();
        }
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

    // ================= NEW SYMBOLIC DRIVER FOR ISSUE BOOK =================
    public static void testIssueBook() {
        // Symbolic selectors for object kinds
        int libraryType = Debug.makeSymbolicInteger("issue_libraryType");
        int bookType = Debug.makeSymbolicInteger("issue_bookType");
        int studentType = Debug.makeSymbolicInteger("issue_studentType");

        // Symbolic flags controlling presence/state
        int hasStudent = Debug.makeSymbolicInteger("issue_hasStudent"); // 0=no student, 1=has student
        int hasLibraryCard = Debug.makeSymbolicInteger("issue_hasLibraryCard"); // 0=no card, 1=has card
        int attemptIssue = Debug.makeSymbolicInteger("issue_attemptIssue"); // 0=skip issue, 1=attempt

        // Core symbolic data
        String libraryName = Debug.makeSymbolicString("issue_libraryName");
        String bookName = Debug.makeSymbolicString("issue_bookName");
        String bookAuthor = Debug.makeSymbolicString("issue_bookAuthor");
        String studentId = Debug.makeSymbolicString("issue_studentId");
        String studentName = Debug.makeSymbolicString("issue_studentName");
        String studentProgram = Debug.makeSymbolicString("issue_studentProgram");

        // Extended attributes for specialised book/student types
        String bookSubject = Debug.makeSymbolicString("issue_bookSubject");
        String bookGenre = Debug.makeSymbolicString("issue_bookGenre");
        String bookCategory = Debug.makeSymbolicString("issue_bookCategory");
        String bookFormat = Debug.makeSymbolicString("issue_bookFormat");
        int bookSize = Debug.makeSymbolicInteger("issue_bookSize");

        int studentYear = Debug.makeSymbolicInteger("issue_studentYear");
        String studentMajor = Debug.makeSymbolicString("issue_studentMajor");
        String researchArea = Debug.makeSymbolicString("issue_researchArea");
        String advisor = Debug.makeSymbolicString("issue_advisor");
        String dissertation = Debug.makeSymbolicString("issue_dissertation");
        String homeUni = Debug.makeSymbolicString("issue_homeUni");
        String exchangeProgram = Debug.makeSymbolicString("issue_exchangeProgram");
        int duration = Debug.makeSymbolicInteger("issue_duration");

        // Construct library polymorphically
        Library library;
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

        // Prepare catalog
        Books books = new Books();
        books.setBooks(new Book().displayBooks());
        library.addBookToLibrary(books);
        booksArrayList = library.getBooksArrayList();

        // Create symbolic book instance
        Book book;
        if (bookType == 0) {
            book = new Book(bookName, bookAuthor);
        } else if (bookType == 1) {
            String edition = Debug.makeSymbolicString("issue_edition");
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
        addBookFunction(library, book);

        // Students collection
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
        }

        // Attempt issuance under varying preconditions
        if (attemptIssue == 1) {
            issueBook(library, bookName, studentId);
        }

        // Lightweight post-condition exploration
        Book postBook = checkBookAvailable(library, bookName);
        int probe = Debug.makeSymbolicInteger("issue_probe");
        if (postBook != null && probe == 1) {
            // Access a few attributes to keep them in path conditions
            String borrower = postBook.getBorrowerId();
            boolean pending = postBook.isPendingReturn();
        }
    }

    public static void addBookFunction(Library lib, Book book) {
        //@ assume book != null;
        book.addBook();
        book.setPendingReturn(false);

        //@ assert !book.isPendingReturn();

        Book foundBook = checkBookAvailable(lib, book.getBookName());
        //@ assert foundBook == null || foundBook.getBookName().equals(book.getBookName());

        if (foundBook != null) {
            //@ assume lib.findBooksCollection(book.getBookName()) != null;
            Books books = lib.findBooksCollection(book.getBookName());
            if (books != null) {
                Book newBook = new Book(foundBook);
                books.addBookToList(newBook);
                books.setNumOfCopies(books.getNumOfCopies() + 1);
                //@ assert books.getNumOfCopies() > 0;
            }
        } else {
            Books books = new Books();
            books.setNumOfCopies(1);
            books.addBookToList(book);
            lib.addBookToLibrary(books);
            //@ assert books.getNumOfCopies() == 1;
        }
    }

    private static void returnBook(Library library, String studentId, String bookName) {
        Student student = findStudent(studentId);
        if (student != null) {
            ArrayList<Book> bookList = student.getIssuedBooks();
            if (!bookList.isEmpty()) {
                for (Book book : bookList) {
                    calculateFine(student, book.getBorrowDate(), book.getReturnDate());
                }
                for (int i = 0; i < bookList.size(); i++) {
                    Book book = bookList.get(i);
                    if (book.getBookName().equalsIgnoreCase(bookName)) {
                        bookList.remove(i);
                        book.setPendingReturn(false);
                        break;
                    }
                }
            }
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
        return null; // Return null if no student is found
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

    private static void addStudents(Student student) {
        student.addStudent();
        students.add(student);
    }

    private static void calculateFine(Student student, String borrowDate, String returnDate) {
        String todaysDate = "26/06/2021";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            java.util.Date returnDateObj = sdf.parse(returnDate);
            java.util.Date todayDateObj = sdf.parse(todaysDate);
            
            // Calculate difference in milliseconds, then convert to days
            long diffInMillis = returnDateObj.getTime() - todayDateObj.getTime();
            long days = diffInMillis / (24 * 60 * 60 * 1000);

            if (days < 0) {
                int numOfDays = (int) Math.abs(days);
                int totalFine = student.getFine() + numOfDays * perDayFine;
                student.setFine(totalFine);
            }
        } catch (Exception e) {
            System.out.println("Date parsing failed: " + e.getMessage());
        }
    }

    private static void issueLibraryCard(Library library, String studentId) {
        Student student = findStudent(studentId);
        if (student != null) {
            student.issueCard(library.getInstituteName());
        }
    }

    // ========== NEW POLYMORPHIC TEST METHODS ==========

    // Helper methods for testing interfaces
    private static void testBorrowableInterface(Borrowable borrowable) {
        String borrowerId = Debug.makeSymbolicString("borrowerId");
        if (borrowable.canBorrow(borrowerId)) {
            borrowable.markAsBorrowed(borrowerId);
            int maxDays = borrowable.getMaxBorrowDays();
            int overdueDays = Debug.makeSymbolicInteger("overdueDays");
            double fee = borrowable.calculateLateFee(overdueDays);
        }
    }

    private static void testSearchableInterface(Searchable searchable, String query) {
        boolean matches = searchable.matches(query);
        double score = searchable.getRelevanceScore(query);
        String content = searchable.getSearchableContent();
    }

    private static void testLibraryMemberInterface(LibraryMember member, Library library) {
        String memberId = member.getMemberId();
        String memberName = member.getMemberName();
        boolean hasMembership = member.hasActiveMembership();
        if (!hasMembership) {
            member.activateMembership();
        }
        int limit = member.getBorrowingLimit();
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

    private static void testInstituteAbstractMethods(Institute institute, String memberId, String memberType) {
        boolean valid = institute.validateMembership(memberId);
        int limit = institute.getMaxBorrowingLimit(memberType);
        java.util.List<String> services = institute.getAvailableServices();
        double fee = institute.calculateMembershipFee(memberType);
        boolean active = institute.isActive();
        int age = institute.getAge();
    }
}
