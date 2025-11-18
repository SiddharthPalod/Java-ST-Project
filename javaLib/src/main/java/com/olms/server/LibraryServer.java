package com.olms.server;

import com.olms.model.Book;
import com.olms.model.User;
import com.olms.storage.LibraryStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TCP server that mirrors the behaviour of the original C implementation but
 * uses a more expressive text protocol.
 */
public class LibraryServer {
    private final int port;
    private final LibraryStore store;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public LibraryServer(int port, Path dataDirectory) throws IOException {
        this.port = port;
        this.store = new LibraryStore(dataDirectory);
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("OLMS Java server listening on port %d%n", port);
            while (!clientPool.isShutdown()) {
                Socket socket = serverSocket.accept();
                clientPool.submit(new ClientHandler(socket, store));
            }
        } finally {
            shutdownPool();
        }
    }

    private void shutdownPool() {
        clientPool.shutdown();
        try {
            if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            clientPool.shutdownNow();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final LibraryStore store;
        private User currentUser;

        ClientHandler(Socket socket, LibraryStore store) {
            this.socket = socket;
            this.store = store;
        }

        @Override
        public void run() {
            System.out.printf("Client connected from %s%n", socket.getRemoteSocketAddress());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.println("OK|CONNECTED");
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    try {
                        boolean shouldClose = handleCommand(line, writer);
                        if (shouldClose) {
                            break;
                        }
                    } catch (Exception e) {
                        writer.printf("ERROR|%s%n", e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.printf("Client communication error: %s%n", e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                System.out.printf("Client disconnected from %s%n", socket.getRemoteSocketAddress());
            }
        }

        private boolean handleCommand(String line, PrintWriter writer) throws IOException {
            String[] parts = line.split("\\|");
            String command = parts[0].toUpperCase(Locale.ROOT);
            switch (command) {
                case "PING":
                    writer.println("OK|PONG");
                    break;
                case "LOGIN":
                    handleLogin(parts, writer);
                    break;
                case "SIGNUP":
                    handleSignup(parts, writer);
                    break;
                case "LIST_BOOKS":
                    requireLogin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            sendBookList(writer);
                        }
                    }, writer);
                    break;
                case "SEARCH_BOOK":
                    requireLogin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            searchBook(parts, writer);
                        }
                    }, writer);
                    break;
                case "RENT_BOOK":
                    requireCustomer(new Action() {
                        @Override
                        public void execute() throws IOException {
                            rentBook(parts, writer);
                        }
                    }, writer);
                    break;
                case "RETURN_BOOK":
                    requireCustomer(new Action() {
                        @Override
                        public void execute() throws IOException {
                            returnBook(parts, writer);
                        }
                    }, writer);
                    break;
                case "ADD_BOOK":
                    requireAdmin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            addBook(parts, writer);
                        }
                    }, writer);
                    break;
                case "DELETE_BOOK":
                    requireAdmin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            deleteBook(parts, writer);
                        }
                    }, writer);
                    break;
                case "UPDATE_QTY":
                    requireAdmin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            updateQuantity(parts, writer);
                        }
                    }, writer);
                    break;
                case "LIST_USERS":
                    requireAdmin(new Action() {
                        @Override
                        public void execute() throws IOException {
                            sendUsers(writer);
                        }
                    }, writer);
                    break;
                case "LOGOUT":
                    writer.println("OK|BYE");
                    return true;
                default:
                    writer.println("ERROR|Unknown command");
            }
            return false;
        }

        private void requireLogin(Action action, PrintWriter writer) throws IOException {
            if (currentUser == null) {
                writer.println("ERROR|Login required");
                return;
            }
            action.execute();
        }

        private void requireCustomer(Action action, PrintWriter writer) throws IOException {
            if (currentUser == null || currentUser.isAdmin()) {
                writer.println("ERROR|Customer privileges required");
                return;
            }
            action.execute();
        }

        private void requireAdmin(Action action, PrintWriter writer) throws IOException {
            if (currentUser == null || !currentUser.isAdmin()) {
                writer.println("ERROR|Admin privileges required");
                return;
            }
            action.execute();
        }

        private void handleLogin(String[] parts, PrintWriter writer) {
            if (parts.length < 4) {
                writer.println("ERROR|LOGIN|ROLE|USERNAME|PASSWORD");
                return;
            }
            boolean admin = "ADMIN".equalsIgnoreCase(parts[1]);
            String username = parts[2];
            String password = parts[3];
            Optional<User> userOpt = store.authenticate(username, password, admin);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                writer.printf("OK|%d|%s|%s%n", currentUser.getId(), currentUser.getUsername(),
                        currentUser.isAdmin() ? "ADMIN" : "CUSTOMER");
            } else {
                writer.println("ERROR|Invalid credentials");
            }
        }

        private void handleSignup(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 4) {
                writer.println("ERROR|SIGNUP|ROLE|USERNAME|PASSWORD");
                return;
            }
            boolean admin = "ADMIN".equalsIgnoreCase(parts[1]);
            String username = parts[2];
            String password = parts[3];
            User user = store.register(username, password, admin);
            writer.printf("OK|REGISTERED|%d%n", user.getId());
        }

        private void sendBookList(PrintWriter writer) {
            writer.println("OK|BOOKS");
            List<Book> books = store.getAllBooks();
            if (books.isEmpty()) {
                writer.println("EMPTY");
            } else {
                for (Book book : books) {
                    writer.printf("BOOK|%d|%s|%d%n", book.getId(), book.getTitle(), book.getQuantity());
                }
            }
            writer.println("END");
        }

        private void searchBook(String[] parts, PrintWriter writer) {
            if (parts.length < 2) {
                writer.println("ERROR|SEARCH_BOOK|TITLE");
                return;
            }
            String title = parts[1];
            Optional<Book> bookOpt = store.findBookByTitle(title);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                writer.printf("OK|BOOK|%d|%s|%d%n", book.getId(), book.getTitle(), book.getQuantity());
            } else {
                writer.println("OK|BOOK|NONE");
            }
        }

        private void rentBook(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 2) {
                writer.println("ERROR|RENT_BOOK|BOOK_ID");
                return;
            }
            int bookId = Integer.parseInt(parts[1]);
            String status = store.rentBook(bookId, currentUser.getId());
            writer.printf("OK|%s%n", status);
        }

        private void returnBook(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 2) {
                writer.println("ERROR|RETURN_BOOK|BOOK_ID");
                return;
            }
            int bookId = Integer.parseInt(parts[1]);
            String status = store.returnBook(bookId, currentUser.getId());
            writer.printf("OK|%s%n", status);
        }

        private void addBook(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 4) {
                writer.println("ERROR|ADD_BOOK|ID|TITLE|QTY");
                return;
            }
            int id = Integer.parseInt(parts[1]);
            String title = parts[2];
            int qty = Integer.parseInt(parts[3]);
            store.addBook(new Book(id, title, qty));
            writer.println("OK|BOOK_ADDED");
        }

        private void deleteBook(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 2) {
                writer.println("ERROR|DELETE_BOOK|ID");
                return;
            }
            int id = Integer.parseInt(parts[1]);
            boolean deleted = store.deleteBook(id);
            if (deleted) {
                writer.println("OK|BOOK_DELETED");
            } else {
                writer.println("ERROR|Book not found");
            }
        }

        private void updateQuantity(String[] parts, PrintWriter writer) throws IOException {
            if (parts.length < 3) {
                writer.println("ERROR|UPDATE_QTY|ID|QTY");
                return;
            }
            int id = Integer.parseInt(parts[1]);
            int qty = Integer.parseInt(parts[2]);
            boolean updated = store.updateQuantity(id, qty);
            if (updated) {
                writer.println("OK|BOOK_UPDATED");
            } else {
                writer.println("ERROR|Book not found");
            }
        }

        private void sendUsers(PrintWriter writer) {
            writer.println("OK|USERS");
            List<User> users = store.getAllCustomers();
            if (users.isEmpty()) {
                writer.println("EMPTY");
            } else {
                for (User user : users) {
                    writer.printf("USER|%d|%s|%s%n", user.getId(), user.getUsername(),
                            user.isAdmin() ? "ADMIN" : "CUSTOMER");
                }
            }
            writer.println("END");
        }
    }

    @FunctionalInterface
    private interface Action {
        void execute() throws IOException;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path dataDir = Paths.get(args.length > 1 ? args[1] : "Java Version/data");
        LibraryServer server = new LibraryServer(port, dataDir);
        server.start();
    }
}

