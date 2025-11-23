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
        int forceLateReturn = Debug.makeSymbolicInteger("forceLateReturn"); // 0=on time, 1=late return
        
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
                    // FORCE LATE RETURNS: Use symbolic constraint to test calculateFine branches
                    // Today's date in calculateFine is hardcoded as "26/06/2021"
                    // returnDate is the EXPECTED return date (when book should be returned)
                    // calculateFine compares: returnDate - today
                    // If returnDate < today (days < 0): book is LATE -> triggers fine calculation (line 328)
                    // If returnDate >= today (days >= 0): book is ON TIME -> no fine
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    Calendar c = Calendar.getInstance();
                    
                    // Set borrow date to a fixed past date (June 1, 2021)
                    String borrowDateStr = "01/06/2021";
                    issuedBook.setBorrowDate(borrowDateStr);
                    
                    // Expected return date = borrowDate + maxDays (12 days for COMSATS)
                    // So expected return = June 13, 2021
                    c.set(2021, 5, 1); // June 1, 2021 (month is 0-indexed, so 5 = June)
                    c.add(Calendar.DATE, 12); // Add maxDays (12)
                    String expectedReturnDateStr = sdf.format(c.getTime()); // "13/06/2021"
                    
                    // FORCE LATE RETURN: Use constraint to ensure returnDate < today
                    // Constraint: if forceLateReturn == 1, then days_borrowed > limit (12 days)
                    // This ensures the book is returned late, triggering the fine calculation
                    if (forceLateReturn == 1) {
                        // Force late: set returnDate to a date before today (26/06/2021)
                        // Expected return (13/06) is already before today (26/06), so it's late
                        // This will trigger the branch at line 328: if (days < 0)
                        issuedBook.setReturnDate(expectedReturnDateStr); // "13/06/2021" < "26/06/2021" = LATE
                        // Add constraint: ensure we're testing late return scenario
                        // The date comparison in calculateFine will be: 13/06 - 26/06 = -13 days (late)
                    } else {
                        // Force on-time: set returnDate to today or future to avoid fine
                        // This tests the branch where days >= 0 (no fine)
                        c.set(2021, 5, 26); // June 26, 2021 (today) - on time
                        String onTimeReturnDateStr = sdf.format(c.getTime());
                        issuedBook.setReturnDate(onTimeReturnDateStr); // "26/06/2021" >= "26/06/2021" = ON TIME
                    }
                    
                    issuedBook.setPendingReturn(true);
                    issuedBook.setBorrowerId(studentId);
                    student.addBookToIssueList(issuedBook);
                }
            }
        }        
        
        // NEGATIVE TEST: Try to return book even when not borrowed
        // This forces error branches in returnBook when book is not in student's issued list
        String returnBookName = bookName;
        if (correctBookName == 0 && hasStudent == 1 && student != null) {
            returnBookName = Debug.makeSymbolicString("wrongBookName");
        }
        // Always call returnBook to test both positive and negative cases
        // When hasBookIssued == 0, this tests the negative case (book not borrowed)
        returnBook(library, studentId, returnBookName);        
        // DEPRECATED: Borrowable and Searchable interfaces show 0% coverage
        // These interfaces are not implemented by any Book classes, creating noise
        // Commented out to focus on actual code paths
        /*
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
        */
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
        /*
        // DEPRECATED: Searchable interface shows 0% coverage - not implemented by any Book classes
        if (book instanceof Searchable) {
            Searchable searchable = (Searchable) book;
            String query = Debug.makeSymbolicString("query");
            boolean matches = searchable.matches(query);
            double score = searchable.getRelevanceScore(query);
            String content = searchable.getSearchableContent();
        }
        */
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
