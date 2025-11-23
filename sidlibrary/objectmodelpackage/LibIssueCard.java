// SPDX-License-Identifier: MIT
package sidlibrary.objectmodelpackage;

import gov.nasa.jpf.symbc.Debug;
import java.util.*;

public class LibIssueCard {
    private static ArrayList<Student> students;
    private static Library comsatsLibrary;

    public static void main(String[] args) {
        testIssueCardUnified();
    }

    public static void testIssueCardUnified() {        
        int libraryType = Debug.makeSymbolicInteger("libraryType");        
        int studentType = Debug.makeSymbolicInteger("studentType");
        
        int hasStudent = Debug.makeSymbolicInteger("hasStudent"); // 0=no student, 1=has student
        int alreadyHasCard = Debug.makeSymbolicInteger("alreadyHasCard"); // 0=no card, 1=already has card
        int correctInstitute = Debug.makeSymbolicInteger("correctInstitute"); // 0=wrong institute, 1=COMSATS
        int validStudentId = Debug.makeSymbolicInteger("validStudentId"); // 0=invalid, 1=valid
        
        String libraryName = Debug.makeSymbolicString("libraryName");
        String studentId = Debug.makeSymbolicString("studentId");
        String studentName = Debug.makeSymbolicString("studentName");
        String studentProgram = Debug.makeSymbolicString("studentProgram");
        String instituteName = Debug.makeSymbolicString("instituteName");

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
            
            // Setup: Issue card beforehand if needed
            if (alreadyHasCard == 1) {
                issueLibraryCard(library, studentId);
            }
        }
        
        // MAIN TEST: Test library card issuance
        String testStudentId = studentId;
        if (validStudentId == 0 && hasStudent == 1) {
            testStudentId = Debug.makeSymbolicString("invalidStudentId");
        }
        
        String testInstituteName = "COMSATS";
        if (correctInstitute == 0) {
            testInstituteName = instituteName; // Use symbolic institute name
        }
        
        // Test issueCard
        Student targetStudent = findStudent(testStudentId);
        if (targetStudent != null) {
            int cardResult = targetStudent.issueCard(testInstituteName);
            
            // Test hasLibraryAccess before and after card issuance
            boolean hasAccessBefore = targetStudent.hasLibraryAccess("COMSATS");
            
            // Issue card again to test duplicate card scenario
            if (alreadyHasCard == 1 || cardResult == 1) {
                int duplicateResult = targetStudent.issueCard(testInstituteName);
            }
            
            // Test hasLibraryAccess after card issuance
            boolean hasAccessAfter = targetStudent.hasLibraryAccess("COMSATS");
            
            // Test with different institute names
            boolean hasAccessOther = targetStudent.hasLibraryAccess("OTHER_INSTITUTE");
        } else {
            // Test with non-existent student
            Student nonExistent = findStudent("NON_EXISTENT");
        }
        
        // Test issueCard with null student
        Student nullStudent = findStudent("NULL_STUDENT");
        if (nullStudent == null) {
            // This path tests when student is not found
        }
        
        // Test polymorphic student methods
        if (student != null) {
            testStudentPolymorphism(student);
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

