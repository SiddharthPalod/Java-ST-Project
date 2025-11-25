# Symbolic Execution Analysis of a Java Library Management System

## Project Overview

This project is a comprehensive, object-oriented Library Management System implemented in Java. The system is designed to model the core functionalities of a real-world library, providing a robust foundation for managing books, library members, and transactions.

The domain model is built around a clear class hierarchy. Key entities include:
- **`Book`**: A base class with specialized subclasses like `Textbook`, `Novel`, `ReferenceBook`, and `EBook`, each with potentially unique attributes and rules.
- **`Student`**: Represents library members, with subclasses such as `Undergraduate`, `Graduate`, and `PhDStudent` to handle different borrowing privileges and rules.
- **`Library`**: Manages the collections of books and members, and provides the main interface for all operations.

The system supports a range of standard library operations, including adding new books and students, issuing books to members, processing returns, calculating fines, renewing borrowed items, and searching the catalog.

## Adaptation for Symbolic Execution

To rigorously test the system's logic, the source code was analyzed using **Symbolic PathFinder (SPF)**, an extension of the **Java PathFinder (JPF)** framework that enables symbolic execution. Symbolic execution is a powerful testing technique that explores multiple execution paths of a program simultaneously without requiring concrete input values.

To facilitate this, some of the original methods were adapted or rewritten into specialized **symbolic drivers**. These drivers replace concrete inputs with symbolic variables, allowing SPF to automatically explore different conditions and branches in the code. For example, instead of testing with a single student ID, a symbolic variable is used to represent all possible student IDs. This approach enables the discovery of edge-case bugs and ensures a high degree of branch coverage, which might be missed with traditional testing methods.
## Prerequisites and Setup

