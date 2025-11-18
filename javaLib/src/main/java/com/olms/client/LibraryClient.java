package com.olms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;

/**
 * Console client that interoperates with {@link com.olms.server.LibraryServer}.
 */
public class LibraryClient {
    private final String host;
    private final int port;
    private int loggedInUserId = -1;

    public LibraryClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected: " + reader.readLine());
            while (true) {
                System.out.println("Select role: 1) Customer 2) Admin 3) Exit");
                String roleChoice = scanner.nextLine().trim();
                if ("3".equals(roleChoice)) {
                    writer.println("LOGOUT");
                    System.out.println("Bye!");
                    return;
                }
                boolean admin = "2".equals(roleChoice);
                if (!"1".equals(roleChoice) && !"2".equals(roleChoice)) {
                    System.out.println("Invalid option");
                    continue;
                }
                if (loginFlow(scanner, writer, reader, admin)) {
                    if (admin) {
                        adminMenu(scanner, writer, reader);
                    } else {
                        customerMenu(scanner, writer, reader);
                    }
                }
            }
        }
    }

    private boolean loginFlow(Scanner scanner, PrintWriter writer, BufferedReader reader, boolean admin) throws IOException {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        writer.printf("LOGIN|%s|%s|%s%n", admin ? "ADMIN" : "CUSTOMER", username, password);
        String response = reader.readLine();
        if (response != null && response.startsWith("OK|")) {
            String[] parts = response.split("\\|");
            loggedInUserId = Integer.parseInt(parts[1]);
            System.out.printf("Login successful. User id: %d%n", loggedInUserId);
            return true;
        }
        System.out.println("Login failed: " + response);
        System.out.print("Would you like to sign up? (y/n): ");
        String signup = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (signup.startsWith("y")) {
            writer.printf("SIGNUP|%s|%s|%s%n", admin ? "ADMIN" : "CUSTOMER", username, password);
            String signupResponse = reader.readLine();
            System.out.println(signupResponse);
        }
        return false;
    }

    private void customerMenu(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        boolean keepRunning = true;
        while (keepRunning) {
            System.out.println("Customer Menu:");
            System.out.println("a) View collection");
            System.out.println("b) Search by name");
            System.out.println("c) Rent a book");
            System.out.println("d) Return a book");
            System.out.println("e) Logout");
            String choice = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            switch (choice) {
                case "a":
                    listBooks(writer, reader);
                    break;
                case "b":
                    searchBook(scanner, writer, reader);
                    break;
                case "c":
                    rentBook(scanner, writer, reader);
                    break;
                case "d":
                    returnBook(scanner, writer, reader);
                    break;
                case "e":
                    keepRunning = false;
                    break;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    private void adminMenu(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        boolean keepRunning = true;
        while (keepRunning) {
            System.out.println("Admin Menu:");
            System.out.println("a) Add book");
            System.out.println("b) Delete book");
            System.out.println("c) Modify quantity");
            System.out.println("d) View collection");
            System.out.println("e) View users");
            System.out.println("f) Search book");
            System.out.println("g) Logout");
            String choice = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            switch (choice) {
                case "a":
                    addBook(scanner, writer, reader);
                    break;
                case "b":
                    deleteBook(scanner, writer, reader);
                    break;
                case "c":
                    updateQuantity(scanner, writer, reader);
                    break;
                case "d":
                    listBooks(writer, reader);
                    break;
                case "e":
                    listUsers(writer, reader);
                    break;
                case "f":
                    searchBook(scanner, writer, reader);
                    break;
                case "g":
                    keepRunning = false;
                    break;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    private void listBooks(PrintWriter writer, BufferedReader reader) throws IOException {
        writer.println("LIST_BOOKS");
        String header = reader.readLine();
        if (header == null || !header.startsWith("OK|BOOKS")) {
            System.out.println("Failed to fetch books: " + header);
            return;
        }
        String line;
        System.out.println("ID\tTitle\tQty");
        while ((line = reader.readLine()) != null && !"END".equals(line)) {
            if ("EMPTY".equals(line)) {
                System.out.println("[Empty]");
                break;
            }
            if (line.startsWith("BOOK|")) {
                String[] parts = line.split("\\|", 4);
                System.out.printf("%s\t%s\t%s%n", parts[1], parts[2], parts[3]);
            }
        }
    }

    private void listUsers(PrintWriter writer, BufferedReader reader) throws IOException {
        writer.println("LIST_USERS");
        String header = reader.readLine();
        if (header == null || !header.startsWith("OK|USERS")) {
            System.out.println("Failed to fetch users: " + header);
            return;
        }
        String line;
        System.out.println("ID\tUsername\tRole");
        while ((line = reader.readLine()) != null && !"END".equals(line)) {
            if ("EMPTY".equals(line)) {
                System.out.println("[Empty]");
                break;
            }
            if (line.startsWith("USER|")) {
                String[] parts = line.split("\\|", 4);
                System.out.printf("%s\t%s\t%s%n", parts[1], parts[2], parts[3]);
            }
        }
    }

    private void searchBook(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        System.out.print("Book title: ");
        String title = scanner.nextLine().trim();
        writer.printf("SEARCH_BOOK|%s%n", title);
        String response = reader.readLine();
        if (response != null && response.startsWith("OK|BOOK|NONE")) {
            System.out.println("Book not found");
        } else if (response != null && response.startsWith("OK|BOOK|")) {
            String[] parts = response.split("\\|", 5);
            System.out.printf("ID: %s, Title: %s, Quantity: %s%n", parts[2], parts[3], parts[4]);
        } else {
            System.out.println("Error: " + response);
        }
    }

    private void rentBook(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        int bookId = promptForInt(scanner, "Book ID: ");
        writer.printf("RENT_BOOK|%d%n", bookId);
        System.out.println(reader.readLine());
    }

    private void returnBook(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        int bookId = promptForInt(scanner, "Book ID: ");
        writer.printf("RETURN_BOOK|%d%n", bookId);
        System.out.println(reader.readLine());
    }

    private void addBook(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        int id = promptForInt(scanner, "Book ID: ");
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();
        int qty = promptForInt(scanner, "Quantity: ");
        writer.printf("ADD_BOOK|%d|%s|%d%n", id, title, qty);
        System.out.println(reader.readLine());
    }

    private void deleteBook(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        int id = promptForInt(scanner, "Book ID to delete: ");
        writer.printf("DELETE_BOOK|%d%n", id);
        System.out.println(reader.readLine());
    }

    private void updateQuantity(Scanner scanner, PrintWriter writer, BufferedReader reader) throws IOException {
        int id = promptForInt(scanner, "Book ID: ");
        int qty = promptForInt(scanner, "New quantity: ");
        writer.printf("UPDATE_QTY|%d|%d%n", id, qty);
        System.out.println(reader.readLine());
    }

    private int promptForInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        new LibraryClient(host, port).start();
    }
}

