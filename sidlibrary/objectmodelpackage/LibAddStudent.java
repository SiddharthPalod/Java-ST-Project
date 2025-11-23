// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.util.*;

public class LibAddStudent {
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;

    public static void main(String[] args) {
        testAddStudentUnified();
    }

    public static void testAddStudentUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int studentType = Debug.makeSymbolicInteger("studentType");
        
        int studentAlreadyExists = Debug.makeSymbolicInteger("studentAlreadyExists"); // 0=new student, 1=existing student
        int duplicateStudentId = Debug.makeSymbolicInteger("duplicateStudentId"); // 0=unique ID, 1=duplicate ID
        int validStudentData = Debug.makeSymbolicInteger("validStudentData"); // 0=invalid, 1=valid
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String studentId = Debug.makeSymbolicString("studentId");
        String studentName = Debug.makeSymbolicString("studentName");
        String studentProgram = Debug.makeSymbolicString("studentProgram");

        int studentYear = Debug.makeSymbolicInteger("studentYear");
        String studentMajor = Debug.makeSymbolicString("studentMajor");
        String researchArea = Debug.makeSymbolicString("researchArea");
        String advisor = Debug.makeSymbolicString("advisor");
        String dissertation = Debug.makeSymbolicString("dissertation");
        String homeUni = Debug.makeSymbolicString("homeUni");
        String exchangeProgram = Debug.makeSymbolicString("exchangeProgram");
        int duration = Debug.makeSymbolicInteger("duration");
        
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
        
        students = new ArrayList<>();
        
        // Setup: Add existing student if needed
        if (studentAlreadyExists == 1) {
            Student existingStudent = null;
            if (studentType == 0) {
                existingStudent = new Student(studentName, studentId, studentProgram);
            } else if (studentType == 1) {
                existingStudent = new Undergraduate(studentName, studentId, studentProgram, studentYear, studentMajor);
            } else if (studentType == 2) {
                existingStudent = new Graduate(studentName, studentId, studentProgram, researchArea, advisor);
            } else if (studentType == 3) {
                existingStudent = new PhDStudent(studentName, studentId, studentProgram, researchArea, advisor, dissertation);
            } else if (studentType == 4) {
                existingStudent = new ExchangeStudent(studentName, studentId, studentProgram, homeUni, exchangeProgram, duration);
            } else {
                existingStudent = new Student(studentName, studentId, studentProgram);
            }
            addStudents(existingStudent);
        }
        
        // MAIN TEST: Add student(s) to system
        Student student = null;
        String newStudentId = studentId;
        
        // Use duplicate ID if needed
        if (duplicateStudentId == 1 && studentAlreadyExists == 1) {
            newStudentId = studentId; // Same ID as existing
        }
        
        if (studentType == 0) {
            student = new Student(studentName, newStudentId, studentProgram);
        } else if (studentType == 1) {
            student = new Undergraduate(studentName, newStudentId, studentProgram, studentYear, studentMajor);
        } else if (studentType == 2) {
            student = new Graduate(studentName, newStudentId, studentProgram, researchArea, advisor);
        } else if (studentType == 3) {
            student = new PhDStudent(studentName, newStudentId, studentProgram, researchArea, advisor, dissertation);
        } else if (studentType == 4) {
            student = new ExchangeStudent(studentName, newStudentId, studentProgram, homeUni, exchangeProgram, duration);
        } else {
            student = new Student(studentName, newStudentId, studentProgram); // default
        }
        
        addStudents(student);
        
        // Verify student was added
        Student foundStudent = findStudent(newStudentId);
        if (foundStudent != null) {
            String foundId = foundStudent.getStudentId();
            String foundName = foundStudent.getStudentName();
            String foundProgram = foundStudent.getProgramEnrolledIn();
            String foundInstitute = foundStudent.getInstituteName();
        }
        
        // Test student methods
        if (student != null) {
            String id = student.getStudentId();
            String name = student.getStudentName();
            String program = student.getProgramEnrolledIn();
            String institute = student.getInstituteName();
            ArrayList<Book> issuedBooks = student.getIssuedBooks();
            int fine = student.getFine();
            
            // Test issueCard
            String instituteName = Debug.makeSymbolicString("instituteName");
            int cardResult = student.issueCard(instituteName);
            boolean hasAccess = student.hasLibraryAccess(instituteName);
            
            // Test hasLibraryAccess with COMSATS
            boolean hasComsatsAccess = student.hasLibraryAccess("COMSATS");
        }
        
        // Test polymorphic student methods
        testStudentPolymorphism(student);
        
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
        
        // Test Institute interface (Student extends Institute)
        if (student instanceof Institute) {
            Institute institute = (Institute) student;
            String memberId = Debug.makeSymbolicString("memberId");
            String memberType = Debug.makeSymbolicString("memberType");
            boolean valid = institute.validateMembership(memberId);
            int limit = institute.getMaxBorrowingLimit(memberType);
            java.util.List<String> services = institute.getAvailableServices();
            double fee = institute.calculateMembershipFee(memberType);
        }
        
        // Test polymorphic library methods
        testLibraryPolymorphism(library);
        
        // Test Institute interface for library
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

    public static Student findStudent(String studentId) {
        for (Student student : students) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
    }

    private static void addStudents(Student student) {
        student.addStudent();
        students.add(student);
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

