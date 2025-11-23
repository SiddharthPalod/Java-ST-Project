package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.text.SimpleDateFormat;
import java.util.*;

public class LibIssueBook1 {
    // Static state replicated from LibReturnBook needed for symbolic driver
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;
    private static ArrayList<Books> booksArrayList;
    private static boolean outOfStock = false;

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

    // Helper mimicking logic from LibReturnBook to add book copies
    private static void addBookFunction(Library lib, Book book) {
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

    private static RulesResultSet maxBookStudentCanIssue(String bookLocation, String instituteName, String programEnrolledIn) {
        if (bookLocation.equals("COMSATS")) {
            return comsatsLibrary.comsatsRules(instituteName, programEnrolledIn);
        }
        return null;
    }

    // ================= SYMBOLIC TEST FOR checkBookAvailable =================
    public static void testCheckBookAvailable() {
        // Initialize library context
        String libName = gov.nasa.jpf.symbc.Debug.makeSymbolicString("c_libraryName");
        comsatsLibrary = new Library(libName);

        // Bounded symbolic number of book collections
        int numCollections = gov.nasa.jpf.symbc.Debug.makeSymbolicInteger("c_numCollections");
        //@ assume numCollections >= 0 && numCollections <= 2;
        booksArrayList = new ArrayList<>();

        for (int ci = 0; ci < 3; ci++) { // upper bound loop unrolled symbolically
            if (ci < numCollections) {
                Books bs = new Books();
                int numBooks = gov.nasa.jpf.symbc.Debug.makeSymbolicInteger("c_numBooks_" + ci);
                //@ assume numBooks >= 0 && numBooks <= 2;
                ArrayList<Book> internal = new ArrayList<>();
                for (int bi = 0; bi < 3; bi++) { // bounded
                    if (bi < numBooks) {
                        String bName = gov.nasa.jpf.symbc.Debug.makeSymbolicString("c_bookName_" + ci + "_" + bi);
                        String bAuthor = gov.nasa.jpf.symbc.Debug.makeSymbolicString("c_bookAuthor_" + ci + "_" + bi);
                        Book b = new Book(bName, bAuthor);
                        int pendingFlag = gov.nasa.jpf.symbc.Debug.makeSymbolicInteger("c_pending_" + ci + "_" + bi);
                        //@ assume pendingFlag == 0 || pendingFlag == 1;
                        b.setPendingReturn(pendingFlag == 1);
                        internal.add(b);
                    }
                }
                bs.setBooks(internal);
                bs.setNumOfCopies(internal.size());
                booksArrayList.add(bs);
            }
        }

        // Symbolic query
        String queryName = gov.nasa.jpf.symbc.Debug.makeSymbolicString("c_queryName");
        Book result = checkBookAvailable(comsatsLibrary, queryName);
        int observe = gov.nasa.jpf.symbc.Debug.makeSymbolicInteger("c_observe");
        if (result != null && observe == 1) {
            String foundName = result.getBookName();
            boolean pending = result.isPendingReturn();
        }
    }

    // ================= SYMBOLIC TEST FOR addBooksToLibrary =================
    public static void testAddBooksToLibrary() {
        String libName = Debug.makeSymbolicString("ab_libraryName");
        Library library = new Library(libName);
        comsatsLibrary = library;
        booksArrayList = library.getBooksArrayList();

        int numAdds = Debug.makeSymbolicInteger("ab_numAdds");
        //@ assume numAdds >= 0 && numAdds <= 3;

        String lastName = null;
        for (int i = 0; i < 4; i++) { // bounded unrolling
            if (i < numAdds) {
                String bookName = Debug.makeSymbolicString("ab_bookName_" + i);
                int reuse = Debug.makeSymbolicInteger("ab_reuse_" + i); //@ 0=fresh,1=duplicate previous
                //@ assume reuse == 0 || reuse == 1;
                if (reuse == 1 && lastName != null) {
                    bookName = lastName; // force duplicate path
                }
                String bookAuthor = Debug.makeSymbolicString("ab_bookAuthor_" + i);
                addBooksToLibrary(library, bookName, bookAuthor);
                booksArrayList = library.getBooksArrayList();
                lastName = bookName;
            }
        }

        // Post-condition exploration
        int observe = Debug.makeSymbolicInteger("ab_observe");
        if (observe == 1) {
            int totalCopies = 0;
            int collections = booksArrayList.size();
            for (Books bs : booksArrayList) {
                totalCopies += bs.getNumOfCopies();
                // touch first book if exists
                java.util.List<Book> internal = bs.getBooks();
                if (!internal.isEmpty()) {
                    String nm = internal.get(0).getBookName();
                }
            }
            // Simple sanity assertion pattern (conceptual for JPF)
            //@ assert totalCopies >= collections; // copies at least number of collections
        }
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
    private static void addBooksToLibrary(Library library, String bookName, String bookAuthor) {
        Book book = new Book(bookName, bookAuthor);
        book.addBook();
        book.setPendingReturn(false);


        Book foundBook = checkBookAvailable(library, bookName);
        if(foundBook != null) {
            Books books = library.findBooksCollection(bookName);
            if(books != null) {
                Book newBook = new Book(foundBook);
                books.addBookToList(newBook);
                books.setNumOfCopies(books.getNumOfCopies()+1);
            }
            else {
                System.out.println("Sorry but there's a bug");
            }
        }
        else {
            Books books = new Books();
            books.setNumOfCopies(1);
            books.addBookToList(book);
            library.addBookToLibrary(books);
        }
    }
}
