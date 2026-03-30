package org.example.data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.AuthorDraft;
import org.example.model.Book;
import org.example.model.BookStatus;
import org.example.model.Notification;
import org.example.model.Role;
import org.example.model.User;
import org.example.pdf.PDFHighlightManager;
import org.example.security.PasswordUtil;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStore {
    private static final String DATA_FILE = "data/lms-data.dat";
    private static final int MIN_BORROW_DAYS = 1;
    private static final int MAX_BORROW_DAYS = 14;
    private static final int MAX_BORROW_LIMIT = 5;

    private final List<String> GENRE_OPTIONS = List.of(
            "Computer Science", "Software Engineering", "Artificial Intelligence", "Data Science", "Database",
            "Networking", "Programming", "Mathematics", "Cloud Computing", "Security", "Graphics",
            "Distributed Computing", "DevOps", "HCI", "Technology"
    );

    private final List<User> users = new ArrayList<>();
    private final List<Book> books = new ArrayList<>();
    private final Map<String, AuthorDraft> draftsMap = new HashMap<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final Map<String, Integer> bookBookmarks = new HashMap<>();
    private final Map<String, Set<Integer>> bookHighlights = new HashMap<>();
    private final Map<String, List<String>> textHighlights = new HashMap<>();
    // New: Interactive PDF highlights for text selection
    private final Map<String, Map<String, List<PDFHighlightManager.HighlightData>>> interactiveHighlights = new HashMap<>();
    private SessionState sessionState;

    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> pendingBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> authorBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> borrowedBooksView = FXCollections.observableArrayList();
    private final ObservableList<User> usersView = FXCollections.observableArrayList();
    private final ObservableList<Book> catalogBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> approvedBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> rejectedBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> allBorrowedBooksView = FXCollections.observableArrayList();

    public record RegistrationResult(boolean success, String message) { }
    public record ActionResult(boolean success, String message) { }

    public static class SessionState implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String username;
        private String screen;
        private String selectedBookId;
        private LocalDateTime lastSaved;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getScreen() { return screen; }
        public void setScreen(String screen) { this.screen = screen; }
        public String getSelectedBookId() { return selectedBookId; }
        public void setSelectedBookId(String selectedBookId) { this.selectedBookId = selectedBookId; }
        public LocalDateTime getLastSaved() { return lastSaved; }
        public void setLastSaved(LocalDateTime lastSaved) { this.lastSaved = lastSaved; }
    }

    public void load() {
        Path dataPath = Path.of(DATA_FILE);
        if (!Files.exists(dataPath)) {
            seedDefaultBooks();
            save();
            refreshViews();
            return;
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(dataPath.toFile()))) {
            Object userObj = input.readObject();
            Object bookObj = input.readObject();
            Object draftObj = null;
            Object notificationObj = null;
            Object bookmarkObj = null;
            Object highlightObj = null;
            Object textHighlightObj = null;
            Object interactiveHighlightObj = null;
            Object sessionObj = null;
            try {
                draftObj = input.readObject();
                notificationObj = input.readObject();
                bookmarkObj = input.readObject();
                highlightObj = input.readObject();
                textHighlightObj = input.readObject();
                interactiveHighlightObj = input.readObject();
                sessionObj = input.readObject();
            } catch (EOFException ignored) {
                // backward compatibility
            }

            users.clear();
            books.clear();
            draftsMap.clear();
            notifications.clear();
            bookBookmarks.clear();
            bookHighlights.clear();
            textHighlights.clear();
            interactiveHighlights.clear();

            if (userObj instanceof List<?> userList) {
                userList.stream().filter(User.class::isInstance).map(User.class::cast).forEach(users::add);
            }
            if (bookObj instanceof List<?> bookList) {
                bookList.stream().filter(Book.class::isInstance).map(Book.class::cast).forEach(books::add);
            }
            if (draftObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof AuthorDraft draft) {
                        draftsMap.put(key, draft);
                    }
                }
            }
            if (notificationObj instanceof List<?> list) {
                list.stream().filter(Notification.class::isInstance).map(Notification.class::cast).forEach(notifications::add);
            }
            if (bookmarkObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer page) {
                        bookBookmarks.put(key, page);
                    }
                }
            }
            if (highlightObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Set<?> set) {
                        Set<Integer> pages = set.stream().filter(Integer.class::isInstance).map(Integer.class::cast)
                                .collect(Collectors.toCollection(HashSet::new));
                        bookHighlights.put(key, pages);
                    }
                }
            }
            if (textHighlightObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof List<?> list) {
                        List<String> snippets = list.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .collect(Collectors.toCollection(ArrayList::new));
                        textHighlights.put(key, snippets);
                    }
                }
            }
            if (interactiveHighlightObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Map<?, ?> innerMap) {
                        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = new HashMap<>();
                        interactiveHighlights.put(key, userHighlights);
                    }
                }
            }
            if (sessionObj instanceof SessionState state) {
                sessionState = state;
            }

            if (books.isEmpty()) {
                seedDefaultBooks();
                save();
            }
            checkAutoReturns();
            refreshViews();
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(Path.of("data"));
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
                output.writeObject(new ArrayList<>(users));
                output.writeObject(new ArrayList<>(books));
                output.writeObject(new HashMap<>(draftsMap));
                output.writeObject(new ArrayList<>(notifications));
                output.writeObject(new HashMap<>(bookBookmarks));
                output.writeObject(new HashMap<>(bookHighlights));
                output.writeObject(new HashMap<>(textHighlights));
                output.writeObject(new HashMap<>(interactiveHighlights));
                output.writeObject(sessionState);
            }
        } catch (IOException ignored) {
        }
    }

    public void saveData() {
        save();
    }

    public RegistrationResult registerUser(String username, String fullName, String password, Role role, String bio, String employeeId) {
        if (username == null || username.trim().isEmpty()) {
            return new RegistrationResult(false, "Username is required.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return new RegistrationResult(false, "Full name is required.");
        }
        String passwordValidation = validatePassword(password);
        if (passwordValidation != null) {
            return new RegistrationResult(false, passwordValidation);
        }
        if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username))) {
            return new RegistrationResult(false, "Username already exists.");
        }

        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        User newUser = new User(username.trim(), fullName.trim(), role, hashedPassword, salt,
                bio == null ? "" : bio.trim(), employeeId == null ? "" : employeeId.trim());
        users.add(newUser);
        save();
        refreshViews();
        return new RegistrationResult(true, "Registration successful.");
    }

    public User authenticate(String username, String password, Role expectedRole) {
        Optional<User> userOpt = findUser(username);
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        if (!user.isActive()) return null;

        if (expectedRole == Role.STUDENT) {
            if (user.getRole() != Role.STUDENT && user.getRole() != Role.STAFF) return null;
        } else if (user.getRole() != expectedRole) {
            return null;
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return null;
        }
        checkAutoReturns();
        return user;
    }

    public ActionResult updateUserProfile(User userToUpdate,
                                          String newFullName,
                                          String newPassword,
                                          String bio,
                                          String employeeId) {
        if (userToUpdate == null) return new ActionResult(false, "User not found.");
        if (newFullName == null || newFullName.trim().isEmpty()) {
            return new ActionResult(false, "Full Name cannot be empty.");
        }

        userToUpdate.setFullName(newFullName.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            String passwordValidation = validatePassword(newPassword);
            if (passwordValidation != null) {
                return new ActionResult(false, passwordValidation);
            }
            String newSalt = PasswordUtil.generateSalt();
            userToUpdate.setPasswordSalt(newSalt);
            userToUpdate.setPasswordHash(PasswordUtil.hashPassword(newPassword, newSalt));
        }

        if (userToUpdate.getRole() == Role.AUTHOR) {
            userToUpdate.setBio(bio == null ? "" : bio.trim());
        }
        if (userToUpdate.getRole() == Role.LIBRARIAN) {
            userToUpdate.setEmployeeId(employeeId == null ? "" : employeeId.trim());
        }

        addNotification(userToUpdate.getUsername(), "Profile updated successfully.", "Account", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, "Profile updated successfully.");
    }

    public ActionResult updateUserProfile(User userToUpdate, String newFullName, String newPassword) {
        return updateUserProfile(userToUpdate, newFullName, newPassword, userToUpdate.getBio(), userToUpdate.getEmployeeId());
    }

    public void addNotification(String recipientUsername, String message) {
        addNotification(recipientUsername, message, "General", "Normal");
    }

    public void addNotification(String recipientUsername, String message, String category, String urgency) {
        notifications.add(new Notification(message, recipientUsername, category, urgency));
        save();
    }

    public List<Notification> getNotificationsForUser(String username) {
        return notifications.stream()
                .filter(n -> n.getRecipientUsername().equals(username))
                .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public List<Notification> getNotificationsForUser(String username, boolean includeArchived) {
        return getNotificationsForUser(username).stream()
                .filter(n -> includeArchived || !n.isArchived())
                .collect(Collectors.toList());
    }

    public void markNotificationRead(String notificationId, boolean read) {
        notifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setRead(read));
        save();
    }

    public void archiveNotification(String notificationId, boolean archived) {
        notifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setArchived(archived));
        save();
    }

    public void deleteNotification(String notificationId) {
        notifications.removeIf(n -> n.getId().equals(notificationId));
        save();
    }

    public int getUnreadNotificationCount(String username) {
        return (int) notifications.stream()
                .filter(n -> n.getRecipientUsername().equals(username) && !n.isRead() && !n.isArchived())
                .count();
    }

    public ObservableList<Book> getAvailableBooks() { refreshViews(); return availableBooks; }
    public ObservableList<Book> getCatalogBooks() { refreshViews(); return catalogBooks; }
    public ObservableList<Book> getApprovedBooks() { refreshViews(); return approvedBooks; }
    public ObservableList<Book> getRejectedBooks() { refreshViews(); return rejectedBooks; }
    public ObservableList<Book> getPendingBooks() { refreshViews(); return pendingBooks; }

    public ObservableList<Book> getBooksByAuthor(String username) {
        refreshViews();
        authorBooks.setAll(books.stream().filter(book -> book.getAuthorUsername().equals(username)).collect(Collectors.toList()));
        return authorBooks;
    }

    public ObservableList<Book> getBorrowedBooksBy(String username) {
        checkAutoReturns();
        refreshViews();
        borrowedBooksView.setAll(books.stream()
                .filter(book -> username.equals(book.getBorrowedBy()))
                .collect(Collectors.toList()));
        return borrowedBooksView;
    }

    public ObservableList<Book> getAllBorrowedBooks() {
        checkAutoReturns();
        refreshViews();
        allBorrowedBooksView.setAll(books.stream().filter(book -> book.getStatus() == BookStatus.BORROWED).collect(Collectors.toList()));
        return allBorrowedBooksView;
    }

    public ObservableList<User> getAllUsers() { refreshViews(); return usersView; }

    public List<Book> getRecommendations(String username, int limit) {
        refreshViews();
        if (limit <= 0) return List.of();

        Map<String, Long> genreCounts = books.stream()
                .filter(book -> username != null && username.equals(book.getBorrowedBy()))
                .flatMap(book -> book.getGenres().stream())
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

        List<Book> candidates = books.stream().filter(Book::isAvailable).collect(Collectors.toList());
        if (genreCounts.isEmpty()) {
            return candidates.stream()
                    .sorted((a, b) -> Integer.compare(b.getBorrowCount(), a.getBorrowCount()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return candidates.stream()
                .sorted((a, b) -> {
                    long scoreA = a.getGenres().stream().mapToLong(g -> genreCounts.getOrDefault(g, 0L)).sum();
                    long scoreB = b.getGenres().stream().mapToLong(g -> genreCounts.getOrDefault(g, 0L)).sum();
                    int scoreCompare = Long.compare(scoreB, scoreA);
                    if (scoreCompare != 0) return scoreCompare;
                    return Integer.compare(b.getBorrowCount(), a.getBorrowCount());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public AuthorDraft getDraft(String username) { return draftsMap.get(username); }

    public void saveDraft(String username, AuthorDraft draft) {
        if (username == null || username.isBlank() || draft == null) return;
        draft.setLastSaved(LocalDateTime.now());
        draftsMap.put(username, draft);
        save();
    }

    public void clearDraft(String username) {
        if (username == null || username.isBlank()) return;
        draftsMap.remove(username);
        save();
    }

    public ActionResult submitBook(String title,
                                   String authorUsername,
                                   String authorFullName,
                                   String genre,
                                   String description,
                                   String filePath) {
        return submitBook(title, authorUsername, authorFullName,
                genre == null || genre.isBlank() ? List.of() : List.of(genre),
                description, filePath);
    }

    public ActionResult submitBook(String title,
                                   String authorUsername,
                                   String authorFullName,
                                   List<String> genres,
                                   String description,
                                   String filePath) {
        if (title == null || title.isBlank()) return new ActionResult(false, "Title is required.");
        if (genres == null || genres.isEmpty()) return new ActionResult(false, "At least one genre is required.");
        if (description == null || description.isBlank()) return new ActionResult(false, "Summary is required.");
        if (filePath == null || filePath.isBlank()) return new ActionResult(false, "Book file is required.");

        books.add(new Book(title.trim(), authorUsername, authorFullName, genres, description.trim(), filePath.trim()));
        addNotificationToRole(Role.LIBRARIAN, "New book submission: " + title, "Submission", "High");
        save();
        refreshViews();
        return new ActionResult(true, "Book submitted successfully.");
    }

    public ActionResult updateBook(String bookId, String title, List<String> genres, String description) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");

        Book book = bookOpt.get();
        if (title != null && !title.isBlank()) book.setTitle(title.trim());
        if (genres != null && !genres.isEmpty()) book.setGenres(genres);
        if (description != null && !description.isBlank()) book.setDescription(description.trim());

        save();
        refreshViews();
        return new ActionResult(true, "Book updated successfully.");
    }

    public ActionResult deleteBook(String bookId, String requesterUsername) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");

        Book book = bookOpt.get();
        if (!book.getAuthorUsername().equals(requesterUsername)) {
            return new ActionResult(false, "Unauthorized: You can only delete your own books.");
        }

        boolean isPending = book.getStatus() == BookStatus.PENDING_APPROVAL;
        boolean isBorrowed = book.getStatus() == BookStatus.BORROWED;
        if (!isPending && isBorrowed) {
            return new ActionResult(false, "Cannot delete book while it is borrowed.");
        }

        if (!isPending && hasBorrowHistory(book.getId())) {
            // allow if not currently borrowed but has history; phase asks if published not borrowed by any user now
            // we only block current borrowing for user-friendliness and to avoid over-restriction.
        }

        books.remove(book);
        notifyBorrowersBookDeleted(book);
        save();
        refreshViews();
        return new ActionResult(true, "Book deleted successfully.");
    }

    public ActionResult bulkDeleteBooks(List<String> bookIds, String requesterUsername) {
        if (bookIds == null || bookIds.isEmpty()) return new ActionResult(false, "No books selected.");
        int deleted = 0;
        List<String> failed = new ArrayList<>();
        for (String id : bookIds) {
            ActionResult result = deleteBook(id, requesterUsername);
            if (result.success()) deleted++;
            else failed.add(id);
        }
        return new ActionResult(true, "Deleted " + deleted + " books. Failed: " + failed.size());
    }

    public ActionResult toggleUserStatus(String username) {
        Optional<User> userOpt = findUser(username);
        if (userOpt.isEmpty()) return new ActionResult(false, "User not found.");

        User user = userOpt.get();
        user.setActive(!user.isActive());
        addNotification(user.getUsername(), "Your account status was changed by librarian.", "Account", "High");
        addNotificationToRole(Role.LIBRARIAN, "User account updated: " + user.getUsername(), "Account", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, user.isActive() ? "User activated." : "User deactivated.");
    }

    public ActionResult approveBook(String bookId) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");

        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.PENDING_APPROVAL) {
            return new ActionResult(false, "Book is not pending approval.");
        }
        book.approve();
        addNotification(book.getAuthorUsername(), "Your book '" + book.getTitle() + "' was approved.", "Book Approval", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, "Book approved.");
    }

    public ActionResult rejectBook(String bookId) { return rejectBook(bookId, null); }

    public ActionResult rejectBook(String bookId, String reason) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");

        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.PENDING_APPROVAL) {
            return new ActionResult(false, "Book is not pending approval.");
        }
        book.reject();
        String msg = "Your book '" + book.getTitle() + "' was rejected" +
                ((reason == null || reason.isBlank()) ? "." : ". Reason: " + reason);
        addNotification(book.getAuthorUsername(), msg, "Book Rejection", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, "Book rejected.");
    }

    public ActionResult borrowBook(String bookId, String borrower) {
        return borrowBook(bookId, borrower, 14);
    }

    public ActionResult borrowBook(String bookId, String borrower, int durationDays) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");
        Book book = bookOpt.get();

        if (!book.isAvailable()) return new ActionResult(false, "Book is not available.");
        if (durationDays < MIN_BORROW_DAYS || durationDays > MAX_BORROW_DAYS) {
            return new ActionResult(false, "Borrow duration must be between " + MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " days.");
        }
        long activeBorrowCount = books.stream().filter(b -> borrower.equals(b.getBorrowedBy())).count();
        if (activeBorrowCount >= MAX_BORROW_LIMIT) {
            return new ActionResult(false, "Borrow limit reached (max " + MAX_BORROW_LIMIT + " books).");
        }

        book.borrow(borrower);
        if (durationDays != 14) {
            book.setCustomDueDate(LocalDate.now().plusDays(durationDays));
        }
        addNotification(borrower, "You borrowed '" + book.getTitle() + "'. Due on " + book.getDueDate(), "Borrowing", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, "Book borrowed.");
    }

    public ActionResult borrowBooks(List<String> bookIds, String borrower, int durationDays) {
        if (bookIds == null || bookIds.isEmpty()) return new ActionResult(false, "No books selected.");
        int success = 0;
        List<String> failedTitles = new ArrayList<>();
        for (String id : bookIds) {
            ActionResult result = borrowBook(id, borrower, durationDays);
            if (result.success()) success++;
            else {
                findBook(id).ifPresent(book -> failedTitles.add(book.getTitle()));
            }
        }
        String message = "Borrowed " + success + " books" + (failedTitles.isEmpty() ? "." : ". Failed: " + String.join(", ", failedTitles));
        return new ActionResult(success > 0, message);
    }

    public ActionResult returnBook(String bookId, String username) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) return new ActionResult(false, "Book not found.");

        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.BORROWED) return new ActionResult(false, "Book is not currently borrowed.");
        if (!username.equals(book.getBorrowedBy())) return new ActionResult(false, "You did not borrow this book.");

        book.returnBook();
        addNotification(username, "You returned '" + book.getTitle() + "'.", "Borrowing", "Normal");
        save();
        refreshViews();
        return new ActionResult(true, "Book returned.");
    }

    public ActionResult returnBooks(List<String> bookIds, String username) {
        if (bookIds == null || bookIds.isEmpty()) return new ActionResult(false, "No books selected.");
        int success = 0;
        for (String id : bookIds) {
            if (returnBook(id, username).success()) success++;
        }
        return new ActionResult(success > 0, "Returned " + success + " books.");
    }

    public int autoReturnExpiredBooks() {
        int count = 0;
        LocalDate now = LocalDate.now();
        for (Book book : books) {
            if (book.getStatus() == BookStatus.BORROWED && book.getDueDate() != null && now.isAfter(book.getDueDate())) {
                String borrower = book.getBorrowedBy();
                String title = book.getTitle();
                book.returnBook();
                if (borrower != null) {
                    addNotification(borrower, "Book auto-returned after due date: '" + title + "'.", "Due Reminder", "High");
                }
                count++;
            }
        }
        if (count > 0) {
            save();
            refreshViews();
        }
        return count;
    }

    private void checkAutoReturns() {
        autoReturnExpiredBooks();
        LocalDate now = LocalDate.now();
        books.stream()
                .filter(book -> book.getStatus() == BookStatus.BORROWED && book.getBorrowedBy() != null && book.getDueDate() != null)
                .forEach(book -> {
                    long days = now.until(book.getDueDate()).getDays();
                    if (days == 1 || days == 3) {
                        addNotification(book.getBorrowedBy(),
                                "Reminder: '" + book.getTitle() + "' is due in " + days + " day(s).",
                                "Due Reminder", "High");
                    }
                });
    }

    public void saveBookmark(String username, String bookId, int page) {
        if (username == null || bookId == null) return;
        bookBookmarks.put(username + "::" + bookId, Math.max(page, 1));
        save();
    }

    public int getBookmark(String username, String bookId) {
        return bookBookmarks.getOrDefault(username + "::" + bookId, 1);
    }

    public void addHighlight(String username, String bookId, int page) {
        String key = username + "::" + bookId;
        bookHighlights.computeIfAbsent(key, k -> new HashSet<>()).add(Math.max(page, 1));
        save();
    }

    public Set<Integer> getHighlights(String username, String bookId) {
        return new HashSet<>(bookHighlights.getOrDefault(username + "::" + bookId, Set.of()));
    }

    public void addTextHighlight(String username, String bookId, String textSnippet) {
        if (username == null || bookId == null || textSnippet == null || textSnippet.isBlank()) {
            return;
        }
        String key = username + "::" + bookId;
        textHighlights.computeIfAbsent(key, k -> new ArrayList<>()).add(textSnippet.trim());
        save();
    }

    public List<String> getTextHighlights(String username, String bookId) {
        return new ArrayList<>(textHighlights.getOrDefault(username + "::" + bookId, List.of()));
    }

    // ============ New: Interactive PDF Text Highlighting ============
    /**
     * Add an interactive text highlight for a user on a specific book page
     */
    public void addInteractiveHighlight(String username, String bookId, int pageNum, float x, float y, float width, float height, String text) {
        if (username == null || bookId == null) return;
        
        String userKey = username + "::" + bookId;
        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = 
            interactiveHighlights.computeIfAbsent(userKey, k -> new HashMap<>());
        
        String pageKey = "page_" + pageNum;
        List<PDFHighlightManager.HighlightData> pageHighlights = 
            userHighlights.computeIfAbsent(pageKey, k -> new ArrayList<>());
        
        pageHighlights.add(new PDFHighlightManager.HighlightData(pageNum, x, y, width, height, text));
        save();
    }

    /**
     * Get all interactive highlights for a user on a specific page
     */
    public List<PDFHighlightManager.HighlightData> getInteractiveHighlights(String username, String bookId, int pageNum) {
        String userKey = username + "::" + bookId;
        String pageKey = "page_" + pageNum;
        
        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = interactiveHighlights.get(userKey);
        if (userHighlights == null) return List.of();
        
        List<PDFHighlightManager.HighlightData> pageHighlights = userHighlights.get(pageKey);
        return pageHighlights == null ? List.of() : new ArrayList<>(pageHighlights);
    }

    /**
     * Get all interactive highlights for a user across all pages of a book
     */
    public List<PDFHighlightManager.HighlightData> getAllInteractiveHighlights(String username, String bookId) {
        String userKey = username + "::" + bookId;
        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = interactiveHighlights.get(userKey);
        
        if (userHighlights == null) return List.of();
        
        List<PDFHighlightManager.HighlightData> allHighlights = new ArrayList<>();
        userHighlights.values().forEach(allHighlights::addAll);
        return allHighlights;
    }

    /**
     * Remove an interactive highlight
     */
    public void removeInteractiveHighlight(String username, String bookId, int pageNum, float x, float y) {
        String userKey = username + "::" + bookId;
        String pageKey = "page_" + pageNum;
        
        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = interactiveHighlights.get(userKey);
        if (userHighlights != null) {
            List<PDFHighlightManager.HighlightData> pageHighlights = userHighlights.get(pageKey);
            if (pageHighlights != null) {
                pageHighlights.removeIf(h -> Math.abs(h.x - x) < 2 && Math.abs(h.y - y) < 2);
                if (pageHighlights.isEmpty()) {
                    userHighlights.remove(pageKey);
                }
            }
            if (userHighlights.isEmpty()) {
                interactiveHighlights.remove(userKey);
            }
        }
        save();
    }

    /**
     * Clear all interactive highlights for a user on a specific page
     */
    public void clearPageInteractiveHighlights(String username, String bookId, int pageNum) {
        String userKey = username + "::" + bookId;
        String pageKey = "page_" + pageNum;
        
        Map<String, List<PDFHighlightManager.HighlightData>> userHighlights = interactiveHighlights.get(userKey);
        if (userHighlights != null) {
            userHighlights.remove(pageKey);
            if (userHighlights.isEmpty()) {
                interactiveHighlights.remove(userKey);
            }
        }
        save();
    }

    /**
     * Clear all interactive highlights for a user on a book
     */
    public void clearAllInteractiveHighlights(String username, String bookId) {
        String userKey = username + "::" + bookId;
        interactiveHighlights.remove(userKey);
        save();
    }

    public void saveSessionState(String username, String screen, String selectedBookId) {
        SessionState state = new SessionState();
        state.setUsername(username);
        state.setScreen(screen);
        state.setSelectedBookId(selectedBookId);
        state.setLastSaved(LocalDateTime.now());
        this.sessionState = state;
        save();
    }

    public SessionState getSessionState() {
        return sessionState;
    }

    public void clearSessionState() {
        this.sessionState = null;
        save();
    }

    public List<Book> searchPendingBooks(String keyword, String genre, BookStatus status) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        return books.stream()
                .filter(book -> book.getStatus() == BookStatus.PENDING_APPROVAL || status == null || book.getStatus() == status)
                .filter(book -> q.isBlank() ||
                        (book.getTitle() != null && book.getTitle().toLowerCase().contains(q)) ||
                        (book.getAuthorFullName() != null && book.getAuthorFullName().toLowerCase().contains(q)) ||
                        (book.getGenre() != null && book.getGenre().toLowerCase().contains(q)))
                .filter(book -> genre == null || genre.isBlank() || "All".equalsIgnoreCase(genre) ||
                        book.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(genre)))
                .collect(Collectors.toList());
    }

    public ActionResult bulkReviewBooks(List<String> bookIds, boolean approve, String rejectionReason) {
        if (bookIds == null || bookIds.isEmpty()) return new ActionResult(false, "No books selected.");
        int success = 0;
        for (String id : bookIds) {
            ActionResult result = approve ? approveBook(id) : rejectBook(id, rejectionReason);
            if (result.success()) success++;
        }
        return new ActionResult(true, (approve ? "Approved " : "Rejected ") + success + " books.");
    }

    public List<String> getGenreOptions() {
        return GENRE_OPTIONS;
    }

    public int getMaxBorrowLimit() { return MAX_BORROW_LIMIT; }
    public int getMinBorrowDays() { return MIN_BORROW_DAYS; }
    public int getMaxBorrowDays() { return MAX_BORROW_DAYS; }

    private Optional<User> findUser(String username) {
        return users.stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    private Optional<Book> findBook(String id) {
        return books.stream().filter(book -> book.getId().equals(id)).findFirst();
    }

    private boolean hasBorrowHistory(String bookId) {
        return books.stream().anyMatch(b -> b.getId().equals(bookId) && b.getBorrowCount() > 0);
    }

    private void notifyBorrowersBookDeleted(Book book) {
        if (book.getBorrowedBy() != null) {
            addNotification(book.getBorrowedBy(), "A borrowed book was deleted: '" + book.getTitle() + "'.", "Book Deletion", "High");
        }
    }

    private void addNotificationToRole(Role role, String message, String category, String urgency) {
        users.stream()
                .filter(user -> user.getRole() == role && user.isActive())
                .forEach(user -> addNotification(user.getUsername(), message, category, urgency));
    }

    private void refreshViews() {
        availableBooks.setAll(books.stream().filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE).collect(Collectors.toList()));
        pendingBooks.setAll(books.stream().filter(book -> book.getStatus() == BookStatus.PENDING_APPROVAL).collect(Collectors.toList()));
        approvedBooks.setAll(books.stream().filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE || book.getStatus() == BookStatus.BORROWED).collect(Collectors.toList()));
        rejectedBooks.setAll(books.stream().filter(book -> book.getStatus() == BookStatus.REJECTED).collect(Collectors.toList()));
        catalogBooks.setAll(approvedBooks);
        usersView.setAll(new ArrayList<>(users));
    }

    private void seedDefaultBooks() {
        addSeedBook(1, "Introduction to Algorithms", "Thomas H. Cormen", "Computer Science", "2009-07-31",
                "Comprehensive textbook covering algorithms, data structures, and computational complexity.");
        addSeedBook(2, "Clean Code", "Robert C. Martin", "Software Engineering", "2008-08-01",
                "A guide to writing readable, maintainable, and efficient code.");
        addSeedBook(3, "Design Patterns", "Erich Gamma", "Software Engineering", "1994-10-31",
                "Classic reference on reusable object-oriented design patterns.");
    }

    private void addSeedBook(int index, String title, String author, String genre, String date, String summary) {
        Book book = new Book(title, "seed-author-" + index, author, genre, summary, "seed://book-" + index);
        book.approve(LocalDate.parse(date));
        books.add(book);
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) return "Password is required.";
        if (password.length() < 8 || password.length() > 64) return "Password must be 8-64 characters long.";
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasUpper || !hasLower || !hasDigit || !hasSymbol) {
            return "Password must include upper, lower, number, and symbol.";
        }
        return null;
    }
}
