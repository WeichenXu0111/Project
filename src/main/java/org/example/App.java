package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.example.data.DataStore;
import org.example.model.AuthorDraft;
import org.example.model.Book;
import org.example.model.BookRequest;
import org.example.model.BookReview;
import org.example.model.BookStatus;
import org.example.model.Notification;
import org.example.model.ReadingHistory;
import org.example.model.Role;
import org.example.model.User;
import org.example.pdf.InteractivePDFReader;
import org.example.pdf.PDFHighlightManager;
import org.example.service.RequestedBookDownloader;
import org.example.service.SummaryService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main application class for the HKUST E-Library System - Phase 2.
 * This JavaFX application provides portals for Students/Staff, Authors, and Librarians
 * with features like book borrowing, reading with bookmarks/highlights, profile management,
 * notifications, and persistent crash recovery.
 */
public class App extends Application {
    // Core application state
    private final DataStore dataStore = new DataStore(); // Central data management
    private Stage stage; // Main application window
    private BorderPane root; // Root layout container
    private Scene scene; // Main scene
    private User currentUser; // Currently logged-in user
    private ImageView headerAvatarView;
    private Label headerAvatarFallback;
    private Runnable refreshStudentHistoryView = () -> {};

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        dataStore.load();

        root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildLanding());
        scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        stage.setTitle("HKUST E-Library System - Phase 2");
        stage.setScene(scene);
        stage.show();

        restorePreviousSessionIfPossible();
    }

    private HBox buildHeader() {
        Label title = new Label("HKUST Library");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("E-Library System - Phase 2");
        subtitle.getStyleClass().add("brand-subtitle");

        VBox brand = new VBox(2, title, subtitle);
        ImageView logoView = tryBuildLogo();
        HBox brandRow = logoView == null ? new HBox(brand) : new HBox(10, logoView, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Button homeBtn = navButton("Home", () -> {
            if (currentUser == null) root.setCenter(buildLanding());
            else showDashboard(currentUser, "Dashboard");
        });
        Button profileBtn = navButton("My Profile", () -> {
            if (currentUser != null) {
                root.setCenter(buildProfileScreen());
                saveSession("Profile", null);
            }
        });
        Button notificationsBtn = navButton("Notifications", () -> {
            if (currentUser != null) {
                root.setCenter(buildNotificationScreen());
                saveSession("Notifications", null);
            }
        });
        Button crashBtn = navButton("Crash Test", this::simulateCrash);
        crashBtn.getStyleClass().add("danger-button");

        Button logoutBtn = navButton("Logout", () -> {
            currentUser = null;
            dataStore.clearSessionState();
            root.setCenter(buildLanding());
        });

        headerAvatarView = new ImageView();
        headerAvatarView.getStyleClass().add("header-avatar-image");
        headerAvatarView.setFitWidth(36);
        headerAvatarView.setFitHeight(36);
        headerAvatarView.setPreserveRatio(false);
        headerAvatarView.setClip(new Circle(18, 18, 18));

        headerAvatarFallback = new Label("U");
        headerAvatarFallback.getStyleClass().add("header-avatar-fallback");

        StackPane avatarBox = new StackPane(headerAvatarView, headerAvatarFallback);
        avatarBox.getStyleClass().add("header-avatar");
        Tooltip.install(avatarBox, new Tooltip("User avatar"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, brandRow, spacer, homeBtn, notificationsBtn, profileBtn, crashBtn, logoutBtn, avatarBox);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        Runnable update = () -> {
            boolean loggedIn = currentUser != null;
            profileBtn.setVisible(loggedIn); profileBtn.setManaged(loggedIn);
            notificationsBtn.setVisible(loggedIn); notificationsBtn.setManaged(loggedIn);
            logoutBtn.setVisible(loggedIn); logoutBtn.setManaged(loggedIn);
            crashBtn.setVisible(loggedIn); crashBtn.setManaged(loggedIn);
            avatarBox.setVisible(loggedIn); avatarBox.setManaged(loggedIn);
            refreshHeaderAvatar();
        };
        root.centerProperty().addListener((a, b, c) -> update.run());
        update.run();

        return header;
    }

    private void refreshHeaderAvatar() {
        if (headerAvatarView == null || headerAvatarFallback == null) {
            return;
        }
        Image avatar = resolveAvatarImage(currentUser);
        if (avatar != null) {
            headerAvatarView.setImage(avatar);
            headerAvatarFallback.setVisible(false);
            headerAvatarFallback.setManaged(false);
        } else {
            headerAvatarView.setImage(null);
            headerAvatarFallback.setText(currentUser != null && !safe(currentUser.getFullName()).isBlank()
                    ? safe(currentUser.getFullName()).substring(0, 1).toUpperCase()
                    : "U");
            headerAvatarFallback.setVisible(true);
            headerAvatarFallback.setManaged(true);
        }
    }

    private Image resolveAvatarImage(User user) {
        if (user != null) {
            String avatarPath = safe(user.getAvatarPath()).trim();
            if (!avatarPath.isBlank()) {
                File avatarFile = new File(avatarPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                    try {
                        return new Image(avatarFile.toURI().toString(), 36, 36, false, true);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        try {
            if (getClass().getResource("/logo.png") != null) {
                return new Image(getClass().getResource("/logo.png").toExternalForm(), 36, 36, false, true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private ImageView tryBuildLogo() {
        try {
            Image image = null;
            if (getClass().getResource("/logo.png") != null) image = new Image(getClass().getResource("/logo.png").toExternalForm());
            else if (new File("logo.png").exists()) image = new Image(new File("logo.png").toURI().toString());
            if (image == null) return null;
            ImageView v = new ImageView(image);
            v.setFitHeight(44);
            v.setPreserveRatio(true);
            return v;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Button navButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private VBox buildLanding() {
        Label title = new Label("Welcome to HKUST Library");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Phase 2: Student/Staff, Author, and Librarian portals with advanced features.");
        subtitle.getStyleClass().add("hero-subtitle");

        VBox studentCard = portalCard("Student / Staff", "Borrow, read, bookmark, return, and manage profile.", () -> root.setCenter(buildAuth(Role.STUDENT)));
        VBox authorCard = portalCard("Author", "Publish, update, delete, and track submission notifications.", () -> root.setCenter(buildAuth(Role.AUTHOR)));
        VBox librarianCard = portalCard("Librarian", "Approve books, manage users, and monitor records.", () -> root.setCenter(buildAuth(Role.LIBRARIAN)));

        HBox cards = new HBox(16, studentCard, authorCard, librarianCard);
        cards.setAlignment(Pos.CENTER);
        HBox.setHgrow(studentCard, Priority.ALWAYS);
        HBox.setHgrow(authorCard, Priority.ALWAYS);
        HBox.setHgrow(librarianCard, Priority.ALWAYS);

        VBox wrap = new VBox(20, title, subtitle, cards);
        wrap.setPadding(new Insets(28));
        return wrap;
    }

    private VBox portalCard(String title, String desc, Runnable action) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        Label d = new Label(desc);
        d.getStyleClass().add("muted-text");
        d.setWrapText(true);
        Button enter = new Button("Enter Portal");
        enter.getStyleClass().add("primary-button");
        enter.setOnAction(e -> action.run());
        VBox box = new VBox(10, t, d, enter);
        box.setPadding(new Insets(16));
        box.getStyleClass().add("card");
        return box;
    }

    private BorderPane buildAuth(Role role) {
        Label title = new Label(role.getDisplayName() + " Portal");
        title.getStyleClass().add("section-title");

        TabPane tabs = new TabPane(buildLoginTab(role), buildRegisterTab(role));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane pane = new BorderPane(tabs);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
        BorderPane.setMargin(tabs, new Insets(0, 20, 20, 20));
        return pane;
    }

    private Tab buildLoginTab(Role role) {
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        Label msg = new Label();

        GridPane form = formGrid();
        form.addRow(0, formLabel("Username"), username);
        form.addRow(1, formLabel("Password"), password);

        Button login = new Button("Login");
        login.getStyleClass().add("primary-button");
        login.setOnAction(e -> {
            User u = dataStore.authenticate(username.getText().trim(), password.getText(), role);
            if (u == null) {
                setMessage(msg, "Invalid credentials, role mismatch, or account is inactive.", false);
                return;
            }
            currentUser = u;
            showDashboard(u, "Dashboard");
        });

        VBox content = new VBox(14, form, login, msg);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("card");
        return new Tab("Login", content);
    }

    private Tab buildRegisterTab(Role role) {
        TextField username = new TextField();
        TextField fullName = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> roleSelect = new ComboBox<>();
        TextField bio = new TextField();
        TextField employeeId = new TextField();
        Label msg = new Label();

        if (role == Role.STUDENT) {
            roleSelect.getItems().addAll(Role.STUDENT, Role.STAFF);
            roleSelect.getSelectionModel().select(Role.STUDENT);
        }

        GridPane form = formGrid();
        int r = 0;
        form.addRow(r++, formLabel("Username"), username);
        form.addRow(r++, formLabel("Full Name"), fullName);
        form.addRow(r++, formLabel("Password"), password);
        if (role == Role.STUDENT) form.addRow(r++, formLabel("Role"), roleSelect);
        if (role == Role.AUTHOR) form.addRow(r++, formLabel("Bio"), bio);
        if (role == Role.LIBRARIAN) form.addRow(r++, formLabel("Employee ID"), employeeId);

        Button create = new Button("Create Account");
        create.getStyleClass().add("primary-button");
        create.setOnAction(e -> {
            Role finalRole = role == Role.STUDENT ? roleSelect.getValue() : role;
            DataStore.RegistrationResult result = dataStore.registerUser(
                    username.getText().trim(),
                    fullName.getText().trim(),
                    password.getText(),
                    finalRole,
                    bio.getText().trim(),
                    employeeId.getText().trim()
            );
            setMessage(msg, result.message(), result.success());
        });

        VBox content = new VBox(14, form, create, msg);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("card");
        return new Tab("Register", content);
    }

    private void showDashboard(User user, String screenName) {
        if (user.getRole() == Role.AUTHOR) root.setCenter(buildAuthorDashboard());
        else if (user.getRole() == Role.LIBRARIAN) root.setCenter(buildLibrarianDashboard());
        else root.setCenter(buildStudentDashboard());
        saveSession(screenName, null);
    }

    private BorderPane buildStudentDashboard() {
        dataStore.autoReturnExpiredBooks();

        Label title = new Label("Student / Staff Dashboard");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Borrow, read, review, and track your library activity in one place.");
        subtitle.getStyleClass().add("muted-text");
        VBox titleBlock = new VBox(2, title, subtitle);

        VBox statsA = statCard("Available Books", String.valueOf(dataStore.getAvailableBooks().size()));
        VBox statsB = statCard("My Borrowed", String.valueOf(dataStore.getBorrowedBooksBy(currentUser.getUsername()).size()));
        VBox statsC = statCard("Borrow Limit", "Max " + dataStore.getMaxBorrowLimit());
        HBox stats = new HBox(10, statsA, statsB, statsC);

        TableView<Book> catalogTable = buildAvailableBooksTable();
        catalogTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        FilteredList<Book> filtered = new FilteredList<>(dataStore.getCatalogBooks(), b -> true);
        SortedList<Book> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(catalogTable.comparatorProperty());
        catalogTable.setItems(sorted);

        VBox filters = buildCatalogFilters(filtered);
        // Add instruction for multiple selection
        Label multiSelectTip = new Label("Tip: Hold Ctrl (or Cmd on Mac) to select multiple books.");
        multiSelectTip.getStyleClass().add("muted-text");
        // Add message label for filter actions
        Label filterMsg = new Label();
        filterMsg.getStyleClass().add("form-message");
        VBox borrowActions = buildBorrowActions(catalogTable, () -> {
            ((Label) statsA.getChildren().get(0)).setText(String.valueOf(dataStore.getAvailableBooks().size()));
            ((Label) statsB.getChildren().get(0)).setText(String.valueOf(dataStore.getBorrowedBooksBy(currentUser.getUsername()).size()));
            catalogTable.refresh();
            refreshStudentHistoryView.run();
        });

        TableView<Book> borrowedTable = buildBorrowedBooksTable();
        borrowedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox borrowedActions = buildBorrowedActions(borrowedTable, () -> {
            borrowedTable.setItems(dataStore.getBorrowedBooksBy(currentUser.getUsername()));
            ((Label) statsA.getChildren().get(0)).setText(String.valueOf(dataStore.getAvailableBooks().size()));
            ((Label) statsB.getChildren().get(0)).setText(String.valueOf(dataStore.getBorrowedBooksBy(currentUser.getUsername()).size()));
            catalogTable.refresh();
            refreshStudentHistoryView.run();
        });

        Label catalogTitle = new Label("Catalog");
        catalogTitle.getStyleClass().add("card-title");
        VBox left = new VBox(10, catalogTitle, filters, filterMsg, multiSelectTip, catalogTable, borrowActions);
        left.getStyleClass().addAll("card", "workspace-card");
        left.setPadding(new Insets(14));
        HBox.setHgrow(left, Priority.ALWAYS);

        Label borrowedTitle = new Label("My Borrowed Books");
        borrowedTitle.getStyleClass().add("card-title");
        VBox right = new VBox(10, borrowedTitle, borrowedTable, borrowedActions);
        right.getStyleClass().addAll("card", "workspace-card");
        right.setPadding(new Insets(14));
        right.setPrefWidth(470);

        HBox bodyRow = new HBox(12, left, right);

        TabPane taskTabs = new TabPane();
        taskTabs.getStyleClass().add("portal-tabs");
        taskTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab borrowTab = new Tab("Catalog & Borrowed", bodyRow);
        Tab historyTab = new Tab("Reading History", buildReadingHistoryScreen());
        Tab reviewTab = new Tab("Reviews & Ratings", buildReviewScreen());
        Tab requestTab = new Tab("Request New Book", buildBookRequestScreen());
        taskTabs.getTabs().addAll(borrowTab, historyTab, reviewTab, requestTab);
        VBox.setVgrow(taskTabs, Priority.ALWAYS);

        VBox body = new VBox(14, stats, taskTabs);
        body.setPadding(new Insets(0, 20, 20, 20));

        BorderPane pane = new BorderPane(body);
        pane.setTop(titleBlock);
        BorderPane.setMargin(titleBlock, new Insets(20, 20, 8, 20));
        return pane;
    }

    private TableView<Book> buildAvailableBooksTable() {
        TableView<Book> table = new TableView<>();
        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Author", Book::getAuthorFullName, 150),
                col("Genre", Book::getGenre, 120),
                col("Rating", b -> ratingSummary(b.getId()), 110),
                col("Publish Date", b -> formatDate(b.getApprovedDate()), 120),
                col("Status", b -> b.getStatus().getDisplayName(), 130)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No books found."));
        return table;
    }

    private TableView<Book> buildBorrowedBooksTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getBorrowedBooksBy(currentUser.getUsername()));
        table.getColumns().addAll(
                col("Title", Book::getTitle, 170),
                col("Borrow Date", b -> formatDate(b.getBorrowedDate()), 110),
                col("Due Date", b -> formatDate(b.getDueDate()), 110),
                col("Status", b -> b.getStatus().getDisplayName(), 120)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildBorrowActions(TableView<Book> table, Runnable onRefresh) {
        Label msg = new Label();
        Spinner<Integer> days = new Spinner<>(dataStore.getMinBorrowDays(), dataStore.getMaxBorrowDays(), 14);
        days.setEditable(true);

        Button quickPreview = new Button("Read / Preview");
        quickPreview.getStyleClass().add("secondary-button");
        quickPreview.setOnAction(e -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setMessage(msg, "Please select a book.", false);
                return;
            }
            showBookReader(selected, false);
        });

        Button borrowSelected = new Button("Borrow Selected");
        borrowSelected.getStyleClass().add("primary-button");
        borrowSelected.setOnAction(e -> {
            List<Book> selected = table.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) {
                setMessage(msg, "Please select one or more books.", false);
                return;
            }
            List<String> ids = selected.stream().map(Book::getId).collect(Collectors.toList());
            DataStore.ActionResult r = dataStore.borrowBooks(ids, currentUser.getUsername(), days.getValue());
            setMessage(msg, r.message(), r.success());
            if (onRefresh != null) onRefresh.run();
        });

        HBox row = new HBox(8, new Label("Duration (days):"), days, quickPreview, borrowSelected);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, row, msg);
    }

    private VBox buildBorrowedActions(TableView<Book> borrowedTable, Runnable onRefresh) {
        // Add tip for multiple selection
        Label multiSelectTip = new Label("Tip: Hold Ctrl (or Cmd on Mac) to select multiple books.");
        multiSelectTip.getStyleClass().add("muted-text");

        Label msg = new Label();
        Button readBtn = new Button("Read with Bookmark/Highlight");
        readBtn.getStyleClass().add("secondary-button");
        readBtn.setOnAction(e -> {
            Book selected = borrowedTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setMessage(msg, "Please select a borrowed book.", false);
                return;
            }
            showBookReader(selected, true);
            if (onRefresh != null) onRefresh.run();
        });

        Button returnSelected = new Button("Return Selected (Partial Return)");
        returnSelected.getStyleClass().add("danger-button");
        returnSelected.setOnAction(e -> {
            List<Book> selected = borrowedTable.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) {
                setMessage(msg, "Select one or more books to return.", false);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Return " + selected.size() + " selected book(s)?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            List<String> ids = selected.stream().map(Book::getId).collect(Collectors.toList());
            DataStore.ActionResult r = dataStore.returnBooks(ids, currentUser.getUsername());
            setMessage(msg, r.message(), r.success());
            if (onRefresh != null) onRefresh.run();
        });

        HBox row = new HBox(8, readBtn, returnSelected);
        return new VBox(8, multiSelectTip, row, msg);
    }

    private VBox buildCatalogFilters(FilteredList<Book> filtered) {
        TextField search = new TextField();
        search.setPromptText("Search title or author...");
        ComboBox<String> genre = new ComboBox<>();
        genre.getItems().add("All");
        genre.getItems().addAll(dataStore.getGenreOptions());
        genre.getSelectionModel().selectFirst();
        ComboBox<String> availability = new ComboBox<>();
        availability.getItems().addAll("All", "Available", "Borrowed");
        availability.getSelectionModel().selectFirst();
        DatePicker date = new DatePicker();
        date.setPromptText("Publish date");

        Runnable apply = () -> filtered.setPredicate(book -> {
            if (book == null) return false;
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            if (!q.isBlank()) {
                String t = safe(book.getTitle()).toLowerCase();
                String a = safe(book.getAuthorFullName()).toLowerCase();
                if (!t.contains(q) && !a.contains(q)) return false;
            }
            if (!"All".equals(genre.getValue())) {
                boolean ok = book.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(genre.getValue()));
                if (!ok) return false;
            }
            if ("Available".equals(availability.getValue()) && !book.isAvailable()) return false;
            if ("Borrowed".equals(availability.getValue()) && book.isAvailable()) return false;
            if (date.getValue() != null && (book.getApprovedDate() == null || !date.getValue().equals(book.getApprovedDate()))) return false;
            return true;
        });

        search.textProperty().addListener((a, b, c) -> apply.run());
        genre.valueProperty().addListener((a, b, c) -> apply.run());
        availability.valueProperty().addListener((a, b, c) -> apply.run());
        date.valueProperty().addListener((a, b, c) -> apply.run());

        Button clear = new Button("Clear");
        clear.getStyleClass().add("ghost-button");
        clear.setMinWidth(60); // Ensure enough width for the label
        // Accept a message label for notifications
        clear.setOnAction(e -> {
            search.clear();
            genre.getSelectionModel().selectFirst();
            availability.getSelectionModel().selectFirst();
            date.setValue(null);
            apply.run();
            // Show notification if filterMsg is available in parent
            VBox parent = (VBox) ((Button) e.getSource()).getParent().getParent();
            for (javafx.scene.Node node : parent.getChildren()) {
                if (node instanceof Label && ((Label) node).getStyleClass().contains("form-message")) {
                    setMessage((Label) node, "Filters cleared.", true);
                }
            }
        });

        HBox row = new HBox(8, search, genre, availability, date, clear);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        // Optionally, prevent the date picker from growing too wide
        date.setMaxWidth(120);
        // Optionally, prevent the combo boxes from growing too wide
        genre.setMaxWidth(110);
        availability.setMaxWidth(110);
        // Optionally, let the clear button keep its preferred width
        // clear.setMaxWidth(Region.USE_PREF_SIZE);

        VBox box = new VBox(8, row, new Label("Search/Filter by title, author, genre, publish date, availability."));
        box.getStyleClass().add("aligned-block");
        return box;
    }

    private VBox buildReadingHistoryScreen() {
        ObservableList<ReadingHistory> source = FXCollections.observableArrayList(dataStore.getReadingHistory(currentUser.getUsername()));
        FilteredList<ReadingHistory> filtered = new FilteredList<>(source, h -> true);

        TextField search = new TextField();
        search.setPromptText("Search title or author...");
        ComboBox<String> genre = new ComboBox<>();
        genre.getItems().add("All");
        genre.getItems().addAll(dataStore.getGenreOptions());
        genre.getSelectionModel().selectFirst();
        DatePicker from = new DatePicker();
        from.setPromptText("Borrowed from");
        DatePicker to = new DatePicker();
        to.setPromptText("Borrowed to");

        Runnable apply = () -> filtered.setPredicate(history -> {
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            if (!q.isBlank()
                    && !safe(history.getBookTitle()).toLowerCase().contains(q)
                    && !safe(history.getAuthor()).toLowerCase().contains(q)) return false;
            if (!"All".equals(genre.getValue()) && !safe(history.getGenre()).toLowerCase().contains(genre.getValue().toLowerCase())) return false;
            if (from.getValue() != null && history.getBorrowDate().isBefore(from.getValue())) return false;
            if (to.getValue() != null && history.getBorrowDate().isAfter(to.getValue())) return false;
            return true;
        });
        search.textProperty().addListener((a, b, c) -> apply.run());
        genre.valueProperty().addListener((a, b, c) -> apply.run());
        from.valueProperty().addListener((a, b, c) -> apply.run());
        to.valueProperty().addListener((a, b, c) -> apply.run());

        TableView<ReadingHistory> table = new TableView<>(filtered);
        table.getColumns().addAll(
                typedCol("Book Title", ReadingHistory::getBookTitle, 190),
                typedCol("Author", ReadingHistory::getAuthor, 150),
                typedCol("Genre", ReadingHistory::getGenre, 130),
                typedCol("Borrow Date", h -> formatDate(h.getBorrowDate()), 110),
                typedCol("Return Date", h -> formatDate(h.getReturnDate()), 110),
                typedCol("Duration", h -> h.getReadingDurationDays() + " day(s)", 105),
                typedCol("Progress", h -> h.getProgressPercent() + "%", 90)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Borrow and read a book to build your history."));

        Label insight = new Label(readingInsight(source));
        insight.getStyleClass().add("muted-text");
        Label badges = new Label(readingBadges(source));
        badges.getStyleClass().add("status-pill");

        Button exportCsv = new Button("Export CSV");
        exportCsv.getStyleClass().add("secondary-button");
        exportCsv.setOnAction(e -> exportReadingHistoryCsv(new ArrayList<>(filtered)));

        Button exportPdf = new Button("Export PDF");
        exportPdf.getStyleClass().add("secondary-button");
        exportPdf.setOnAction(e -> exportReadingHistoryPdf(new ArrayList<>(filtered)));

        HBox filters = new HBox(8, search, genre, from, to);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actionRow = new HBox(8, exportCsv, exportPdf);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        PieChart genreChart = buildGenrePieChart(source);
        BarChart<String, Number> progressChart = buildProgressBarChart(source);
        HBox insights = new HBox(12, genreChart, progressChart);
        insights.getStyleClass().add("chart-row");
        HBox.setHgrow(insights.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(insights.getChildren().get(1), Priority.ALWAYS);

        refreshStudentHistoryView = () -> {
            source.setAll(dataStore.getReadingHistory(currentUser.getUsername()));
            apply.run();
            insight.setText(readingInsight(source));
            badges.setText(readingBadges(source));
            refreshGenrePieChart(genreChart, source);
            refreshProgressBarChart(progressChart, source);
            table.refresh();
        };

        Label screenTitle = new Label("Reading History");
        screenTitle.getStyleClass().add("card-title");
        VBox box = new VBox(10, screenTitle, filters, insight, badges, insights, table, actionRow);
        box.getStyleClass().add("task-surface");
        box.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox buildReviewScreen() {
        TableView<Book> books = buildAvailableBooksTable();
        books.setItems(dataStore.getCatalogBooks());

        ListView<BookReview> reviews = new ListView<>();
        reviews.setPlaceholder(new Label("Select a book to view reviews."));
        reviews.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(BookReview review, boolean empty) {
                super.updateItem(review, empty);
                if (empty || review == null) { setText(null); setGraphic(null); return; }
                Label title = new Label(stars(review.getRating()) + "  " + review.getDisplayName());
                title.getStyleClass().add("card-title");
                Label body = new Label(review.getReviewText());
                body.setWrapText(true);
                Label meta = new Label(formatDateTime(review.getSubmittedAt()) + " | Helpful: " + review.getHelpfulCount());
                meta.getStyleClass().add("muted-text");
                setGraphic(new VBox(3, title, body, meta));
            }
        });

        Label rating = new Label("Select a book");
        rating.getStyleClass().add("card-title");
        Spinner<Integer> stars = new Spinner<>(1, 5, 5);
        stars.setEditable(true);
        TextArea reviewText = new TextArea();
        reviewText.setPromptText("Write your review after borrowing this book...");
        reviewText.setPrefRowCount(4);
        CheckBox anonymous = new CheckBox("Submit anonymously");
        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("Most Recent", "Most Helpful", "Highest Rating", "Lowest Rating");
        sort.getSelectionModel().selectFirst();
        Label msg = new Label();

        Runnable refreshReviews = () -> {
            Book selected = books.getSelectionModel().getSelectedItem();
            if (selected == null) {
                rating.setText("Select a book");
                reviews.setItems(FXCollections.observableArrayList());
                return;
            }
            rating.setText(selected.getTitle() + " - " + ratingSummary(selected.getId()));
            List<BookReview> reviewData = new ArrayList<>(dataStore.getReviewsForBook(selected.getId()));
            if ("Most Helpful".equals(sort.getValue())) {
                reviewData.sort(Comparator.comparingInt(BookReview::getHelpfulCount).reversed()
                        .thenComparing(BookReview::getSubmittedAt, Comparator.reverseOrder()));
            } else if ("Highest Rating".equals(sort.getValue())) {
                reviewData.sort(Comparator.comparingInt(BookReview::getRating).reversed()
                        .thenComparing(BookReview::getSubmittedAt, Comparator.reverseOrder()));
            } else if ("Lowest Rating".equals(sort.getValue())) {
                reviewData.sort(Comparator.comparingInt(BookReview::getRating)
                        .thenComparing(BookReview::getSubmittedAt, Comparator.reverseOrder()));
            }
            reviews.setItems(FXCollections.observableArrayList(reviewData));
        };
        books.getSelectionModel().selectedItemProperty().addListener((a, b, c) -> refreshReviews.run());
        sort.valueProperty().addListener((a, b, c) -> refreshReviews.run());

        Button submit = new Button("Submit / Update Review");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(e -> {
            Book selected = books.getSelectionModel().getSelectedItem();
            if (selected == null) { setMessage(msg, "Select a book first.", false); return; }
            DataStore.ActionResult result = dataStore.submitReview(
                    currentUser.getUsername(),
                    selected.getId(),
                    stars.getValue(),
                    reviewText.getText(),
                    anonymous.isSelected()
            );
            setMessage(msg, result.message(), result.success());
            if (result.success()) {
                reviewText.clear();
                refreshReviews.run();
                books.refresh();
            }
        });

        Button helpful = new Button("Mark Review Helpful");
        helpful.getStyleClass().add("secondary-button");
        helpful.setOnAction(e -> {
            BookReview selectedReview = reviews.getSelectionModel().getSelectedItem();
            if (selectedReview == null) { setMessage(msg, "Select a review first.", false); return; }
            DataStore.ActionResult result = dataStore.markReviewHelpful(selectedReview.getId(), currentUser.getUsername());
            setMessage(msg, result.message(), result.success());
            refreshReviews.run();
        });

        VBox right = new VBox(10, rating, new HBox(8, new Label("Sort"), sort, helpful), reviews, new Label("Your Review"), new HBox(8, new Label("Rating"), stars, anonymous), reviewText, submit, msg);
        right.setPrefWidth(430);
        HBox body = new HBox(12, books, right);
        HBox.setHgrow(books, Priority.ALWAYS);
        VBox box = new VBox(10, new Label("Book Reviews & Ratings"), body);
        box.setPadding(new Insets(14));
        return box;
    }

    private VBox buildBookRequestScreen() {
        TextField title = new TextField();
        title.setPromptText("Book title");
        TextField author = new TextField();
        author.setPromptText("Author");
        ComboBox<String> genre = new ComboBox<>();
        genre.getItems().addAll(dataStore.getGenreOptions());
        genre.setPromptText("Genre");
        TextArea reason = new TextArea();
        reason.setPromptText("Why should the library add this book?");
        reason.setPrefRowCount(4);
        Label msg = new Label();

        TableView<BookRequest> history = new TableView<>();
        history.setItems(FXCollections.observableArrayList(dataStore.getBookRequestsByUser(currentUser.getUsername())));
        history.getColumns().addAll(
                typedCol("Title", BookRequest::getTitle, 170),
                typedCol("Author", BookRequest::getAuthor, 140),
                typedCol("Genre", BookRequest::getGenre, 120),
                typedCol("Priority", BookRequest::getPriorityDisplay, 95),
                typedCol("Status", r -> r.getStatus().getDisplayName(), 100),
                typedCol("Submitted", r -> formatDateTime(r.getSubmittedAt()), 140),
                typedCol("Librarian Note", BookRequest::getLibrarianNote, 180)
        );
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        history.setPlaceholder(new Label("Your request history will appear here."));

        Button submit = new Button("Send Request");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(e -> {
            DataStore.ActionResult result = dataStore.requestNewBook(
                    currentUser.getUsername(),
                    title.getText(),
                    author.getText(),
                    genre.getValue(),
                    reason.getText()
            );
            setMessage(msg, result.message(), result.success());
            if (result.success()) {
                title.clear();
                author.clear();
                genre.getSelectionModel().clearSelection();
                reason.clear();
                history.setItems(FXCollections.observableArrayList(dataStore.getBookRequestsByUser(currentUser.getUsername())));
            }
        });

        GridPane form = formGrid();
        form.addRow(0, formLabel("Title"), title);
        form.addRow(1, formLabel("Author"), author);
        form.addRow(2, formLabel("Genre"), genre);
        form.addRow(3, formLabel("Reason"), reason);

        VBox formBox = new VBox(10, new Label("Request a New Book"), form, submit, msg);
        formBox.setPrefWidth(430);
        HBox body = new HBox(12, formBox, history);
        HBox.setHgrow(history, Priority.ALWAYS);
        VBox box = new VBox(10, body);
        box.setPadding(new Insets(14));
        return box;
    }

    private void showBookReader(Book book, boolean saveProgress) {
        // Check if borrowing period has expired before allowing reading
        if (saveProgress && book.getDueDate() != null && LocalDate.now().isAfter(book.getDueDate())) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Borrow period expired. The book has been auto-returned.");
            a.showAndWait();
            dataStore.autoReturnExpiredBooks();
            showDashboard(currentUser, "Dashboard");
            return;
        }

        // Validate PDF file path and existence
        String filePath = safe(book.getFilePath()).trim();
        if (filePath.isBlank() || !filePath.toLowerCase().endsWith(".pdf") || !Files.exists(Path.of(filePath))) {
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "This book does not have a readable local PDF file.\nPath: " + filePath);
            info.showAndWait();
            return;
        }

        // Render PDF pages using Apache PDFBox
        List<Image> pages = renderPdfPages(filePath);

        if (pages.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Unable to load this PDF.").showAndWait();
            return;
        }
        if (saveProgress) {
            dataStore.updateReadingProgress(currentUser.getUsername(), book.getId(), 1, pages.size());
        }

        // Retrieve saved bookmark for the user and book
        int savedPage = Math.max(1, dataStore.getBookmark(currentUser.getUsername(), book.getId()));
        int initialPage = Math.min(saveProgress ? savedPage : 1, pages.size());

        // Create PDF reader dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("PDF Reader - " + book.getTitle());
        dialog.setHeaderText(saveProgress
                ? "📌 Interactive Reading (Drag to highlight text, bookmark enabled)"
                : "👁️ Preview mode (read-only)");

        // Store interactive readers for each page
        Map<Integer, InteractivePDFReader> readerMap = new HashMap<>();
        
        // Current page index holder
        int[] currentPageIndex = {initialPage - 1};

        // Create interactive reader for initial page
        InteractivePDFReader currentReader = new InteractivePDFReader(pages.get(currentPageIndex[0]), initialPage);
        readerMap.put(initialPage, currentReader);
        loadHighlightsForPage(currentReader, initialPage, book.getId());

        // UI components for page indicator and highlights
        Label pageIndicator = new Label();
        Label selectionInfo = new Label("💡 Tip: Drag to select text on the page to highlight");
        selectionInfo.getStyleClass().add("muted-text");
        Label interactiveHighlightInfo = new Label();
        
        // Pagination control for navigating PDF pages
        Pagination pagination = new Pagination(pages.size(), currentPageIndex[0]);
        pagination.getStyleClass().add("pdf-pagination");
        pagination.setPageFactory(index -> {
            final int pageNum = index + 1;
            InteractivePDFReader reader = readerMap.getOrDefault(pageNum,
                new InteractivePDFReader(pages.get(index), pageNum));

            reader.setSelectionCallback((text, x, y, width, height) -> {
                if (saveProgress) {
                    dataStore.addInteractiveHighlight(currentUser.getUsername(), book.getId(), pageNum, x, y, width, height, text);
                    // Render immediately on the current page after drag release.
                    reader.addHighlight(new PDFHighlightManager.HighlightData(pageNum, x, y, width, height, text));
                    updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), pageNum);
                }
            });

            loadHighlightsForPage(reader, pageNum, book.getId());
            readerMap.put(pageNum, reader);
            currentPageIndex[0] = index;
            
            updatePageIndicator(pageIndicator, pageNum, pages.size());
            updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), pageNum);
            
            if (saveProgress) {
                dataStore.saveBookmark(currentUser.getUsername(), book.getId(), pageNum);
                dataStore.updateReadingProgress(currentUser.getUsername(), book.getId(), pageNum, pages.size());
            }
            
            return reader.getNode();
        });
        
        updatePageIndicator(pageIndicator, initialPage, pages.size());
        updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), initialPage);

        // Remove the hardcoded currentReader callback since factory handles it

        // Navigation buttons
        Button prev = new Button("◀ Previous");
        prev.getStyleClass().add("secondary-button");
        prev.setOnAction(e -> {
            int current = pagination.getCurrentPageIndex();
            if (current > 0) pagination.setCurrentPageIndex(current - 1);
        });

        Button next = new Button("Next ▶");
        next.getStyleClass().add("secondary-button");
        next.setOnAction(e -> {
            int current = pagination.getCurrentPageIndex();
            if (current < pages.size() - 1) pagination.setCurrentPageIndex(current + 1);
        });

        // Clear highlights button
        Button clearHighlights = new Button("🗑️ Clear Highlights");
        clearHighlights.getStyleClass().add("danger-button");
        clearHighlights.setDisable(!saveProgress);
        clearHighlights.setOnAction(e -> {
            if (new Alert(Alert.AlertType.CONFIRMATION, "Clear all highlights on this page?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            
            int page = pagination.getCurrentPageIndex() + 1;
            dataStore.clearPageInteractiveHighlights(currentUser.getUsername(), book.getId(), page);
            
            InteractivePDFReader reader = readerMap.get(page);
            if (reader != null) reader.clearHighlights();
            
            updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), page);
        });

        // Export highlights button (only in saveProgress mode)
        Button exportHighlights = new Button("💾 Export Highlights");
        exportHighlights.getStyleClass().add("secondary-button");
        exportHighlights.setDisable(!saveProgress);
        exportHighlights.setOnAction(e -> {
            List<PDFHighlightManager.HighlightData> allHighlights = 
                dataStore.getAllInteractiveHighlights(currentUser.getUsername(), book.getId());
            if (allHighlights.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No highlights to export.").showAndWait();
                return;
            }
            String summary = String.format("Total highlights: %d pages with highlights", 
                allHighlights.stream().map(h -> h.page).distinct().count());
            new Alert(Alert.AlertType.INFORMATION, summary).showAndWait();
        });

        // Toolbar with navigation and highlight controls
        HBox toolbar = new HBox(10, prev, next, pageIndicator, new Separator(Orientation.VERTICAL),
                clearHighlights, exportHighlights);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("pdf-toolbar");
        HBox.setHgrow(pageIndicator, Priority.ALWAYS);

        // Main content layout
        VBox content = new VBox(10, toolbar, selectionInfo, pagination, interactiveHighlightInfo);
        content.setPadding(new Insets(12));
        content.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        content.setMinSize(620, 420);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("pdf-reader-card");
        VBox.setVgrow(pagination, Priority.ALWAYS);

        dialog.setResizable(true);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // Start a background timer to check for borrowing period expiration during reading
        Timeline expirationChecker = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            if (saveProgress && book.getDueDate() != null && LocalDate.now().isAfter(book.getDueDate())) {
                dialog.getDialogPane().getScene().getWindow().hide();
                Alert a = new Alert(Alert.AlertType.WARNING, "Borrow period expired. The book has been auto-returned.");
                a.showAndWait();
                dataStore.autoReturnExpiredBooks();
                showDashboard(currentUser, "Dashboard");
            }
        }));
        expirationChecker.setCycleCount(Timeline.INDEFINITE);
        expirationChecker.play();

        dialog.showAndWait();

        // Stop the expiration checker after dialog closes
        expirationChecker.stop();

        // Save final bookmark state
        if (saveProgress) {
            int finalPage = pagination.getCurrentPageIndex() + 1;
            dataStore.saveBookmark(currentUser.getUsername(), book.getId(), finalPage);
            dataStore.updateReadingProgress(currentUser.getUsername(), book.getId(), finalPage, pages.size());
            saveSession("Reader", book.getId());
        }
    }

    private void loadHighlightsForPage(InteractivePDFReader reader, int pageNum, String bookId) {
        reader.clearHighlights();
        List<PDFHighlightManager.HighlightData> highlights = 
            dataStore.getInteractiveHighlights(currentUser.getUsername(), bookId, pageNum);
        for (PDFHighlightManager.HighlightData h : highlights) {
            reader.addHighlight(h);
        }
    }

    private void updateInteractiveHighlightInfo(Label label, String bookId, int pageNum) {
        List<PDFHighlightManager.HighlightData> highlights = 
            dataStore.getInteractiveHighlights(currentUser.getUsername(), bookId, pageNum);
        if (highlights.isEmpty()) {
            label.setText("✨ No highlights on this page");
        } else {
            label.setText(String.format("✨ Highlights on this page: %d", highlights.size()));
        }
        label.getStyleClass().setAll("muted-text");
    }

    private javafx.scene.Node buildPdfPageView(Image pageImage, int pageNumber, Label pageIndicator) {
        ImageView view = new ImageView(pageImage);
        view.setPreserveRatio(true);
        view.setFitWidth(820);
        view.getStyleClass().add("pdf-page-image");

        StackPane centered = new StackPane(view);
        centered.setPadding(new Insets(10));
        centered.getStyleClass().add("pdf-page-wrap");

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("page-scroll");

        pageIndicator.setText("Page " + pageNumber);
        return scroll;
    }

    private List<Image> renderPdfPages(String filePath) {
        List<Image> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(Path.of(filePath).toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int total = document.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                BufferedImage buffered = renderer.renderImageWithDPI(i, 130, ImageType.RGB);
                images.add(SwingFXUtils.toFXImage(buffered, null));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return images;
    }

    private void updatePageIndicator(Label label, int current, int total) {
        label.setText("Page " + current + " / " + total);
        label.getStyleClass().setAll("toolbar-chip");
    }

    private void updateHighlightInfo(Label label, String bookId) {
        Set<Integer> pages = dataStore.getHighlights(currentUser.getUsername(), bookId);
        String text = pages.isEmpty() ? "Highlighted pages: none" : "Highlighted pages: " + pages;
        label.setText(text);
        label.getStyleClass().setAll("muted-text");
    }

    private void updateTextHighlightInfo(Label label, String bookId) {
        List<String> snippets = dataStore.getTextHighlights(currentUser.getUsername(), bookId);
        if (snippets.isEmpty()) {
            label.setText("Text highlights: none");
        } else {
            int shown = Math.min(3, snippets.size());
            String latest = snippets.subList(snippets.size() - shown, snippets.size())
                    .stream()
                    .map(s -> "• " + s)
                    .collect(Collectors.joining("\n"));
            label.setText("Text highlights (latest " + shown + "):\n" + latest);
        }
        label.getStyleClass().setAll("muted-text");
    }

    private BorderPane buildAuthorDashboard() {
        Label title = new Label("Author Dashboard");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Publish books, generate summaries, monitor statistics, and respond to reader feedback.");
        subtitle.getStyleClass().add("muted-text");
        VBox titleBlock = new VBox(2, title, subtitle);

        VBox left = buildAuthorPublishForm();
        TableView<Book> table = buildAuthorSubmissionsTable();
        VBox actions = buildAuthorActions(table);
        Label multiSelectTip = new Label("Tip: Hold Ctrl (or Cmd on Mac) to select multiple books.");
        multiSelectTip.getStyleClass().add("muted-text");
        VBox right = new VBox(10, new Label("Published / Submitted Books"), table, multiSelectTip, actions);
        right.setPadding(new Insets(14));
        right.getStyleClass().add("card");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox bodyRow = new HBox(12, left, right);
        bodyRow.setPadding(new Insets(14));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Publish & Books", bodyRow),
                new Tab("Stats", buildAuthorStatsScreen()),
                new Tab("Reviews & Feedback", buildAuthorFeedbackScreen())
        );

        BorderPane pane = new BorderPane(tabs);
        pane.setTop(titleBlock);
        BorderPane.setMargin(titleBlock, new Insets(20, 20, 8, 20));
        return pane;
    }

    private VBox buildAuthorPublishForm() {
        Label t = new Label("Publish New Book");
        t.getStyleClass().add("card-title");

        TextField title = new TextField();
        TextArea summary = new TextArea();
        summary.setPrefRowCount(4);
        TextField filePath = new TextField();
        filePath.setEditable(false);
        TextField coverPath = new TextField();
        coverPath.setEditable(false);
        Label msg = new Label();
        ComboBox<String> summaryStyle = new ComboBox<>();
        summaryStyle.getItems().addAll("Short", "Medium", "Detailed");
        summaryStyle.getSelectionModel().select("Medium");

        FlowPane genrePane = new FlowPane(8, 8);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String g : dataStore.getGenreOptions()) {
            CheckBox cb = new CheckBox(g);
            genreChecks.add(cb);
            genrePane.getChildren().add(cb);
        }

        Button pickFile = new Button("Choose Book File");
        pickFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(stage);
            if (f != null) filePath.setText(f.getAbsolutePath());
        });

        Button pickCover = new Button("Upload Cover (Optional)");
        pickCover.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                if (f.length() > 3 * 1024 * 1024) {
                    coverPath.setText("");
                    new Alert(Alert.AlertType.ERROR, "Cover image too large (max 3MB).").showAndWait();
                    return;
                }
                coverPath.setText(f.getAbsolutePath());
            }
        });

        Button generateSummary = new Button("Generate Summary");
        generateSummary.getStyleClass().add("secondary-button");
        generateSummary.setOnAction(e -> {
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            summary.setText(SummaryService.generateSummary(title.getText(), currentUser.getFullName(), genres, filePath.getText(), summaryStyle.getValue()));
            setMessage(msg, "Summary generated. You can edit it before submission.", true);
        });

        Button refineSummary = new Button("Refine Summary");
        refineSummary.getStyleClass().add("secondary-button");
        refineSummary.setOnAction(e -> {
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            summary.setText(SummaryService.refineSummary(summary.getText(), title.getText(), genres));
            setMessage(msg, "Summary refined.", true);
        });

        Button finalizeSummary = new Button("Finalize Summary");
        finalizeSummary.getStyleClass().add("secondary-button");
        finalizeSummary.setOnAction(e -> {
            if (summary.getText().trim().isEmpty()) {
                setMessage(msg, "Generate or write a summary first.", false);
                return;
            }
            setMessage(msg, "Summary finalized for submission.", true);
        });

        AuthorDraft draft = dataStore.getDraft(currentUser.getUsername());
        if (draft != null) {
            title.setText(draft.getTitle());
            summary.setText(draft.getDescription());
            filePath.setText(draft.getFilePath());
            genreChecks.forEach(cb -> cb.setSelected(draft.getGenres().contains(cb.getText())));
        }

        Button saveDraft = new Button("Save Draft");
        saveDraft.setOnAction(e -> {
            AuthorDraft d = new AuthorDraft();
            d.setTitle(title.getText().trim());
            d.setDescription(summary.getText().trim());
            d.setFilePath(filePath.getText().trim());
            d.setGenres(genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList()));
            dataStore.saveDraft(currentUser.getUsername(), d);
            setMessage(msg, "Draft saved.", true);
        });

        Button submit = new Button("Submit for Approval");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(e -> {
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            DataStore.ActionResult r = dataStore.submitBook(
                    title.getText().trim(),
                    currentUser.getUsername(),
                    currentUser.getFullName(),
                    genres,
                    summary.getText().trim(),
                    filePath.getText().trim()
            );
            setMessage(msg, r.message(), r.success());
            if (r.success()) {
                title.clear(); summary.clear(); filePath.clear(); coverPath.clear(); genreChecks.forEach(cb -> cb.setSelected(false));
                dataStore.clearDraft(currentUser.getUsername());
                root.setCenter(buildAuthorDashboard());
            }
        });

        HBox fileRow = new HBox(8, filePath, pickFile);
        HBox.setHgrow(filePath, Priority.ALWAYS);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        HBox coverRow = new HBox(8, coverPath, pickCover);
        HBox.setHgrow(coverPath, Priority.ALWAYS);
        coverRow.setAlignment(Pos.CENTER_LEFT);

        GridPane g = formGrid();
        int r = 0;
        g.addRow(r++, formLabel("Title"), title);
        g.addRow(r++, formLabel("Genres"), genrePane);
        g.addRow(r++, formLabel("Summary Style"), summaryStyle);
        g.addRow(r++, formLabel("Summary"), summary);
        g.addRow(r++, formLabel("Book File"), fileRow);
        g.addRow(r++, formLabel("Cover Image"), coverRow);

        VBox box = new VBox(10, t, g, new HBox(8, generateSummary, refineSummary, finalizeSummary), new HBox(8, saveDraft, submit), msg);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("card");
        box.setPrefWidth(460);
        return box;
    }

    private TableView<Book> buildAuthorSubmissionsTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getBooksByAuthor(currentUser.getUsername()));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().addAll(
                col("Title", Book::getTitle, 170),
                col("Genre", Book::getGenre, 120),
                col("Status", b -> b.getStatus().getDisplayName(), 130),
                col("Submitted", b -> formatDate(b.getSubmittedDate()), 110)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildAuthorActions(TableView<Book> table) {
        Label msg = new Label();

        Button read = new Button("Read Selected");
        read.getStyleClass().add("secondary-button");
        read.setOnAction(e -> {
            Book b = table.getSelectionModel().getSelectedItem();
            if (b == null) { setMessage(msg, "Select one book.", false); return; }
            showBookReader(b, false);
        });

        Button edit = new Button("Edit Selected");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(e -> {
            Book b = table.getSelectionModel().getSelectedItem();
            if (b == null) { setMessage(msg, "Select one book.", false); return; }
            if (b.getStatus() == BookStatus.BORROWED) { setMessage(msg, "Borrowed books cannot be edited now.", false); return; }
            showAuthorEditDialog(b, msg);
        });

        Button delete = new Button("Delete Selected");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> {
            List<Book> selected = table.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) { setMessage(msg, "Select one or more books.", false); return; }

            List<Book> borrowedBooks = selected.stream()
                .filter(b -> b.getStatus() == org.example.model.BookStatus.BORROWED)
                .collect(Collectors.toList());
            if (!borrowedBooks.isEmpty()) {
                String borrowedRecordStr = borrowedBooks.stream().map(org.example.model.Book::getTitle).collect(Collectors.joining("\n- "));
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Cannot delete the following book(s) because they are currently borrowed by someone:\n- " + borrowedRecordStr).showAndWait();
                setMessage(msg, "Deletion cancelled. Some books are borrowed.", false);
                return;
            }

            if (new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.size() + " selected books?").showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            DataStore.ActionResult r = dataStore.bulkDeleteBooks(selected.stream().map(Book::getId).collect(Collectors.toList()), currentUser.getUsername());
            setMessage(msg, r.message(), r.success());
            root.setCenter(buildAuthorDashboard());
        });

        return new VBox(8, new HBox(8, read, edit, delete), msg);
    }

    private VBox buildAuthorStatsScreen() {
        List<Book> books = dataStore.getBooksByAuthorSnapshot(currentUser.getUsername());
        List<BookReview> reviews = dataStore.getReviewsForAuthor(currentUser.getUsername());
        List<ReadingHistory> histories = dataStore.getReadingHistoriesForAuthor(currentUser.getUsername());
        long published = books.stream()
                .filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE || book.getStatus() == BookStatus.BORROWED)
                .count();

        VBox publishedCard = statCard("Published Books", String.valueOf(published));
        VBox readsCard = statCard("Reads", String.valueOf(dataStore.getReadCountForAuthor(currentUser.getUsername())));
        VBox ratingCard = statCard("Average Rating", reviews.isEmpty() ? "n/a" : String.format(Locale.US, "%.1f/5", dataStore.getAverageRatingForAuthor(currentUser.getUsername())));
        VBox borrowCard = statCard("Borrow Count", String.valueOf(dataStore.getBorrowCountForAuthor(currentUser.getUsername())));
        VBox reviewCard = statCard("Reviews", String.valueOf(reviews.size()));
        HBox stats = new HBox(10, publishedCard, readsCard, ratingCard, borrowCard, reviewCard);

        CheckBox showPublished = new CheckBox("Published");
        CheckBox showReads = new CheckBox("Reads");
        CheckBox showRating = new CheckBox("Rating");
        CheckBox showBorrow = new CheckBox("Borrow Count");
        CheckBox showReviews = new CheckBox("Reviews");
        List<CheckBox> toggles = List.of(showPublished, showReads, showRating, showBorrow, showReviews);
        toggles.forEach(cb -> cb.setSelected(true));
        bindVisibility(publishedCard, showPublished);
        bindVisibility(readsCard, showReads);
        bindVisibility(ratingCard, showRating);
        bindVisibility(borrowCard, showBorrow);
        bindVisibility(reviewCard, showReviews);

        Button exportCsv = new Button("Export Stats CSV");
        exportCsv.getStyleClass().add("secondary-button");
        exportCsv.setOnAction(e -> exportAuthorStatsCsv(books, reviews, histories));
        Button exportPdf = new Button("Export Stats PDF");
        exportPdf.getStyleClass().add("secondary-button");
        exportPdf.setOnAction(e -> exportAuthorStatsPdf(books, reviews, histories));

        TableView<Book> table = new TableView<>(FXCollections.observableArrayList(books));
        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Status", b -> b.getStatus().getDisplayName(), 130),
                col("Borrow Count", Book::getBorrowCount, 110),
                col("Reads", b -> dataStore.getReadCountForBook(b.getId()), 90),
                col("Avg Rating", b -> ratingSummary(b.getId()), 120),
                col("Reviews", b -> dataStore.getReviewCount(b.getId()), 90)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        ComboBox<String> trendMode = new ComboBox<>();
        trendMode.getItems().addAll("Weekly", "Monthly");
        trendMode.getSelectionModel().selectFirst();
        BarChart<String, Number> trendChart = buildAuthorTrendChart(histories, trendMode.getValue());
        trendMode.valueProperty().addListener((a, b, c) -> refreshAuthorTrendChart(trendChart, histories, c));

        HBox charts = new HBox(12, buildAuthorBorrowBarChart(books), buildAuthorRatingPieChart(reviews), trendChart);
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(2), Priority.ALWAYS);
        charts.getStyleClass().add("chart-row");

        HBox dashboardControls = new HBox(8, showPublished, showReads, showRating, showBorrow, showReviews, new Label("Trend"), trendMode, exportCsv, exportPdf);
        dashboardControls.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, dashboardControls, stats, charts, table);
        box.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox buildAuthorFeedbackScreen() {
        List<Book> authoredBooks = dataStore.getBooksByAuthorSnapshot(currentUser.getUsername());
        Map<String, Book> booksById = authoredBooks.stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        ObservableList<BookReview> source = FXCollections.observableArrayList(dataStore.getReviewsForAuthor(currentUser.getUsername()));
        FilteredList<BookReview> filtered = new FilteredList<>(source, r -> true);

        ComboBox<String> bookFilter = new ComboBox<>();
        bookFilter.getItems().add("All Books");
        authoredBooks.stream().map(Book::getTitle).sorted().forEach(bookFilter.getItems()::add);
        bookFilter.getSelectionModel().selectFirst();
        CheckBox flaggedOnly = new CheckBox("Flagged only");

        Runnable apply = () -> filtered.setPredicate(review -> {
            if (review == null) return false;
            if (flaggedOnly.isSelected() && !review.isFlagged()) return false;
            String selected = bookFilter.getValue();
            if (selected != null && !"All Books".equals(selected)) {
                Book book = booksById.get(review.getBookId());
                return book != null && selected.equals(book.getTitle());
            }
            return true;
        });
        bookFilter.valueProperty().addListener((a, b, c) -> apply.run());
        flaggedOnly.selectedProperty().addListener((a, b, c) -> apply.run());

        ListView<BookReview> reviews = new ListView<>(filtered);
        reviews.setPlaceholder(new Label("Reviews for your books will appear here."));
        reviews.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(BookReview review, boolean empty) {
                super.updateItem(review, empty);
                if (empty || review == null) { setText(null); setGraphic(null); return; }
                Book book = booksById.get(review.getBookId());
                Label title = new Label(shortLabel(book == null ? "Unknown book" : book.getTitle(), 42) + " | " + stars(review.getRating()));
                title.getStyleClass().add("card-title");
                Label body = new Label(review.getDisplayName() + ": " + review.getReviewText());
                body.setWrapText(true);
                Label meta = new Label(formatDateTime(review.getSubmittedAt()) + " | Helpful: " + review.getHelpfulCount()
                        + " | " + review.getFlagDisplay() + " | Sentiment: " + SummaryService.analyzeSentiment(review.getReviewText()));
                meta.getStyleClass().add("muted-text");
                VBox cell = new VBox(4, title, body, meta);
                if (!review.getAuthorReply().isBlank()) {
                    Label reply = new Label("Author reply: " + review.getAuthorReply());
                    reply.setWrapText(true);
                    reply.getStyleClass().add("muted-text");
                    cell.getChildren().add(reply);
                }
                if (review.isFlagged()) {
                    Label flag = new Label("Flag reason: " + review.getFlagReason());
                    flag.setWrapText(true);
                    flag.getStyleClass().add("error-text");
                    cell.getChildren().add(flag);
                }
                setGraphic(cell);
            }
        });

        TextArea reply = new TextArea();
        reply.setPromptText("Write a reply to the selected review...");
        reply.setPrefRowCount(3);
        ComboBox<String> replyTemplate = new ComboBox<>();
        replyTemplate.getItems().addAll(
                "Thanks for reading and sharing your feedback.",
                "Thank you for the suggestion. I will consider it in future revisions.",
                "I appreciate the detailed comments and will use them to improve the book.",
                "Thanks for pointing this out. I will review the issue carefully."
        );
        replyTemplate.setPromptText("Quick reply template");
        replyTemplate.valueProperty().addListener((a, b, c) -> {
            if (c != null) reply.setText(c);
        });
        TextArea flagReason = new TextArea();
        flagReason.setPromptText("Reason for flagging inappropriate feedback...");
        flagReason.setPrefRowCount(2);
        Label msg = new Label();
        Label analytics = new Label(feedbackAnalytics(source));
        analytics.getStyleClass().add("muted-text");

        Runnable refresh = () -> {
            source.setAll(dataStore.getReviewsForAuthor(currentUser.getUsername()));
            apply.run();
            analytics.setText(feedbackAnalytics(source));
        };

        Button sendReply = new Button("Send Reply");
        sendReply.getStyleClass().add("primary-button");
        sendReply.setOnAction(e -> {
            BookReview selected = reviews.getSelectionModel().getSelectedItem();
            if (selected == null) { setMessage(msg, "Select a review first.", false); return; }
            DataStore.ActionResult result = dataStore.replyToReview(selected.getId(), currentUser.getUsername(), reply.getText());
            setMessage(msg, result.message(), result.success());
            if (result.success()) {
                reply.clear();
                refresh.run();
            }
        });

        Button flag = new Button("Flag Review");
        flag.getStyleClass().add("danger-button");
        flag.setOnAction(e -> {
            BookReview selected = reviews.getSelectionModel().getSelectedItem();
            if (selected == null) { setMessage(msg, "Select a review first.", false); return; }
            DataStore.ActionResult result = dataStore.flagReview(selected.getId(), currentUser.getUsername(), flagReason.getText());
            setMessage(msg, result.message(), result.success());
            if (result.success()) {
                flagReason.clear();
                refresh.run();
            }
        });

        HBox filters = new HBox(8, bookFilter, flaggedOnly);
        filters.setAlignment(Pos.CENTER_LEFT);
        VBox actions = new VBox(8, analytics, replyTemplate, reply, sendReply, flagReason, flag, msg);
        actions.setPrefWidth(430);
        HBox body = new HBox(12, reviews, actions);
        HBox.setHgrow(reviews, Priority.ALWAYS);
        VBox box = new VBox(10, filters, body);
        box.setPadding(new Insets(14));
        return box;
    }

    private void showAuthorEditDialog(Book book, Label msg) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Edit Book");
        TextField title = new TextField(book.getTitle());
        TextArea summary = new TextArea(book.getDescription());
        GridPane g = formGrid();
        g.addRow(0, formLabel("Title"), title);
        g.addRow(1, formLabel("Summary"), summary);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            DataStore.ActionResult r = dataStore.updateBook(book.getId(), title.getText().trim(), null, summary.getText().trim());
            setMessage(msg, r.message(), r.success());
            root.setCenter(buildAuthorDashboard());
        }
    }

    private BorderPane buildLibrarianDashboard() {
        Label title = new Label("Librarian Dashboard");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Review submissions, upload formatted books, process requests, and monitor borrowing.");
        subtitle.getStyleClass().add("muted-text");
        VBox titleBlock = new VBox(2, title, subtitle);

        TableView<Book> pendingTable = buildPendingTable();
        VBox approvalActions = buildApprovalActions(pendingTable);
        TableView<Book> publishedTable = buildPublishedBooksTable();
        VBox publishedActions = buildPublishedBookActions(publishedTable);

        // Add multi-selection tip for librarian
        Label multiSelectTip = new Label("Tip: Hold Ctrl or Shift to select multiple books.");
        multiSelectTip.getStyleClass().add("muted-text");
        VBox pendingTableWithTip = new VBox(4, pendingTable, multiSelectTip);

        TableView<User> usersTable = buildUsersTable();
        VBox userActions = buildUserActions(usersTable);

        TableView<BookRequest> requestTable = buildBookRequestsTable();
        VBox requestActions = buildBookRequestActions(requestTable);

        VBox p0 = card("Manage Published Books", publishedTable, publishedActions);
        VBox p1 = card("Pending Submissions", pendingTableWithTip, approvalActions);
        VBox p2 = buildLibrarianUploadForm();
        VBox p3 = card("Manage All Users", usersTable, userActions);
        VBox p4 = card("New Book Requests", requestTable, requestActions);
        VBox p5 = buildBorrowedRecordsPanel();
        VBox p6 = buildRequestInsightsPanel();
        VBox p7 = buildDownloadedBookStatsPanel();

        VBox body = new VBox(12, p0, p1, p2, p3, p4, p5, p6, p7);
        body.setPadding(new Insets(0, 20, 20, 20));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);

        BorderPane pane = new BorderPane(scroll);
        pane.setTop(titleBlock);
        BorderPane.setMargin(titleBlock, new Insets(20, 20, 8, 20));
        return pane;
    }

    private TableView<Book> buildPublishedBooksTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getPublishedBooks());
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Author", Book::getAuthorFullName, 150),
                col("Genre", Book::getGenre, 130),
                col("Rating", b -> ratingSummary(b.getId()), 120),
                col("Borrow Count", Book::getBorrowCount, 110),
                col("Status", b -> b.getStatus().getDisplayName(), 120)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No published books."));
        return table;
    }

    private VBox buildPublishedBookActions(TableView<Book> table) {
        TextField search = new TextField();
        search.setPromptText("Search published books...");
        ComboBox<String> genre = new ComboBox<>();
        genre.getItems().add("All");
        genre.getItems().addAll(dataStore.getGenreOptions());
        genre.getSelectionModel().selectFirst();
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("All", "Available", "Borrowed");
        status.getSelectionModel().selectFirst();
        Label msg = new Label();

        Runnable apply = () -> {
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            String selectedGenre = genre.getValue();
            String selectedStatus = status.getValue();
            table.setItems(FXCollections.observableArrayList(dataStore.getPublishedBooks().stream()
                    .filter(book -> q.isBlank()
                            || safe(book.getTitle()).toLowerCase().contains(q)
                            || safe(book.getAuthorFullName()).toLowerCase().contains(q)
                            || safe(book.getGenre()).toLowerCase().contains(q))
                    .filter(book -> "All".equals(selectedGenre)
                            || book.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(selectedGenre)))
                    .filter(book -> "All".equals(selectedStatus)
                            || ("Available".equals(selectedStatus) && book.getStatus() == BookStatus.APPROVED_AVAILABLE)
                            || ("Borrowed".equals(selectedStatus) && book.getStatus() == BookStatus.BORROWED))
                    .collect(Collectors.toList())));
        };
        search.textProperty().addListener((a, b, c) -> apply.run());
        genre.valueProperty().addListener((a, b, c) -> apply.run());
        status.valueProperty().addListener((a, b, c) -> apply.run());

        Button read = new Button("Read Selected");
        read.getStyleClass().add("secondary-button");
        read.setOnAction(e -> {
            Book book = table.getSelectionModel().getSelectedItem();
            if (book == null) { setMessage(msg, "Select a published book.", false); return; }
            showBookReader(book, false);
        });

        Button edit = new Button("Edit Details");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(e -> {
            Book book = table.getSelectionModel().getSelectedItem();
            if (book == null) { setMessage(msg, "Select a published book.", false); return; }
            showLibrarianEditBookDialog(book, msg);
        });

        Button delete = new Button("Delete Available");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> {
            List<Book> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) { setMessage(msg, "Select one or more books.", false); return; }
            if (new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.size() + " available published book(s)?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            int success = 0;
            List<String> failed = new ArrayList<>();
            for (Book book : selected) {
                DataStore.ActionResult result = dataStore.deletePublishedBookByLibrarian(book.getId());
                if (result.success()) success++;
                else failed.add(book.getTitle());
            }
            setMessage(msg, "Deleted " + success + " book(s). Failed: " + failed.size(), failed.isEmpty());
            table.setItems(dataStore.getPublishedBooks());
        });

        Button bulkEdit = new Button("Bulk Edit");
        bulkEdit.getStyleClass().add("secondary-button");
        bulkEdit.setOnAction(e -> {
            List<Book> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) { setMessage(msg, "Select books for bulk edit.", false); return; }
            showBulkEditPublishedBooksDialog(selected, msg);
        });

        Button versionHistory = new Button("Version History");
        versionHistory.getStyleClass().add("secondary-button");
        versionHistory.setOnAction(e -> {
            Book book = table.getSelectionModel().getSelectedItem();
            if (book == null) { setMessage(msg, "Select a published book.", false); return; }
            showBookVersionHistory(book);
        });

        HBox filters = new HBox(8, search, genre, status);
        HBox.setHgrow(search, Priority.ALWAYS);
        return new VBox(8, filters, new HBox(8, read, edit, bulkEdit, versionHistory, delete), msg);
    }

    private void showBulkEditPublishedBooksDialog(List<Book> selected, Label msg) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Bulk Edit Published Books");
        FlowPane genrePane = new FlowPane(8, 8);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String option : dataStore.getGenreOptions()) {
            CheckBox cb = new CheckBox(option);
            genreChecks.add(cb);
            genrePane.getChildren().add(cb);
        }
        TextArea description = new TextArea();
        description.setPromptText("Optional shared description. Leave blank to keep each current description.");
        description.setPrefRowCount(3);
        GridPane grid = formGrid();
        grid.addRow(0, formLabel("Genres"), genrePane);
        grid.addRow(1, formLabel("Description"), description);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (new Alert(Alert.AlertType.CONFIRMATION, "Apply bulk edit to " + selected.size() + " book(s)?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            DataStore.ActionResult result = dataStore.bulkUpdatePublishedBooksByLibrarian(
                    selected.stream().map(Book::getId).collect(Collectors.toList()),
                    genres,
                    description.getText());
            setMessage(msg, result.message(), result.success());
            root.setCenter(buildLibrarianDashboard());
        }
    }

    private void showBookVersionHistory(Book book) {
        List<String> history = dataStore.getBookVersionHistory(book.getId());
        String text = history.isEmpty() ? "No version history recorded yet." : String.join("\n", history);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text);
        alert.setTitle("Version History");
        alert.setHeaderText(book.getTitle());
        alert.showAndWait();
    }

    private void showLibrarianEditBookDialog(Book book, Label msg) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Published Book");
        TextField title = new TextField(book.getTitle());
        TextField author = new TextField(book.getAuthorFullName());
        TextArea description = new TextArea(book.getDescription());
        description.setPrefRowCount(4);
        TextField filePath = new TextField(safe(book.getFilePath()));
        TextField coverPath = new TextField(safe(book.getCoverPath()));
        filePath.setEditable(false);
        coverPath.setEditable(false);
        ComboBox<String> summaryStyle = new ComboBox<>();
        summaryStyle.getItems().addAll("Short", "Medium", "Detailed");
        summaryStyle.getSelectionModel().select("Medium");

        FlowPane genrePane = new FlowPane(8, 8);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String option : dataStore.getGenreOptions()) {
            CheckBox cb = new CheckBox(option);
            cb.setSelected(book.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(option)));
            genreChecks.add(cb);
            genrePane.getChildren().add(cb);
        }

        Button choosePdf = new Button("Choose PDF");
        choosePdf.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Replacement PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) filePath.setText(file.getAbsolutePath());
        });

        Button chooseCover = new Button("Choose Cover");
        chooseCover.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Cover Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) coverPath.setText(file.getAbsolutePath());
        });

        Button generate = new Button("Generate Description");
        generate.setOnAction(e -> {
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            description.setText(SummaryService.generateSummary(title.getText(), author.getText(), genres, filePath.getText(), summaryStyle.getValue()));
        });

        GridPane grid = formGrid();
        int row = 0;
        grid.addRow(row++, formLabel("Title"), title);
        grid.addRow(row++, formLabel("Author Names"), author);
        grid.addRow(row++, formLabel("Genres"), genrePane);
        grid.addRow(row++, formLabel("Summary Style"), summaryStyle);
        grid.addRow(row++, formLabel("Description"), description);
        HBox fileRow = new HBox(8, filePath, choosePdf);
        HBox.setHgrow(filePath, Priority.ALWAYS);
        grid.addRow(row++, formLabel("Book File"), fileRow);
        HBox coverRow = new HBox(8, coverPath, chooseCover);
        HBox.setHgrow(coverPath, Priority.ALWAYS);
        grid.addRow(row++, formLabel("Cover"), coverRow);

        dialog.getDialogPane().setContent(new VBox(10, grid, generate));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (new Alert(Alert.AlertType.CONFIRMATION, "Save changes to this published book?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            DataStore.ActionResult result = dataStore.updatePublishedBookByLibrarian(
                    book.getId(),
                    title.getText(),
                    author.getText(),
                    genres,
                    description.getText(),
                    filePath.getText(),
                    coverPath.getText());
            setMessage(msg, result.message(), result.success());
            root.setCenter(buildLibrarianDashboard());
        }
    }

    private TableView<Book> buildPendingTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getPendingBooks());
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Author", Book::getAuthorFullName, 150),
                col("Genre", Book::getGenre, 110),
                col("Submitted", b -> formatDate(b.getSubmittedDate()), 110),
                col("Status", b -> b.getStatus().getDisplayName(), 120)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        // If you want to show a tip, do it where this table is added to the UI, not here.
        return table;
    }

    private VBox buildApprovalActions(TableView<Book> table) {
        TextField search = new TextField();
        search.setPromptText("Search by title / author / genre");
        ComboBox<String> genre = new ComboBox<>();
        genre.getItems().add("All");
        genre.getItems().addAll(dataStore.getGenreOptions());
        genre.getSelectionModel().selectFirst();

        TextArea rejectionReason = new TextArea();
        rejectionReason.setPromptText("Rejection reason (required for better feedback)");
        rejectionReason.setPrefRowCount(2);

        Label msg = new Label();

        Runnable apply = () -> table.setItems(FXCollections.observableArrayList(
                dataStore.searchPendingBooks(search.getText(), genre.getValue(), BookStatus.PENDING_APPROVAL)
        ));
        search.textProperty().addListener((a, b, c) -> apply.run());
        genre.valueProperty().addListener((a, b, c) -> apply.run());

        Button approve = new Button("Approve Selected");
        approve.getStyleClass().add("primary-button");
        approve.setOnAction(e -> {
            List<Book> selected = table.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) { setMessage(msg, "Select at least one submission.", false); return; }
            DataStore.ActionResult r = dataStore.bulkReviewBooks(selected.stream().map(Book::getId).collect(Collectors.toList()), true, null);
            setMessage(msg, r.message(), r.success());
            root.setCenter(buildLibrarianDashboard());
        });

        Button reject = new Button("Reject Selected");
        reject.getStyleClass().add("danger-button");
        reject.setOnAction(e -> {
            List<Book> selected = table.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) { setMessage(msg, "Select at least one submission.", false); return; }
            if (rejectionReason.getText().trim().isEmpty()) { setMessage(msg, "Please provide rejection reason.", false); return; }
            DataStore.ActionResult r = dataStore.bulkReviewBooks(selected.stream().map(Book::getId).collect(Collectors.toList()), false, rejectionReason.getText().trim());
            setMessage(msg, r.message(), r.success());
            root.setCenter(buildLibrarianDashboard());
        });

        HBox top = new HBox(8, search, genre);
        HBox.setHgrow(search, Priority.ALWAYS);
        return new VBox(8, top, rejectionReason, new HBox(8, approve, reject), msg);
    }

    private VBox buildLibrarianUploadForm() {
        TextField title = new TextField();
        title.setPromptText("Required title");
        TextField author = new TextField();
        author.setPromptText("Required author name(s)");
        TextArea description = new TextArea();
        description.setPromptText("Required description / summary");
        description.setPrefRowCount(3);
        TextField filePath = new TextField();
        filePath.setEditable(false);
        filePath.setPromptText("Required PDF file");
        TextField coverPath = new TextField();
        coverPath.setEditable(false);
        coverPath.setPromptText("Optional PNG/JPG cover");
        ComboBox<String> summaryStyle = new ComboBox<>();
        summaryStyle.getItems().addAll("Short", "Medium", "Detailed");
        summaryStyle.getSelectionModel().select("Medium");

        FlowPane genrePane = new FlowPane(8, 8);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String genre : dataStore.getGenreOptions()) {
            CheckBox cb = new CheckBox(genre);
            cb.getStyleClass().add("chip-check");
            genreChecks.add(cb);
            genrePane.getChildren().add(cb);
        }

        Button pickPdf = new Button("Choose PDF");
        pickPdf.getStyleClass().add("secondary-button");
        pickPdf.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Book PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) filePath.setText(file.getAbsolutePath());
        });

        Button pickCover = new Button("Choose Cover");
        pickCover.getStyleClass().add("secondary-button");
        pickCover.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Cover Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                String lower = file.getName().toLowerCase();
                if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                    new Alert(Alert.AlertType.ERROR, "Cover must be PNG/JPG/JPEG.").showAndWait();
                    return;
                }
                if (file.length() > 3L * 1024 * 1024) {
                    new Alert(Alert.AlertType.ERROR, "Cover image must be <= 3MB.").showAndWait();
                    return;
                }
                coverPath.setText(file.getAbsolutePath());
            }
        });

        Label msg = new Label();
        Button generateDescription = new Button("Generate Description");
        generateDescription.getStyleClass().add("secondary-button");
        generateDescription.setOnAction(e -> {
            List<String> genres = genreChecks.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());
            description.setText(SummaryService.generateSummary(title.getText(), author.getText(), genres, filePath.getText(), summaryStyle.getValue()));
            setMessage(msg, "Description generated. Review and edit before upload.", true);
        });

        Button upload = new Button("Upload & Publish");
        upload.getStyleClass().add("primary-button");
        upload.setOnAction(e -> {
            List<String> genres = genreChecks.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());
            String path = filePath.getText().trim();
            if (!path.toLowerCase().endsWith(".pdf")) {
                setMessage(msg, "Book file must be a PDF.", false);
                return;
            }
            if (new Alert(Alert.AlertType.CONFIRMATION, "Upload and publish this book now?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            DataStore.ActionResult result = dataStore.addLibrarianBook(
                    title.getText(),
                    author.getText(),
                    genres,
                    description.getText(),
                    path,
                    coverPath.getText().trim(),
                    currentUser.getUsername());
            setMessage(msg, result.message(), result.success());
            if (result.success()) {
                title.clear();
                author.clear();
                description.clear();
                filePath.clear();
                coverPath.clear();
                genreChecks.forEach(cb -> cb.setSelected(false));
            }
        });

        GridPane form = formGrid();
        int row = 0;
        form.addRow(row++, formLabel("Title"), title);
        form.addRow(row++, formLabel("Author Names"), author);
        form.addRow(row++, formLabel("Genres"), genrePane);
        form.addRow(row++, formLabel("Summary Style"), summaryStyle);
        form.addRow(row++, formLabel("Description"), description);
        HBox fileRow = new HBox(8, filePath, pickPdf);
        HBox.setHgrow(filePath, Priority.ALWAYS);
        form.addRow(row++, formLabel("File Upload"), fileRow);
        HBox coverRow = new HBox(8, coverPath, pickCover);
        HBox.setHgrow(coverPath, Priority.ALWAYS);
        form.addRow(row++, formLabel("Cover Upload"), coverRow);

        Label note = new Label("Required format: Title, Author Names, Genre, Description, PDF File, optional Cover Upload.");
        note.getStyleClass().add("muted-text");
        return card("Upload Library Book", note, form, new HBox(8, generateDescription, upload), msg);
    }

    private TableView<User> buildUsersTable() {
        TableView<User> table = new TableView<>();
        table.setItems(dataStore.getAllUsers());
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().addAll(
                userCol("Username", User::getUsername, 120),
                userCol("Full Name", User::getFullName, 160),
                userCol("Role", u -> u.getRole().getDisplayName(), 100),
                userCol("Active", u -> u.isActive() ? "Yes" : "No", 80),
                userCol("Last Login", u -> formatDateTime(u.getLastLogin()), 140),
                userCol("Borrowed Now", u -> String.valueOf(dataStore.getCurrentBorrowedCountForUser(u.getUsername())), 110),
                userCol("Total Reads", u -> String.valueOf(dataStore.getTotalReadCountForUser(u.getUsername())), 100),
                userCol("Bio", u -> safe(u.getBio()), 150),
                userCol("Employee ID", u -> safe(u.getEmployeeId()), 110)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildUserActions(TableView<User> table) {
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Search users...");
        ComboBox<String> roleFilter = new ComboBox<>();
        roleFilter.getItems().add("All Roles");
        Arrays.stream(Role.values()).map(Role::getDisplayName).forEach(roleFilter.getItems()::add);
        roleFilter.getSelectionModel().selectFirst();

        Runnable apply = () -> {
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            String role = roleFilter.getValue();
            table.setItems(FXCollections.observableArrayList(dataStore.getAllUsers().stream()
                    .filter(user -> q.isBlank()
                            || safe(user.getUsername()).toLowerCase().contains(q)
                            || safe(user.getFullName()).toLowerCase().contains(q))
                    .filter(user -> "All Roles".equals(role) || user.getRole().getDisplayName().equals(role))
                    .collect(Collectors.toList())));
        };
        search.textProperty().addListener((a, b, c) -> apply.run());
        roleFilter.valueProperty().addListener((a, b, c) -> apply.run());

        Button add = new Button("Add New User");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> showAddUserDialog(msg));

        Button edit = new Button("Edit User");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(e -> {
            User user = table.getSelectionModel().getSelectedItem();
            if (user == null) { setMessage(msg, "Select a user first.", false); return; }
            showEditUserDialog(user, msg);
        });

        Button toggle = new Button("Deactivate / Activate");
        toggle.getStyleClass().add("secondary-button");
        toggle.setOnAction(e -> {
            User user = table.getSelectionModel().getSelectedItem();
            if (user == null) { setMessage(msg, "Select a user first.", false); return; }
            if (user.getUsername().equals(currentUser.getUsername())) { setMessage(msg, "Cannot change your own status.", false); return; }
            DataStore.ActionResult r = dataStore.toggleUserStatus(user.getUsername());
            setMessage(msg, r.message(), r.success());
            table.refresh();
        });

        Button bulkActivate = new Button("Bulk Activate");
        bulkActivate.getStyleClass().add("secondary-button");
        bulkActivate.setOnAction(e -> {
            List<User> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) { setMessage(msg, "Select users first.", false); return; }
            DataStore.ActionResult result = dataStore.bulkSetUserStatus(
                    selected.stream().map(User::getUsername).collect(Collectors.toList()), true, currentUser.getUsername());
            setMessage(msg, result.message(), result.success());
            apply.run();
        });

        Button bulkDeactivate = new Button("Bulk Deactivate");
        bulkDeactivate.getStyleClass().add("danger-button");
        bulkDeactivate.setOnAction(e -> {
            List<User> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) { setMessage(msg, "Select users first.", false); return; }
            DataStore.ActionResult result = dataStore.bulkSetUserStatus(
                    selected.stream().map(User::getUsername).collect(Collectors.toList()), false, currentUser.getUsername());
            setMessage(msg, result.message(), result.success());
            apply.run();
        });

        HBox filters = new HBox(8, search, roleFilter);
        HBox.setHgrow(search, Priority.ALWAYS);
        return new VBox(8, filters, new HBox(8, add, edit, toggle, bulkActivate, bulkDeactivate), msg);
    }

    private TableView<BookRequest> buildBookRequestsTable() {
        TableView<BookRequest> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
        table.getColumns().addAll(
                typedCol("Title", BookRequest::getTitle, 170),
                typedCol("Author", BookRequest::getAuthor, 140),
                typedCol("Genre", BookRequest::getGenre, 110),
                typedCol("Requester", BookRequest::getRequesterUsername, 120),
                typedCol("Priority", BookRequest::getPriorityDisplay, 95),
                typedCol("Status", r -> r.getStatus().getDisplayName(), 100),
                typedCol("Submitted", r -> formatDateTime(r.getSubmittedAt()), 140),
                typedCol("Downloaded File", r -> shortLabel(r.getDownloadedFilePath(), 24), 140),
                typedCol("Reason", BookRequest::getReason, 220)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No new book requests."));
        return table;
    }

    private VBox buildBookRequestActions(TableView<BookRequest> table) {
        Label msg = new Label();
        TextField sourceUrl = new TextField();
        sourceUrl.setPromptText("Direct PDF/TXT URL or webpage URL to crawl...");
        TextArea note = new TextArea();
        note.setPromptText("Librarian note or rejection reason...");
        note.setPrefRowCount(2);
        ProgressBar downloadProgress = new ProgressBar(0);
        downloadProgress.setPrefWidth(220);
        downloadProgress.setVisible(false);
        downloadProgress.setManaged(false);

        Button approve = new Button("Approve Request");
        approve.getStyleClass().add("primary-button");
        approve.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            DataStore.ActionResult result = dataStore.approveBookRequest(request.getId(), note.getText());
            setMessage(msg, result.message(), result.success());
            table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
        });

        Button reject = new Button("Reject Request");
        reject.getStyleClass().add("danger-button");
        reject.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            DataStore.ActionResult result = dataStore.rejectBookRequest(request.getId(), note.getText());
            setMessage(msg, result.message(), result.success());
            table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
        });

        Button download = new Button("Download Source");
        download.getStyleClass().add("secondary-button");
        download.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            download.setDisable(true);
            downloadProgress.setVisible(true);
            downloadProgress.setManaged(true);
            downloadProgress.setProgress(-1);
            Task<RequestedBookDownloader.DownloadResult> task = new Task<>() {
                @Override
                protected RequestedBookDownloader.DownloadResult call() {
                    return RequestedBookDownloader.download(sourceUrl.getText(), request);
                }
            };
            task.setOnSucceeded(done -> {
                RequestedBookDownloader.DownloadResult downloadResult = task.getValue();
                download.setDisable(false);
                downloadProgress.setProgress(1);
                if (!downloadResult.success()) {
                    setMessage(msg, downloadResult.message(), false);
                    return;
                }
                DataStore.ActionResult mark = dataStore.markBookRequestDownloaded(
                        request.getId(),
                        downloadResult.filePath().toString(),
                        note.getText().isBlank() ? downloadResult.message() : note.getText());
                setMessage(msg, mark.success() ? downloadResult.message() + " " + mark.message() : mark.message(), mark.success());
                table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
            });
            task.setOnFailed(done -> {
                download.setDisable(false);
                downloadProgress.setVisible(false);
                downloadProgress.setManaged(false);
                setMessage(msg, "Download failed: " + task.getException().getMessage(), false);
            });
            new Thread(task, "requested-book-download").start();
        });

        Button upload = new Button("Upload Fulfilled Book");
        upload.getStyleClass().add("primary-button");
        upload.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            showRequestUploadDialog(request, msg);
            table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
        });

        Button priority = new Button("Toggle Urgent");
        priority.getStyleClass().add("secondary-button");
        priority.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            DataStore.ActionResult result = dataStore.toggleBookRequestPriority(request.getId());
            setMessage(msg, result.message(), result.success());
            table.setItems(FXCollections.observableArrayList(dataStore.getAllBookRequests()));
        });

        Button suggest = new Button("Suggest Alternatives");
        suggest.getStyleClass().add("secondary-button");
        suggest.setOnAction(e -> {
            BookRequest request = table.getSelectionModel().getSelectedItem();
            if (request == null) { setMessage(msg, "Select a request.", false); return; }
            List<Book> alternatives = dataStore.findSimilarBooksForRequest(request, 5);
            if (alternatives.isEmpty()) {
                setMessage(msg, "No similar titles found.", false);
                return;
            }
            String titles = alternatives.stream().map(book -> book.getTitle() + " by " + book.getAuthorFullName()).collect(Collectors.joining("\n"));
            Alert alert = new Alert(Alert.AlertType.INFORMATION, titles);
            alert.setTitle("Similar Books");
            alert.setHeaderText("Available alternatives for " + request.getTitle());
            alert.showAndWait();
            DataStore.ActionResult result = dataStore.notifySimilarBooksForRequest(request.getId(), alternatives);
            setMessage(msg, result.message(), result.success());
        });

        HBox.setHgrow(sourceUrl, Priority.ALWAYS);
        return new VBox(8, sourceUrl, note, new HBox(8, approve, reject, priority, suggest), new HBox(8, download, upload, downloadProgress), msg);
    }

    private void showRequestUploadDialog(BookRequest request, Label msg) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Upload Requested Book");
        TextField title = new TextField(request.getTitle());
        TextField author = new TextField(request.getAuthor());
        TextArea description = new TextArea();
        description.setPrefRowCount(4);
        TextField filePath = new TextField(request.getDownloadedFilePath());
        filePath.setEditable(false);
        TextField coverPath = new TextField();
        coverPath.setEditable(false);
        TextField fulfillmentNote = new TextField(request.getLibrarianNote());
        ComboBox<String> summaryStyle = new ComboBox<>();
        summaryStyle.getItems().addAll("Short", "Medium", "Detailed");
        summaryStyle.getSelectionModel().select("Medium");

        FlowPane genrePane = new FlowPane(8, 8);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String option : dataStore.getGenreOptions()) {
            CheckBox cb = new CheckBox(option);
            cb.setSelected(option.equalsIgnoreCase(request.getGenre()));
            genreChecks.add(cb);
            genrePane.getChildren().add(cb);
        }

        Button choosePdf = new Button("Choose PDF");
        choosePdf.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Fulfilled Book PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) filePath.setText(file.getAbsolutePath());
        });

        Button chooseCover = new Button("Choose Cover");
        chooseCover.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Cover Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) coverPath.setText(file.getAbsolutePath());
        });

        Button generate = new Button("Generate Description");
        generate.setOnAction(e -> {
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            String generated = SummaryService.generateSummary(title.getText(), author.getText(), genres, filePath.getText(), summaryStyle.getValue());
            description.setText(generated + "\n\nRequest reason: " + request.getReason());
        });
        generate.fire();

        GridPane grid = formGrid();
        int row = 0;
        grid.addRow(row++, formLabel("Title"), title);
        grid.addRow(row++, formLabel("Author Names"), author);
        grid.addRow(row++, formLabel("Genres"), genrePane);
        grid.addRow(row++, formLabel("Summary Style"), summaryStyle);
        grid.addRow(row++, formLabel("Description"), description);
        HBox fileRow = new HBox(8, filePath, choosePdf);
        HBox.setHgrow(filePath, Priority.ALWAYS);
        grid.addRow(row++, formLabel("Book File"), fileRow);
        HBox coverRow = new HBox(8, coverPath, chooseCover);
        HBox.setHgrow(coverPath, Priority.ALWAYS);
        grid.addRow(row++, formLabel("Cover"), coverRow);
        grid.addRow(row, formLabel("Fulfillment Note"), fulfillmentNote);

        dialog.getDialogPane().setContent(new VBox(10, grid, generate));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (new Alert(Alert.AlertType.CONFIRMATION, "Upload this requested book and notify the requester?")
                    .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            List<String> genres = genreChecks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            DataStore.ActionResult result = dataStore.uploadBookForRequest(
                    request.getId(),
                    title.getText(),
                    author.getText(),
                    genres,
                    description.getText(),
                    filePath.getText(),
                    coverPath.getText(),
                    currentUser.getUsername(),
                    fulfillmentNote.getText());
            setMessage(msg, result.message(), result.success());
            root.setCenter(buildLibrarianDashboard());
        }
    }

    private VBox buildRequestInsightsPanel() {
        Map<String, Long> byStatus = dataStore.getBookRequestStatusCounts();
        Map<String, Long> byGenre = dataStore.getBookRequestGenreCounts();
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        Label summary = new Label("Total requests: " + total + " | Pending priority is shown at the top of the request queue.");
        summary.getStyleClass().add("muted-text");
        HBox charts = new HBox(12, pieChartFromMap("Request Status", byStatus), barChartFromMap("Requested Genres", byGenre));
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);
        return card("Request Analytics", summary, charts);
    }

    private VBox buildDownloadedBookStatsPanel() {
        List<Book> books = dataStore.getFulfilledRequestBooks();
        int reads = books.stream().mapToInt(book -> dataStore.getReadCountForBook(book.getId())).sum();
        int reviews = books.stream().mapToInt(book -> dataStore.getReviewCount(book.getId())).sum();
        int borrows = books.stream().mapToInt(Book::getBorrowCount).sum();
        HBox stats = new HBox(10,
                statCard("Downloaded Books", String.valueOf(books.size())),
                statCard("Reads", String.valueOf(reads)),
                statCard("Borrow Count", String.valueOf(borrows)),
                statCard("Reviews", String.valueOf(reviews))
        );
        TableView<Book> table = new TableView<>(FXCollections.observableArrayList(books));
        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Author", Book::getAuthorFullName, 150),
                col("Genre", Book::getGenre, 140),
                col("Reads", b -> dataStore.getReadCountForBook(b.getId()), 90),
                col("Rating", b -> ratingSummary(b.getId()), 120),
                col("Borrow Count", Book::getBorrowCount, 110)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return card("Downloaded Book Stats", stats, table);
    }

    private void showAddUserDialog(Label msg) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Add New User");
        TextField username = new TextField();
        TextField fullName = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> role = new ComboBox<>();
        role.getItems().addAll(Role.values());
        role.getSelectionModel().selectFirst();

        GridPane g = formGrid();
        g.addRow(0, formLabel("Username"), username);
        g.addRow(1, formLabel("Full Name"), fullName);
        g.addRow(2, formLabel("Password"), password);
        g.addRow(3, formLabel("Role"), role);

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            DataStore.RegistrationResult r = dataStore.registerUser(username.getText().trim(), fullName.getText().trim(), password.getText(), role.getValue(), "", "");
            if (r.success()) {
                setMessage(msg, r.message(), true);
                root.setCenter(buildLibrarianDashboard());
            } else {
                new Alert(Alert.AlertType.ERROR, r.message()).showAndWait();
            }
        }
    }

    private void showEditUserDialog(User user, Label msg) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Edit User: " + user.getUsername());
        TextField fullName = new TextField(user.getFullName());
        TextField bio = new TextField(safe(user.getBio()));
        TextField employee = new TextField(safe(user.getEmployeeId()));

        GridPane g = formGrid();
        int r = 0;
        g.addRow(r++, formLabel("Full Name"), fullName);
        if (user.getRole() == Role.AUTHOR) {
            g.addRow(r++, formLabel("Bio"), bio);
        }
        if (user.getRole() == Role.LIBRARIAN) {
            g.addRow(r++, formLabel("Employee ID"), employee);
        }

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            DataStore.ActionResult result = dataStore.updateUserProfile(user, fullName.getText().trim(), "", bio.getText().trim(), employee.getText().trim());
            setMessage(msg, result.message(), result.success());
            root.setCenter(buildLibrarianDashboard());
        }
    }

    private TableView<Book> buildAllBorrowedBooksTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getAllBorrowedBooks());
        table.getColumns().addAll(
                col("Book Title", Book::getTitle, 180),
                col("Borrower Username", Book::getBorrowedBy, 160),
                col("Borrow Date", b -> formatDate(b.getBorrowedDate()), 120),
                col("Due Date", b -> formatDate(b.getDueDate()), 120),
                col("Status", b -> b.getStatus().getDisplayName(), 120)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildBorrowedRecordsPanel() {
        ObservableList<ReadingHistory> source = FXCollections.observableArrayList(dataStore.getAllReadingHistories());
        FilteredList<ReadingHistory> filtered = new FilteredList<>(source, h -> true);
        TableView<ReadingHistory> table = new TableView<>(filtered);
        table.getColumns().addAll(
                typedCol("Book Title", ReadingHistory::getBookTitle, 180),
                typedCol("Borrower", ReadingHistory::getUsername, 120),
                typedCol("Author", ReadingHistory::getAuthor, 140),
                typedCol("Borrow Date", h -> formatDate(h.getBorrowDate()), 110),
                typedCol("Due Date", h -> formatDate(h.getBorrowDate().plusDays(dataStore.getMaxBorrowDays())), 110),
                typedCol("Return Date", h -> formatDate(h.getReturnDate()), 110),
                typedCol("Status", this::borrowingRecordStatus, 100),
                typedCol("Duration", h -> h.getReadingDurationDays() + " day(s)", 100)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setRowFactory(v -> new TableRow<>() {
            @Override
            protected void updateItem(ReadingHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (isOverdue(item)) {
                    setStyle("-fx-background-color: #ffe5e5;");
                } else {
                    setStyle("");
                }
            }
        });

        TextField search = new TextField();
        search.setPromptText("Search title / borrower / author...");
        ComboBox<String> mode = new ComboBox<>();
        mode.getItems().addAll("All", "Active", "Returned", "Overdue");
        mode.getSelectionModel().selectFirst();
        Label msg = new Label();

        Runnable apply = () -> filtered.setPredicate(history -> {
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            if (!q.isBlank()
                    && !safe(history.getBookTitle()).toLowerCase().contains(q)
                    && !safe(history.getUsername()).toLowerCase().contains(q)
                    && !safe(history.getAuthor()).toLowerCase().contains(q)) return false;
            String selected = mode.getValue();
            if ("Active".equals(selected) && history.getReturnDate() != null) return false;
            if ("Returned".equals(selected) && history.getReturnDate() == null) return false;
            if ("Overdue".equals(selected) && !isOverdue(history)) return false;
            return true;
        });
        search.textProperty().addListener((a, b, c) -> apply.run());
        mode.valueProperty().addListener((a, b, c) -> apply.run());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> {
            source.setAll(dataStore.getAllReadingHistories());
            apply.run();
            setMessage(msg, "Records refreshed.", true);
        });

        Button export = new Button("Export Excel CSV");
        export.getStyleClass().add("secondary-button");
        export.setOnAction(e -> exportBorrowedRecordsCsv(new ArrayList<>(filtered)));

        HBox filters = new HBox(8, search, mode, refresh, export);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox box = card("Borrowed Books Record", filters, table, msg);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private BorderPane buildProfileScreen() {
        Label title = new Label("Manage Profile");
        title.getStyleClass().add("section-title");

        Label username = new Label(currentUser.getUsername());
        TextField fullName = new TextField(currentUser.getFullName());
        PasswordField currentPass = new PasswordField();
        currentPass.setPromptText("Re-enter current password to save any changes");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Leave empty to keep current password");

        Label strength = new Label();
        newPass.textProperty().addListener((a, b, c) -> strength.setText(passwordStrength(c)));

        TextField bio = new TextField(currentUser.getRole() == Role.AUTHOR ? safe(currentUser.getBio()) : "");
        TextField employee = new TextField(currentUser.getRole() == Role.LIBRARIAN ? safe(currentUser.getEmployeeId()) : "");

        TextField avatarPath = new TextField(safe(currentUser.getAvatarPath()));
        avatarPath.setEditable(false);
        Button uploadAvatar = new Button("Upload Avatar (Optional)");
        uploadAvatar.getStyleClass().add("secondary-button");
        uploadAvatar.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Avatar Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                long maxBytes = 2L * 1024 * 1024;
                String lower = file.getName().toLowerCase();
                boolean okType = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                if (!okType) {
                    new Alert(Alert.AlertType.ERROR, "Avatar format must be PNG/JPG/JPEG.").showAndWait();
                    return;
                }
                if (file.length() > maxBytes) {
                    new Alert(Alert.AlertType.ERROR, "Avatar size must be <= 2MB.").showAndWait();
                    return;
                }
                avatarPath.setText(file.getAbsolutePath());
            }
        });
        HBox avatarRow = new HBox(8, avatarPath, uploadAvatar);
        HBox.setHgrow(avatarPath, Priority.ALWAYS);

        GridPane g = formGrid();
        int r = 0;
        g.addRow(r++, formLabel("Username"), username);
        g.addRow(r++, formLabel("Full Name"), fullName);
        g.addRow(r++, formLabel("Current Password"), currentPass);
        g.addRow(r++, formLabel("New Password"), newPass);
        g.addRow(r++, formLabel("Password Strength"), strength);
        if (currentUser.getRole() == Role.AUTHOR) {
            g.addRow(r++, formLabel("Bio"), bio);
        }
        g.addRow(r++, formLabel("Profile Picture"), avatarRow);
        if (currentUser.getRole() == Role.LIBRARIAN) g.addRow(r++, formLabel("Employee ID"), employee);

        Label msg = new Label();
        Button save = new Button("Save Profile");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> {
            User verified = dataStore.authenticate(currentUser.getUsername(), currentPass.getText(), currentUser.getRole());
            if (verified == null) {
                setMessage(msg, "Current password is incorrect.", false);
                return;
            }
            boolean passwordChanged = !newPass.getText().isBlank();
            DataStore.ActionResult result = dataStore.updateUserProfile(currentUser,
                    fullName.getText().trim(),
                    newPass.getText(),
                    bio.getText().trim(),
                    employee.getText().trim());
            if (result.success()) {
                currentUser.setAvatarPath(avatarPath.getText().trim());
                dataStore.saveData();
                refreshHeaderAvatar();
            }
            setMessage(msg, result.message(), result.success());

            if (result.success() && passwordChanged) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Password changed. You will be logged out now.");
                a.showAndWait();
                currentUser = null;
                root.setCenter(buildLanding());
            }
        });

        VBox body = new VBox(14, g, save, msg);
        body.setPadding(new Insets(20));
        body.getStyleClass().add("card");
        body.setMaxWidth(700);

        BorderPane pane = new BorderPane(body);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
        BorderPane.setMargin(body, new Insets(0, 20, 20, 20));
        return pane;
    }

    private BorderPane buildNotificationScreen() {
        Label title = new Label("Notification Board");
        title.getStyleClass().add("section-title");

        ObservableList<Notification> source = FXCollections.observableArrayList(dataStore.getNotificationsForUser(currentUser.getUsername(), true));
        FilteredList<Notification> filtered = new FilteredList<>(source, n -> true);

        Label unread = new Label("Unread: " + dataStore.getUnreadNotificationCount(currentUser.getUsername()));
        unread.getStyleClass().add("card-title");

        TextField search = new TextField();
        search.setPromptText("Search notifications...");
        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("All", "Due Reminder", "Book Deletion", "Book Approval", "Book Rejection", "Book Request", "Submission", "Account", "General", "Borrowing");
        category.getSelectionModel().selectFirst();
        ComboBox<String> showMode = new ComboBox<>();
        showMode.getItems().addAll("Active", "Archived", "All");
        showMode.getSelectionModel().selectFirst();

        Runnable apply = () -> filtered.setPredicate(n -> {
            String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
            if (!q.isBlank() && !safe(n.getMessage()).toLowerCase().contains(q)) return false;
            if (!"All".equals(category.getValue()) && !category.getValue().equalsIgnoreCase(n.getCategory())) return false;
            if ("Active".equals(showMode.getValue()) && n.isArchived()) return false;
            if ("Archived".equals(showMode.getValue()) && !n.isArchived()) return false;
            return true;
        });
        search.textProperty().addListener((a, b, c) -> apply.run());
        category.valueProperty().addListener((a, b, c) -> apply.run());
        showMode.valueProperty().addListener((a, b, c) -> apply.run());

        ListView<Notification> list = new ListView<>(filtered);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Notification n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); setGraphic(null); setStyle(""); return; }
                Label m = new Label((n.isRead() ? "" : "[UNREAD] ") + n.getMessage());
                m.setWrapText(true);
                Label meta = new Label(n.getCategory() + " | " + n.getUrgency() + " | " + formatDateTime(n.getTimestamp()));
                meta.getStyleClass().add("muted-text");
                if ("High".equalsIgnoreCase(n.getUrgency())) {
                    setStyle("-fx-background-color: #fff0d9; -fx-border-color: #e5c879; -fx-border-width: 0 0 1 0;");
                    m.getStyleClass().add("card-title");
                } else {
                    setStyle("");
                }
                setGraphic(new VBox(4, m, meta));
            }
        });

        Label msg = new Label();
        Button markRead = new Button("Mark as Read");
        markRead.setOnAction(e -> {
            Notification n = list.getSelectionModel().getSelectedItem();
            if (n == null) { setMessage(msg, "Select a notification.", false); return; }
            dataStore.markNotificationRead(n.getId(), true);
            refreshNotificationData(source, unread);
        });

        Button archive = new Button("Archive / Unarchive");
        archive.setOnAction(e -> {
            Notification n = list.getSelectionModel().getSelectedItem();
            if (n == null) { setMessage(msg, "Select a notification.", false); return; }
            dataStore.archiveNotification(n.getId(), !n.isArchived());
            refreshNotificationData(source, unread);
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> {
            Notification n = list.getSelectionModel().getSelectedItem();
            if (n == null) { setMessage(msg, "Select a notification.", false); return; }
            dataStore.deleteNotification(n.getId());
            refreshNotificationData(source, unread);
        });

        HBox filterRow = new HBox(8, search, category, showMode);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actionRow = new HBox(8, markRead, archive, delete);

        VBox body = new VBox(10, unread, filterRow, list, actionRow, msg);
        body.setPadding(new Insets(20));
        body.getStyleClass().add("card");

        BorderPane pane = new BorderPane(body);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
        BorderPane.setMargin(body, new Insets(0, 20, 20, 20));
        return pane;
    }

    private void refreshNotificationData(ObservableList<Notification> source, Label unread) {
        source.setAll(dataStore.getNotificationsForUser(currentUser.getUsername(), true));
        unread.setText("Unread: " + dataStore.getUnreadNotificationCount(currentUser.getUsername()));
    }

    private void simulateCrash() {
        // Simulate a crash by saving current session state and closing the application
        saveSession("CrashTest", null);
        Alert a = new Alert(Alert.AlertType.WARNING, "Crash simulated. App will close now.");
        a.showAndWait();
        stage.close();
    }

    private void saveSession(String screen, String selectedBookId) {
        // Save the current user's session state for crash recovery
        if (currentUser != null) dataStore.saveSessionState(currentUser.getUsername(), screen, selectedBookId);
    }

    private void restorePreviousSessionIfPossible() {
        // Attempt to restore the previous session after application restart
        DataStore.SessionState state = dataStore.getSessionState();
        if (state == null || state.getUsername() == null || state.getUsername().isBlank()) return;

        // Find the user associated with the saved session
        Optional<User> target = dataStore.getAllUsers().stream()
                .filter(u -> u.getUsername().equals(state.getUsername()))
                .findFirst();
        if (target.isEmpty()) return;

        currentUser = target.get();
        String screen = safe(state.getScreen());

        // Restore the appropriate screen based on saved state
        if (screen.equals("Dashboard")) showDashboard(currentUser, "Dashboard");
        else if (screen.equals("Profile")) root.setCenter(buildProfileScreen());
        else if (screen.equals("Notifications")) root.setCenter(buildNotificationScreen());
        else if (screen.equals("Reader") || screen.equals("CrashTest")) showDashboard(currentUser, "Dashboard");
        else showDashboard(currentUser, "Dashboard");

        // Notify user of successful session restoration
        Alert restored = new Alert(Alert.AlertType.INFORMATION,
                "Session restored successfully for " + currentUser.getUsername() +
                        " (last state: " + screen + ")");
        restored.showAndWait();
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(150);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(left, right);
        return g;
    }

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        return l;
    }

    private VBox statCard(String label, String value) {
        Label v = new Label(value); v.getStyleClass().add("stat-value");
        Label l = new Label(label); l.getStyleClass().add("stat-label");
        VBox box = new VBox(4, v, l);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("stat-card");
        return box;
    }

    private VBox card(String title, javafx.scene.Node... children) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        VBox box = new VBox(10);
        box.getChildren().add(t);
        box.getChildren().addAll(children);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("card");
        return box;
    }

    private <T> TableColumn<Book, T> col(String title, javafx.util.Callback<Book, T> mapper, double width) {
        TableColumn<Book, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(mapper.call(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private BarChart<String, Number> buildAuthorBorrowBarChart(List<Book> books) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Borrow Counts");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        books.stream()
                .sorted(Comparator.comparingInt(Book::getBorrowCount).reversed())
                .limit(8)
                .forEach(book -> series.getData().add(new XYChart.Data<>(shortLabel(book.getTitle()), book.getBorrowCount())));
        if (series.getData().isEmpty()) series.getData().add(new XYChart.Data<>("No books", 0));
        chart.getData().add(series);
        return chart;
    }

    private PieChart buildAuthorRatingPieChart(List<BookReview> reviews) {
        PieChart chart = new PieChart();
        Map<String, Long> ratings = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.getRating() + " star", TreeMap::new, Collectors.counting()));
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        ratings.forEach((rating, count) -> data.add(new PieChart.Data(rating, count)));
        if (data.isEmpty()) data.add(new PieChart.Data("No ratings", 1));
        chart.setData(data);
        chart.setTitle("Rating Distribution");
        chart.setLegendVisible(false);
        chart.setPrefHeight(220);
        return chart;
    }

    private BarChart<String, Number> buildAuthorTrendChart(List<ReadingHistory> histories, String mode) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        refreshAuthorTrendChart(chart, histories, mode);
        return chart;
    }

    private void refreshAuthorTrendChart(BarChart<String, Number> chart, List<ReadingHistory> histories, String mode) {
        chart.setTitle(("Monthly".equals(mode) ? "Monthly" : "Weekly") + " Borrowing Trend");
        Map<String, Long> trend = histories.stream()
                .collect(Collectors.groupingBy(history -> trendKey(history.getBorrowDate(), mode), TreeMap::new, Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        trend.entrySet().stream().limit(12).forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        if (series.getData().isEmpty()) series.getData().add(new XYChart.Data<>("No data", 0));
        chart.getData().clear();
        chart.getData().add(series);
    }

    private String trendKey(LocalDate date, String mode) {
        if (date == null) return "Unknown";
        if ("Monthly".equals(mode)) return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return date.getYear() + "-W" + String.format(Locale.US, "%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private PieChart pieChartFromMap(String title, Map<String, Long> values) {
        PieChart chart = new PieChart();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        values.forEach((label, count) -> data.add(new PieChart.Data(label, count)));
        if (data.isEmpty()) data.add(new PieChart.Data("No data", 1));
        chart.setData(data);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setPrefHeight(220);
        return chart;
    }

    private BarChart<String, Number> barChartFromMap(String title, Map<String, Long> values) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(shortLabel(entry.getKey()), entry.getValue())));
        if (series.getData().isEmpty()) series.getData().add(new XYChart.Data<>("No data", 0));
        chart.getData().add(series);
        chart.setPrefHeight(220);
        return chart;
    }

    private PieChart buildGenrePieChart(List<ReadingHistory> histories) {
        PieChart chart = new PieChart();
        refreshGenrePieChart(chart, histories);
        chart.setTitle("Genres Read");
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(190);
        chart.setMinHeight(160);
        chart.getStyleClass().add("insight-chart");
        return chart;
    }

    private void refreshGenrePieChart(PieChart chart, List<ReadingHistory> histories) {
        Map<String, Long> byGenre = histories.stream()
                .collect(Collectors.groupingBy(h -> safe(h.getGenre()).isBlank() ? "Unknown" : h.getGenre(), Collectors.counting()));
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        byGenre.forEach((genre, count) -> data.add(new PieChart.Data(genre, count)));
        if (data.isEmpty()) data.add(new PieChart.Data("No history", 1));
        chart.setData(data);
    }

    private BarChart<String, Number> buildProgressBarChart(List<ReadingHistory> histories) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Reading Progress");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(190);
        chart.getStyleClass().add("insight-chart");
        refreshProgressBarChart(chart, histories);
        return chart;
    }

    private void refreshProgressBarChart(BarChart<String, Number> chart, List<ReadingHistory> histories) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        histories.stream()
                .sorted(Comparator.comparing(ReadingHistory::getBorrowDate).reversed())
                .limit(6)
                .forEach(h -> series.getData().add(new XYChart.Data<>(shortLabel(h.getBookTitle()), h.getProgressPercent())));
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("No history", 0));
        }
        chart.getData().clear();
        chart.getData().add(series);
    }

    private void exportAuthorStatsCsv(List<Book> books, List<BookReview> reviews, List<ReadingHistory> histories) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Author Stats CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("author-stats.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        StringBuilder csv = new StringBuilder("Metric,Value\n");
        csv.append("Books,").append(books.size()).append('\n');
        csv.append("Reads,").append(histories.size()).append('\n');
        csv.append("Borrow Count,").append(books.stream().mapToInt(Book::getBorrowCount).sum()).append('\n');
        csv.append("Reviews,").append(reviews.size()).append('\n');
        csv.append("Average Rating,").append(String.format(Locale.US, "%.2f", reviews.stream().mapToInt(BookReview::getRating).average().orElse(0.0))).append("\n\n");
        csv.append("Title,Status,Borrow Count,Reads,Rating Summary\n");
        for (Book book : books) {
            csv.append(csvCell(book.getTitle())).append(',')
                    .append(csvCell(book.getStatus().getDisplayName())).append(',')
                    .append(book.getBorrowCount()).append(',')
                    .append(dataStore.getReadCountForBook(book.getId())).append(',')
                    .append(csvCell(ratingSummary(book.getId()))).append('\n');
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            new Alert(Alert.AlertType.INFORMATION, "Author stats exported to CSV.").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to export stats: " + ex.getMessage()).showAndWait();
        }
    }

    private void exportAuthorStatsPdf(List<Book> books, List<BookReview> reviews, List<ReadingHistory> histories) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Author Stats PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("author-stats.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        List<String> lines = new ArrayList<>();
        lines.add("Author Stats Report");
        lines.add("Books: " + books.size());
        lines.add("Reads: " + histories.size());
        lines.add("Borrow Count: " + books.stream().mapToInt(Book::getBorrowCount).sum());
        lines.add("Reviews: " + reviews.size());
        lines.add(String.format(Locale.US, "Average Rating: %.2f", reviews.stream().mapToInt(BookReview::getRating).average().orElse(0.0)));
        lines.add("");
        books.forEach(book -> lines.add(shortLabel(book.getTitle(), 30) + " | " + book.getStatus().getDisplayName()
                + " | Borrows " + book.getBorrowCount() + " | Reads " + dataStore.getReadCountForBook(book.getId())));
        writeSimplePdf(file, lines, "Author stats exported to PDF.");
    }

    private void exportBorrowedRecordsCsv(List<ReadingHistory> histories) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Borrowed Records CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("borrowed-records.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        StringBuilder csv = new StringBuilder("Book Title,Borrower,Author,Borrow Date,Due Date,Return Date,Status,Duration Days\n");
        for (ReadingHistory history : histories) {
            csv.append(csvCell(history.getBookTitle())).append(',')
                    .append(csvCell(history.getUsername())).append(',')
                    .append(csvCell(history.getAuthor())).append(',')
                    .append(csvCell(formatDate(history.getBorrowDate()))).append(',')
                    .append(csvCell(formatDate(history.getBorrowDate().plusDays(dataStore.getMaxBorrowDays())))).append(',')
                    .append(csvCell(formatDate(history.getReturnDate()))).append(',')
                    .append(csvCell(borrowingRecordStatus(history))).append(',')
                    .append(history.getReadingDurationDays()).append('\n');
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            new Alert(Alert.AlertType.INFORMATION, "Borrowed records exported to Excel-compatible CSV.").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to export borrowed records: " + ex.getMessage()).showAndWait();
        }
    }

    private void exportReadingHistoryCsv(List<ReadingHistory> histories) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Reading History CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("reading-history.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        StringBuilder csv = new StringBuilder("Book Title,Author,Genre,Borrow Date,Return Date,Duration Days,Progress Percent\n");
        for (ReadingHistory h : histories) {
            csv.append(csvCell(h.getBookTitle())).append(',')
                    .append(csvCell(h.getAuthor())).append(',')
                    .append(csvCell(h.getGenre())).append(',')
                    .append(csvCell(formatDate(h.getBorrowDate()))).append(',')
                    .append(csvCell(formatDate(h.getReturnDate()))).append(',')
                    .append(h.getReadingDurationDays()).append(',')
                    .append(h.getProgressPercent()).append('\n');
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            new Alert(Alert.AlertType.INFORMATION, "Reading history exported to CSV.").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to export CSV: " + ex.getMessage()).showAndWait();
        }
    }

    private void exportReadingHistoryPdf(List<ReadingHistory> histories) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Reading History PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("reading-history.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(48, 740);
                content.showText("Reading History - " + currentUser.getFullName());
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(0, -24);
                int rows = 0;
                for (ReadingHistory h : histories) {
                    if (rows >= 28) break;
                    String line = shortLabel(h.getBookTitle(), 28) + " | " +
                            shortLabel(h.getAuthor(), 18) + " | " +
                            formatDate(h.getBorrowDate()) + " - " + formatDate(h.getReturnDate()) +
                            " | " + h.getProgressPercent() + "%";
                    content.showText(line.replaceAll("[^\\x20-\\x7E]", "?"));
                    content.newLineAtOffset(0, -18);
                    rows++;
                }
                content.endText();
            }
            document.save(file);
            new Alert(Alert.AlertType.INFORMATION, "Reading history exported to PDF.").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to export PDF: " + ex.getMessage()).showAndWait();
        }
    }

    private void writeSimplePdf(File file, List<String> lines, String successMessage) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 11);
            content.setLeading(15);
            content.newLineAtOffset(48, 740);
            int row = 0;
            for (String line : lines) {
                if (row >= 45) {
                    content.endText();
                    content.close();
                    page = new PDPage();
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 11);
                    content.setLeading(15);
                    content.newLineAtOffset(48, 740);
                    row = 0;
                }
                content.showText(shortLabel(line.replaceAll("[^\\x20-\\x7E]", "?"), 92));
                content.newLine();
                row++;
            }
            content.endText();
            content.close();
            document.save(file);
            new Alert(Alert.AlertType.INFORMATION, successMessage).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to write PDF: " + ex.getMessage()).showAndWait();
        }
    }

    private <S, T> TableColumn<S, T> typedCol(String title, javafx.util.Callback<S, T> mapper, double width) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(mapper.call(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private TableColumn<User, String> userCol(String title, javafx.util.Callback<User, String> mapper, double width) {
        TableColumn<User, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(mapper.call(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private void bindVisibility(javafx.scene.Node node, CheckBox toggle) {
        node.visibleProperty().bind(toggle.selectedProperty());
        node.managedProperty().bind(toggle.selectedProperty());
    }

    private String feedbackAnalytics(List<BookReview> reviews) {
        if (reviews == null || reviews.isEmpty()) return "No feedback yet.";
        Map<String, Long> sentiments = reviews.stream()
                .collect(Collectors.groupingBy(review -> SummaryService.analyzeSentiment(review.getReviewText()), Collectors.counting()));
        double average = reviews.stream().mapToInt(BookReview::getRating).average().orElse(0.0);
        long flagged = reviews.stream().filter(BookReview::isFlagged).count();
        return String.format(Locale.US, "Feedback analytics: %d review(s), average %.1f/5, Positive %d, Neutral %d, Negative %d, Flagged %d",
                reviews.size(),
                average,
                sentiments.getOrDefault("Positive", 0L),
                sentiments.getOrDefault("Neutral", 0L),
                sentiments.getOrDefault("Negative", 0L),
                flagged);
    }

    private String borrowingRecordStatus(ReadingHistory history) {
        if (history == null) return "-";
        if (history.getReturnDate() != null) return "Returned";
        return isOverdue(history) ? "Overdue" : "Active";
    }

    private boolean isOverdue(ReadingHistory history) {
        return history != null
                && history.getReturnDate() == null
                && history.getBorrowDate() != null
                && LocalDate.now().isAfter(history.getBorrowDate().plusDays(dataStore.getMaxBorrowDays()));
    }

    private void setMessage(Label label, String text, boolean success) {
        label.setText(text);
        label.getStyleClass().setAll("form-message", success ? "success-text" : "error-text");
    }

    private String formatDate(LocalDate d) { return d == null ? "-" : d.toString(); }
    private String formatDateTime(LocalDateTime d) { return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
    private String safe(String s) { return s == null ? "" : s; }

    private String ratingSummary(String bookId) {
        int count = dataStore.getReviewCount(bookId);
        if (count == 0) return "No reviews";
        return String.format(Locale.US, "%.1f/5 (%d)", dataStore.getAverageRating(bookId), count);
    }

    private String stars(int rating) {
        int clamped = Math.max(1, Math.min(5, rating));
        return "*".repeat(clamped) + "-".repeat(5 - clamped);
    }

    private String readingInsight(List<ReadingHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "No reading history yet. Borrowed books will be tracked automatically.";
        }
        long completed = histories.stream().filter(h -> h.getReturnDate() != null).count();
        double avgProgress = histories.stream().mapToInt(ReadingHistory::getProgressPercent).average().orElse(0.0);
        String topGenre = histories.stream()
                .collect(Collectors.groupingBy(ReadingHistory::getGenre, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("n/a");
        return String.format(Locale.US, "%d record(s), %d returned, average progress %.0f%%, most read genre: %s",
                histories.size(), completed, avgProgress, topGenre);
    }

    private String readingBadges(List<ReadingHistory> histories) {
        if (histories == null || histories.isEmpty()) return "Badge: First Borrow awaits";
        long returned = histories.stream().filter(h -> h.getReturnDate() != null).count();
        long highProgress = histories.stream().filter(h -> h.getProgressPercent() >= 80).count();
        List<String> badges = new ArrayList<>();
        badges.add("First Borrow");
        if (returned >= 1) badges.add("First Return");
        if (histories.size() >= 5) badges.add("5-Book Explorer");
        if (histories.size() >= 10) badges.add("Read 10 books this semester");
        if (highProgress >= 3) badges.add("Focused Reader");
        return "Badges: " + String.join(" | ", badges);
    }

    private String csvCell(String value) {
        String escaped = safe(value).replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String shortLabel(String value) {
        return shortLabel(value, 16);
    }

    private String shortLabel(String value, int max) {
        String text = safe(value);
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(1, max - 3)) + "...";
    }

    private String passwordStrength(String password) {
        if (password == null || password.isBlank()) return "";
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        if (score <= 2) return "Weak";
        if (score <= 4) return "Medium";
        return "Strong";
    }
}
