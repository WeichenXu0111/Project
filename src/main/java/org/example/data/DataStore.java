package org.example.data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.Book;
import org.example.model.BookStatus;
import org.example.model.Role;
import org.example.model.User;
import org.example.model.AuthorDraft;
import org.example.security.PasswordUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataStore {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "lms-data.dat";

    private static final List<String> GENRE_OPTIONS = List.of(
            "Computer Science",
            "Software Engineering",
            "Artificial Intelligence",
            "Data Science",
            "Database",
            "Networking",
            "Programming",
            "Mathematics",
            "Cloud Computing",
            "Security",
            "Graphics",
            "Distributed Computing",
            "DevOps",
            "HCI",
            "Technology"
    );

    private final List<User> users = new ArrayList<>();
    private final List<Book> books = new ArrayList<>();

    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> pendingBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> authorBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> borrowedBooks = FXCollections.observableArrayList();
    private final ObservableList<User> usersView = FXCollections.observableArrayList();
    private final ObservableList<Book> catalogBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> approvedBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> rejectedBooks = FXCollections.observableArrayList();

    private final Map<String, AuthorDraft> drafts = new HashMap<>();

    public record RegistrationResult(boolean success, String message) {
    }

    public record ActionResult(boolean success, String message) {
    }

    public void load() {
        Path dataPath = Path.of(DATA_DIR, DATA_FILE);
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
            try {
                draftObj = input.readObject();
            } catch (EOFException ignored) {
                draftObj = null;
            }
            users.clear();
            books.clear();
            drafts.clear();
            if (userObj instanceof List<?>) {
                for (Object item : (List<?>) userObj) {
                    if (item instanceof User user) {
                        users.add(user);
                    }
                }
            }
            if (bookObj instanceof List<?>) {
                for (Object item : (List<?>) bookObj) {
                    if (item instanceof Book book) {
                        books.add(book);
                    }
                }
            }
            if (draftObj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof AuthorDraft draft) {
                        drafts.put(key, draft);
                    }
                }
            }
            if (books.isEmpty()) {
                seedDefaultBooks();
                save();
            }
            refreshViews();
        } catch (IOException | ClassNotFoundException ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(Path.of(DATA_DIR));
        } catch (IOException ignored) {
        }
        Path dataPath = Path.of(DATA_DIR, DATA_FILE);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dataPath.toFile()))) {
            output.writeObject(new ArrayList<>(users));
            output.writeObject(new ArrayList<>(books));
            output.writeObject(new HashMap<>(drafts));
        } catch (IOException ignored) {
        }
    }

    public RegistrationResult registerUser(String username,
                                           String fullName,
                                           String password,
                                           Role role,
                                           String bio,
                                           String employeeId) {
        if (username == null || username.isBlank()) {
            return new RegistrationResult(false, "Username is required.");
        }
        if (fullName == null || fullName.isBlank()) {
            return new RegistrationResult(false, "Full name is required.");
        }
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            return new RegistrationResult(false, passwordError);
        }
        if (role == null) {
            return new RegistrationResult(false, "Role is required.");
        }
        if (findUser(username).isPresent()) {
            return new RegistrationResult(false, "Username already exists.");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        users.add(new User(username, fullName, role, hash, salt, bio, employeeId));
        save();
        return new RegistrationResult(true, "OK");
    }

    public User authenticate(String username, String password, Role expectedRole) {
        Optional<User> userOpt = findUser(username);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        if (expectedRole == Role.STUDENT) {
            if (user.getRole() != Role.STUDENT && user.getRole() != Role.STAFF) {
                return null;
            }
        } else if (user.getRole() != expectedRole) {
            return null;
        }
        if (!PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return null;
        }
        return user;
    }

    public ObservableList<Book> getAvailableBooks() {
        refreshViews();
        return availableBooks;
    }

    public ObservableList<Book> getCatalogBooks() {
        refreshViews();
        return catalogBooks;
    }

    public ObservableList<Book> getApprovedBooks() {
        refreshViews();
        return approvedBooks;
    }

    public ObservableList<Book> getRejectedBooks() {
        refreshViews();
        return rejectedBooks;
    }

    public ObservableList<Book> getPendingBooks() {
        refreshViews();
        return pendingBooks;
    }

    public ObservableList<Book> getBooksByAuthor(String username) {
        refreshViews();
        authorBooks.setAll(books.stream()
                .filter(book -> book.getAuthorUsername().equals(username))
                .collect(Collectors.toList()));
        return authorBooks;
    }

    public ObservableList<Book> getBorrowedBooksBy(String username) {
        refreshViews();
        borrowedBooks.setAll(books.stream()
                .filter(book -> username.equals(book.getBorrowedBy()))
                .collect(Collectors.toList()));
        return borrowedBooks;
    }

    public ObservableList<User> getAllUsers() {
        refreshViews();
        return usersView;
    }

    public List<Book> getPopularBooks(int limit) {
        refreshViews();
        return books.stream()
                .filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE || book.getStatus() == BookStatus.BORROWED)
                .sorted((a, b) -> Integer.compare(b.getBorrowCount(), a.getBorrowCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public AuthorDraft getDraft(String username) {
        return drafts.get(username);
    }

    public void saveDraft(String username, AuthorDraft draft) {
        if (username == null || username.isBlank() || draft == null) {
            return;
        }
        draft.setLastSaved(LocalDateTime.now());
        drafts.put(username, draft);
        save();
    }

    public void clearDraft(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        drafts.remove(username);
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
        if (title == null || title.isBlank()) {
            return new ActionResult(false, "Title is required.");
        }
        if (genres == null || genres.isEmpty()) {
            return new ActionResult(false, "At least one genre is required.");
        }
        if (description == null || description.isBlank()) {
            return new ActionResult(false, "Summary is required.");
        }
        if (filePath == null || filePath.isBlank()) {
            return new ActionResult(false, "Book file is required.");
        }
        books.add(new Book(title, authorUsername, authorFullName, genres, description, filePath));
        save();
        refreshViews();
        return new ActionResult(true, "OK");
    }

    public ActionResult approveBook(String bookId) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) {
            return new ActionResult(false, "Book not found.");
        }
        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.PENDING_APPROVAL) {
            return new ActionResult(false, "Book is not pending approval.");
        }
        book.approve();
        save();
        refreshViews();
        return new ActionResult(true, "OK");
    }

    public ActionResult rejectBook(String bookId) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) {
            return new ActionResult(false, "Book not found.");
        }
        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.PENDING_APPROVAL) {
            return new ActionResult(false, "Book is not pending approval.");
        }
        book.reject();
        save();
        refreshViews();
        return new ActionResult(true, "OK");
    }

    public ActionResult borrowBook(String bookId, String borrower) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) {
            return new ActionResult(false, "Book not found.");
        }
        Book book = bookOpt.get();
        if (!book.isAvailable()) {
            return new ActionResult(false, "Book is not available.");
        }
        book.borrow(borrower);
        save();
        refreshViews();
        return new ActionResult(true, "OK");
    }

    public ActionResult returnBook(String bookId, String username) {
        Optional<Book> bookOpt = findBook(bookId);
        if (bookOpt.isEmpty()) {
            return new ActionResult(false, "Book not found.");
        }
        Book book = bookOpt.get();
        if (book.getStatus() != BookStatus.BORROWED) {
            return new ActionResult(false, "Book is not currently borrowed.");
        }
        if (!username.equals(book.getBorrowedBy())) {
            return new ActionResult(false, "You did not borrow this book.");
        }
        book.returnBook();
        save();
        refreshViews();
        return new ActionResult(true, "OK");
    }

    public List<Book> getRecommendations(String username, int limit) {
        refreshViews();
        if (limit <= 0) {
            return List.of();
        }
        Map<String, Long> genreCounts = books.stream()
                .filter(book -> username != null && username.equals(book.getBorrowedBy()))
                .flatMap(book -> book.getGenres().stream())
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

        List<Book> candidates = books.stream()
                .filter(Book::isAvailable)
                .collect(Collectors.toList());

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
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return Integer.compare(b.getBorrowCount(), a.getBorrowCount());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<String> getGenreOptions() {
        return GENRE_OPTIONS;
    }

    private Optional<User> findUser(String username) {
        return users.stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    private Optional<Book> findBook(String id) {
        return books.stream().filter(book -> book.getId().equals(id)).findFirst();
    }

    private void refreshViews() {
        availableBooks.setAll(books.stream()
                .filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE)
                .collect(Collectors.toList()));
        pendingBooks.setAll(books.stream()
                .filter(book -> book.getStatus() == BookStatus.PENDING_APPROVAL)
                .collect(Collectors.toList()));
        approvedBooks.setAll(books.stream()
                .filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE || book.getStatus() == BookStatus.BORROWED)
                .collect(Collectors.toList()));
        rejectedBooks.setAll(books.stream()
                .filter(book -> book.getStatus() == BookStatus.REJECTED)
                .collect(Collectors.toList()));
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
        addSeedBook(4, "Artificial Intelligence: A Modern Approach", "Stuart Russell", "Artificial Intelligence", "2020-04-28",
                "Foundational textbook on AI principles and applications.");
        addSeedBook(5, "Operating System Concepts", "Abraham Silberschatz", "Computer Science", "2018-01-01",
                "Detailed explanation of operating system design and implementation.");
        addSeedBook(6, "Computer Networks", "Andrew S. Tanenbaum", "Networking", "2021-03-15",
                "Comprehensive overview of networking principles and protocols.");
        addSeedBook(7, "Database System Concepts", "Henry F. Korth", "Database", "2019-02-10",
                "Introduction to database systems and relational models.");
        addSeedBook(8, "The Pragmatic Programmer", "Andrew Hunt", "Software Engineering", "2019-09-13",
                "Practical advice for modern software development.");
        addSeedBook(9, "Deep Learning", "Ian Goodfellow", "Artificial Intelligence", "2016-11-18",
                "In-depth exploration of deep neural networks and training methods.");
        addSeedBook(10, "Structure and Interpretation of Computer Programs", "Harold Abelson", "Computer Science", "1996-07-25",
                "Influential book on programming paradigms and abstraction.");
        addSeedBook(11, "Modern Operating Systems", "Andrew S. Tanenbaum", "Computer Science", "2014-03-15",
                "Analysis of operating system structures and processes.");
        addSeedBook(12, "Introduction to the Theory of Computation", "Michael Sipser", "Computer Science", "2012-06-27",
                "Core concepts of automata theory and computability.");
        addSeedBook(13, "Computer Organization and Design", "David A. Patterson", "Computer Architecture", "2017-05-20",
                "Hardware and software interface fundamentals.");
        addSeedBook(14, "Distributed Systems", "Maarten van Steen", "Distributed Computing", "2016-01-10",
                "Principles and paradigms of distributed computing systems.");
        addSeedBook(15, "Machine Learning", "Tom M. Mitchell", "Artificial Intelligence", "1997-03-01",
                "Fundamental concepts in machine learning theory.");
        addSeedBook(16, "Pattern Recognition and Machine Learning", "Christopher M. Bishop", "Artificial Intelligence", "2006-08-17",
                "Statistical techniques for machine learning applications.");
        addSeedBook(17, "Compilers: Principles, Techniques, and Tools", "Alfred V. Aho", "Computer Science", "2006-08-02",
                "Comprehensive guide to compiler construction.");
        addSeedBook(18, "The C Programming Language", "Brian W. Kernighan", "Programming", "1988-04-01",
                "Authoritative introduction to C programming.");
        addSeedBook(19, "Java: The Complete Reference", "Herbert Schildt", "Programming", "2018-11-20",
                "Detailed reference for Java programming language.");
        addSeedBook(20, "Python Crash Course", "Eric Matthes", "Programming", "2019-05-03",
                "Hands-on introduction to Python programming.");
        addSeedBook(21, "Software Engineering", "Ian Sommerville", "Software Engineering", "2015-04-30",
                "Comprehensive guide to software development processes.");
        addSeedBook(22, "Algorithms", "Robert Sedgewick", "Computer Science", "2011-04-04",
                "Algorithm design and analysis with practical examples.");
        addSeedBook(23, "Data Mining: Concepts and Techniques", "Jiawei Han", "Data Science", "2011-07-06",
                "Methods and applications of data mining.");
        addSeedBook(24, "Reinforcement Learning: An Introduction", "Richard S. Sutton", "Artificial Intelligence", "2018-10-15",
                "Core concepts of reinforcement learning algorithms.");
        addSeedBook(25, "Cloud Computing", "Kai Hwang", "Cloud Computing", "2012-09-18",
                "Principles of cloud systems and virtualization.");
        addSeedBook(26, "Cybersecurity Essentials", "Charles J. Brooks", "Security", "2018-02-22",
                "Foundational concepts in cybersecurity.");
        addSeedBook(27, "Computer Graphics: Principles and Practice", "John F. Hughes", "Graphics", "2013-07-06",
                "Core techniques in computer graphics.");
        addSeedBook(28, "Information Retrieval", "Christopher D. Manning", "Data Science", "2008-07-07",
                "Search engines and retrieval system design.");
        addSeedBook(29, "Linear Algebra and Its Applications", "Gilbert Strang", "Mathematics", "2016-01-01",
                "Fundamental concepts of linear algebra.");
        addSeedBook(30, "Discrete Mathematics and Its Applications", "Kenneth H. Rosen", "Mathematics", "2018-06-14",
                "Mathematical foundations for computer science.");
        addSeedBook(31, "Probability and Statistics for Engineering", "Jay L. Devore", "Mathematics", "2015-01-01",
                "Statistical methods for engineering applications.");
        addSeedBook(32, "Introduction to Machine Learning with Python", "Andreas C. Muller", "Artificial Intelligence", "2016-09-26",
                "Practical guide to ML using Python.");
        addSeedBook(33, "Hands-On Machine Learning with Scikit-Learn", "Aurelien Geron", "Artificial Intelligence", "2019-10-15",
                "Applied machine learning techniques.");
        addSeedBook(34, "Effective Java", "Joshua Bloch", "Programming", "2018-01-06",
                "Best practices for Java programming.");
        addSeedBook(35, "Head First Design Patterns", "Eric Freeman", "Software Engineering", "2004-10-25",
                "Accessible introduction to design patterns.");
        addSeedBook(36, "The Art of Computer Programming", "Donald E. Knuth", "Computer Science", "2011-03-09",
                "Classic multi-volume work on algorithms.");
        addSeedBook(37, "Clean Architecture", "Robert C. Martin", "Software Engineering", "2017-09-20",
                "Principles of maintainable system design.");
        addSeedBook(38, "Microservices Patterns", "Chris Richardson", "Software Engineering", "2018-11-19",
                "Design patterns for microservices architecture.");
        addSeedBook(39, "Kubernetes Up & Running", "Kelsey Hightower", "Cloud Computing", "2019-10-08",
                "Guide to Kubernetes deployment.");
        addSeedBook(40, "Site Reliability Engineering", "Betsy Beyer", "DevOps", "2016-03-23",
                "Practices for building reliable systems.");
        addSeedBook(41, "The Mythical Man-Month", "Frederick P. Brooks Jr.", "Software Engineering", "1995-08-12",
                "Essays on software project management.");
        addSeedBook(42, "Refactoring", "Martin Fowler", "Software Engineering", "2018-11-19",
                "Techniques for improving code structure.");
        addSeedBook(43, "Code Complete", "Steve McConnell", "Programming", "2004-06-09",
                "Practical handbook of software construction.");
        addSeedBook(44, "Introduction to Data Science", "Rafael A. Irizarry", "Data Science", "2019-04-12",
                "Concepts and tools in data science.");
        addSeedBook(45, "Big Data Fundamentals", "Thomas Erl", "Data Science", "2016-12-16",
                "Core principles of big data systems.");
        addSeedBook(46, "Blockchain Basics", "Daniel Drescher", "Technology", "2017-03-20",
                "Introduction to blockchain technology.");
        addSeedBook(47, "Quantum Computing for Computer Scientists", "Noson S. Yanofsky", "Computer Science", "2008-12-01",
                "Concepts in quantum computing.");
        addSeedBook(48, "Human-Computer Interaction", "Alan Dix", "HCI", "2003-04-14",
                "Design principles for user interfaces.");
        addSeedBook(49, "Computer Vision: Algorithms and Applications", "Richard Szeliski", "Artificial Intelligence", "2010-09-03",
                "Core methods in computer vision.");
        addSeedBook(50, "Natural Language Processing with Python", "Steven Bird", "Artificial Intelligence", "2009-06-12",
                "Practical NLP using Python.");
    }

    private void addSeedBook(int index,
                             String title,
                             String author,
                             String genre,
                             String date,
                             String summary) {
        Book book = new Book(title, "seed-author-" + index, author, genre, summary, "seed://book-" + index);
        book.approve(LocalDate.parse(date));
        books.add(book);
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return "Password is required.";
        }
        if (password.length() < 8 || password.length() > 64) {
            return "Password must be 8-64 characters long.";
        }
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