1. **Clone the SPF Repository**  
    Clone the SPF repository from [SPF GitHub Repository](https://github.com/SymbolicPathFinder/jpf-symbc/tree/gradle-build).

2. **Install Java 8**  
    Download and install [Java 8](https://www.oracle.com/in/java/technologies/javase/javase8-archive-downloads.html).  
    Ensure that both `java -version` and `javac -version` point to Java 8.

3. **Download and Install Maven**  
    Follow the Maven installation instructions provided in the SPF GitHub repository linked in step 1.

4. **Add Testing Folders**  
    Copy the sidlibrary folder of this repository into:  
    ```
    jpf-symbc/src/examples/
    ```

---

## Running the Code

1. **Compile the Codebase**  
    Run the following command to compile and create the required classes:  
    ```bash
    cd SPF
    gradle :jpf-symbc:buildJars
    ```

2. **Navigate into SPF/jpf-symbc**  
    ```bash
    cd jpf-symbc
    ```

3. **Run the Symbolic Execution**  
    Execute the test using the following command:  
    ```bash
    java -Xmx1024m -ea -jar ../jpf-core/build/RunJPF.jar ./src/examples/sidlibrary/objectmodelpackage/LibReturnBook.jpf
    ```
The following analysis details the insights gained from the symbolic execution of these drivers.

## Symbolic Execution Analysis

## Overview of Instrumented Drivers
Symbolic drivers unify multiple behavioral scenarios using flag-controlled branches and bounded loops (e.g., limiting availability checks to small iteration counts). This design increases branch density while keeping path explosion manageable. The main drivers analyzed:
- `testAddBookUnified` (LibAddBook)
- `testAddStudentUnified` (LibAddStudent)
- `testIssueBookUnified` (LibIssueBook)
- `testReturnBookUnified` (LibReturnBook V1 & V2)
- `testIssueCardUnified` (LibIssueCard)
- `testRenewBookUnified` (LibRenewBook)
- `testReserveBookUnified` (LibReserveBook)
- `testSearchBookUnified` (LibSearchBook)

## LibAddBook (testAddBookUnified)
The symbolic execution of `testAddBookUnified` explores the logic for adding different types of books to a library.

- **Branch Coverage**: 
    - `testAddBookUnified`: **70% branch coverage (16/23 branches)**.
    - `addBookFunction`: **0% branch coverage (0/2 branches)**.
    - `checkBookAvailable`: **75% branch coverage (3/4 branches)**.
- **Covered Branches**: The execution successfully explored paths for adding `Book`, `Textbook`, `Novel`, `ReferenceBook`, and `EBook` objects, including the polymorphic behavior in `testAddDifferentBookTypes` and `testLibraryPolymorphism`.
- **Uncovered Branches**: The uncovered branches are primarily in the `addBookFunction` and `checkBookAvailable` methods, where conditions for a book already existing or the library being full were not fully explored.

## LibAddStudent (testAddStudentUnified)
The `testAddStudentUnified` driver focuses on adding various student types to the system.

- **Branch Coverage**: 
    - `testAddStudentUnified`: **72% branch coverage (18/25 branches)**.
    - `findStudent`: **0% branch coverage (0/2 branches)**.
- **Covered Branches**: The analysis covered adding `Student`, `Undergraduate`, `Graduate`, `PhDStudent`, and `ExchangeStudent`. It also covered the polymorphic behavior in `testStudentPolymorphism`.
- **Uncovered Branches**: The uncovered branches are in the `findStudent` method, specifically the paths where a student is not found.

## LibIssueBook (testIssueBookUnified)
This driver tests the logic for issuing a book to a student.

- **Branch Coverage**: 
    - `testIssueBookUnified`: **55% branch coverage (22/40 branches)**.
    - `checkBookAvailable`: **75% branch coverage (3/4 branches)**.
    - `findStudent`: **0% branch coverage (0/2 branches)**.
    - `issueLibraryCard`: **0% branch coverage (0/1 branches)**.
    - `maxBookStudentCanIssue`: **0% branch coverage (0/1 branches)**.
- **Covered Branches**: The execution covered scenarios where a book is available and the student is eligible to borrow it. It explored different student and book types.
- **Uncovered Branches**: Many branches remain uncovered, particularly those related to error conditions like:
    - Student not found.
    - Book not found.
    - Student has reached their borrowing limit.
    - Book is not available for borrowing.

## LibReturnBook Evolution (V1 → V2)
A comparative analysis of two versions of the `testReturnBookUnified` driver highlights significant improvements in test coverage and code structure.

| Metric          | V1 Analysis | V2 Analysis | Improvement |
|-----------------|-------------|-------------|-------------|
| Branch Coverage | 41% (11/27) | **88% (30/34)** | **+47%**    |
| Line Coverage   | 68% (95/140)| 94% (140/149)| +26%        |
| States Explored | 1110        | 1480        | +33%        |

- **V1 Weaknesses**: The initial version had low coverage, failing to explore critical paths related to fine calculation, different student types, and various book return scenarios.
- **V2 Improvements**: The second version introduced more detailed symbolic variables and flags, allowing SPF to explore a much wider range of scenarios, including different fine amounts and student-specific rules. This led to a dramatic increase in branch coverage. The branch coverage for called methods in V2 is:
    - `calculateFine`: **100% branch coverage (2/2 branches)**.
    - `returnBook`: **62% branch coverage (5/8 branches)**.
    - `findStudent`: **0% branch coverage (0/2 branches)**.

## LibIssueCard (testIssueCardUnified)
This driver tests issuing a library card to a student.

- **Branch Coverage**: 
    - `testIssueCardUnified`: **88% branch coverage (22/25 branches)**.
    - `findStudent`: **100% branch coverage (2/2 branches)**.
    - `issueLibraryCard`: **0% branch coverage (0/1 branches)**.
- **Covered Branches**: The execution successfully tested issuing cards to different student types and in different library types.
- **Uncovered Branches**: The few uncovered branches relate to the `issueLibraryCard` method, specifically the case where a student is not found.

## LibRenewBook (testRenewBookUnified)
This driver tests the book renewal logic.

- **Branch Coverage**: 
    - `testRenewBookUnified`: **52% branch coverage (17/33 branches)**.
    - `addBookFunction`: **0% branch coverage (0/2 branches)**.
    - `checkBookAvailable`: **75% branch coverage (3/4 branches)**.
    - `findStudent`: **0% branch coverage (0/2 branches)**.
    - `issueBook`: **0% branch coverage (0/5 branches)**.
    - `issueLibraryCard`: **0% branch coverage (0/1 branches)**.
- **Covered Branches**: The analysis covered basic renewal scenarios.
- **Uncovered Branches**: A significant number of branches were not covered, especially in the `issueBook` and `maxBookStudentCanIssue` methods which are part of the renewal logic. This indicates that the test driver is not adequately exercising the constraints related to issuing and renewing books.

## LibReserveBook (testReserveBookUnified)
This driver is for reserving a book.

- **Branch Coverage**: 
    - `testReserveBookUnified`: **76% branch coverage (19/25 branches)**.
    - `addBookFunction`: **0% branch coverage (0/2 branches)**.
    - `checkBookAvailable`: **50% branch coverage (2/4 branches)**.
- **Covered Branches**: The driver successfully tested reserving different book types.
- **Uncovered Branches**: The uncovered branches are mainly in the `addBookFunction` and `checkBookAvailable` methods, similar to `LibAddBook`.

## LibSearchBook (testSearchBookUnified)
This driver tests the book search functionality.

- **Branch Coverage**: 
    - `testSearchBookUnified`: **90% branch coverage (27/30 branches)**.
    - `addBookFunction`: **0% branch coverage (0/2 branches)**.
    - `checkBookAvailable`: **75% branch coverage (3/4 branches)**.
- **Covered Branches**: The execution successfully tested searching for various books by title and author, including cases where the book is found and not found.
- **Uncovered Branches**: The remaining uncovered branches are in the `addBookFunction`, which is used to populate the library for the test.

## Conclusion

This analysis demonstrates the power of Symbolic PathFinder (SPF) in rigorously testing the Java Library Management System. By replacing concrete inputs with symbolic variables, SPF automatically explored numerous execution paths, systematically testing the complex logic of the application.

The primary benefit of this approach was the ability to achieve high branch coverage. For each path it explored, SPF generated a set of constraints that were then solved to produce concrete test cases. These test cases effectively targeted specific conditions—such as different student types, book availability, and fine calculations—ensuring that a wide variety of scenarios were validated. The evolution from `LibReturnBook` V1 to V2 clearly showed how refining symbolic drivers can significantly increase this coverage.

In essence, symbolic execution served as both a powerful analysis tool and an automated test case generator, providing high confidence in the system's correctness by ensuring its critical logic paths were thoroughly exercised.

## Contributors
-   **Siddharth Palod (IMT2022002)**: Developed and symbolically tested the programs for renewing, reserving, returning, and searching books (`LibRenewBook.java`, `LibReserveBook.java`, `LibReturnBook.java`, `LibSearchBook.java`).
-   **Shreyas S (IMT2022078)**: Developed and symbolically tested the programs for adding books and students, and issuing books and library cards (`LibAddBook.java`, `LibAddStudent.java`, `LibIssueBook.java`, `LibIssueCard.java`).



