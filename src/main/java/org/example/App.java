package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.example.data.DataStore;
import org.example.model.AuthorDraft;
import org.example.model.Book;
import org.example.model.BookStatus;
import org.example.model.Notification;
import org.example.model.Role;
import org.example.model.User;
import org.example.pdf.InteractivePDFReader;
import org.example.pdf.PDFHighlightManager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, brandRow, spacer, homeBtn, notificationsBtn, profileBtn, crashBtn, logoutBtn);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        Runnable update = () -> {
            boolean loggedIn = currentUser != null;
            profileBtn.setVisible(loggedIn); profileBtn.setManaged(loggedIn);
            notificationsBtn.setVisible(loggedIn); notificationsBtn.setManaged(loggedIn);
            logoutBtn.setVisible(loggedIn); logoutBtn.setManaged(loggedIn);
            crashBtn.setVisible(loggedIn); crashBtn.setManaged(loggedIn);
        };
        root.centerProperty().addListener((a, b, c) -> update.run());
        update.run();

        return header;
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
        });

        TableView<Book> borrowedTable = buildBorrowedBooksTable();
        borrowedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox borrowedActions = buildBorrowedActions(borrowedTable, () -> {
            borrowedTable.setItems(dataStore.getBorrowedBooksBy(currentUser.getUsername()));
            ((Label) statsA.getChildren().get(0)).setText(String.valueOf(dataStore.getAvailableBooks().size()));
            ((Label) statsB.getChildren().get(0)).setText(String.valueOf(dataStore.getBorrowedBooksBy(currentUser.getUsername()).size()));
        });

        VBox left = new VBox(10, new Label("Catalog"), filters, filterMsg, multiSelectTip, catalogTable, borrowActions);
        left.getStyleClass().add("card");
        left.setPadding(new Insets(14));
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox right = new VBox(10, new Label("My Borrowed Books"), borrowedTable, borrowedActions);
        right.getStyleClass().add("card");
        right.setPadding(new Insets(14));
        right.setPrefWidth(470);

        HBox bodyRow = new HBox(12, left, right);
        VBox body = new VBox(14, stats, bodyRow);
        body.setPadding(new Insets(0, 20, 20, 20));

        BorderPane pane = new BorderPane(body);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
        return pane;
    }

    private TableView<Book> buildAvailableBooksTable() {
        TableView<Book> table = new TableView<>();
        table.getColumns().addAll(
                col("Title", Book::getTitle, 180),
                col("Author", Book::getAuthorFullName, 150),
                col("Genre", Book::getGenre, 120),
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
            InteractivePDFReader reader = readerMap.getOrDefault(index + 1, 
                new InteractivePDFReader(pages.get(index), index + 1));
            
            // Set selection callback properly for any page
            reader.setSelectionCallback((text, x, y, width, height) -> {
                if (saveProgress) {
                    dataStore.addInteractiveHighlight(currentUser.getUsername(), book.getId(), index + 1, x, y, width, height, text);
                    updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), index + 1);
                    // Add highlight visibly to prevent needing a full reload
                    reader.addHighlight(new org.example.pdf.PDFHighlightManager.HighlightData(index + 1, x, y, width, height, text));
                }
            });

            loadHighlightsForPage(reader, index + 1, book.getId());
            readerMap.put(index + 1, reader);
            currentPageIndex[0] = index;
            
            updatePageIndicator(pageIndicator, index + 1, pages.size());
            updateInteractiveHighlightInfo(interactiveHighlightInfo, book.getId(), index + 1);
            
            if (saveProgress) {
                dataStore.saveBookmark(currentUser.getUsername(), book.getId(), index + 1);
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
            saveSession("Reader", book.getId());
        }
    }

    private void loadHighlightsForPage(InteractivePDFReader reader, int pageNum, String bookId) {
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
        bodyRow.setPadding(new Insets(0, 20, 20, 20));

        BorderPane pane = new BorderPane(bodyRow);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
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

        AuthorDraft draft = dataStore.getDraft(currentUser.getUsername());
        if (draft != null) {
            title.setText(draft.getTitle());
            summary.setText(draft.getDescription());
            filePath.setText(draft.getFilePath());
            genreChecks.forEach(cb -> cb.setSelected(draft.getGenres().contains(cb.getText())));
        }

        Label msg = new Label();
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
        g.addRow(r++, formLabel("Summary"), summary);
        g.addRow(r++, formLabel("Book File"), fileRow);
        g.addRow(r++, formLabel("Cover Image"), coverRow);

        VBox box = new VBox(10, t, g, new HBox(8, saveDraft, submit), msg);
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

        TableView<Book> pendingTable = buildPendingTable();
        VBox approvalActions = buildApprovalActions(pendingTable);

        // Add multi-selection tip for librarian
        Label multiSelectTip = new Label("Tip: Hold Ctrl or Shift to select multiple books.");
        multiSelectTip.getStyleClass().add("muted-text");
        VBox pendingTableWithTip = new VBox(4, pendingTable, multiSelectTip);

        TableView<User> usersTable = buildUsersTable();
        VBox userActions = buildUserActions(usersTable);

        TableView<Book> records = buildAllBorrowedBooksTable();
        TextField searchRecords = new TextField();
        searchRecords.setPromptText("Search borrowed records (title / username)...");
        searchRecords.textProperty().addListener((a, b, c) -> {
            String q = c == null ? "" : c.toLowerCase().trim();
            if (q.isBlank()) records.setItems(dataStore.getAllBorrowedBooks());
            else {
                records.setItems(FXCollections.observableArrayList(dataStore.getAllBorrowedBooks().stream().filter(book ->
                        safe(book.getTitle()).toLowerCase().contains(q) || safe(book.getBorrowedBy()).toLowerCase().contains(q)
                ).collect(Collectors.toList())));
            }
        });

        VBox p1 = card("Pending Submissions", pendingTableWithTip, approvalActions);
        VBox p2 = card("Manage All Users", usersTable, userActions);
        VBox p3 = card("Borrowed Books Record", searchRecords, records);

        VBox body = new VBox(12, p1, p2, p3);
        body.setPadding(new Insets(0, 20, 20, 20));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);

        BorderPane pane = new BorderPane(scroll);
        pane.setTop(title);
        BorderPane.setMargin(title, new Insets(20, 20, 8, 20));
        return pane;
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

    private TableView<User> buildUsersTable() {
        TableView<User> table = new TableView<>();
        table.setItems(dataStore.getAllUsers());
        table.getColumns().addAll(
                userCol("Username", User::getUsername, 120),
                userCol("Full Name", User::getFullName, 160),
                userCol("Role", u -> u.getRole().getDisplayName(), 100),
                userCol("Active", u -> u.isActive() ? "Yes" : "No", 80),
                userCol("Bio", u -> safe(u.getBio()), 150),
                userCol("Employee ID", u -> safe(u.getEmployeeId()), 110)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildUserActions(TableView<User> table) {
        Label msg = new Label();

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

        return new VBox(8, new HBox(8, add, edit, toggle), msg);
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
            setMessage(msg, r.message(), r.success());
            root.setCenter(buildLibrarianDashboard());
        }
    }

    private void showEditUserDialog(User user, Label msg) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Edit User: " + user.getUsername());
        TextField fullName = new TextField(user.getFullName());
        TextField bio = new TextField(safe(user.getBio()));
        TextField employee = new TextField(safe(user.getEmployeeId()));

        GridPane g = formGrid();
        g.addRow(0, formLabel("Full Name"), fullName);
        g.addRow(1, formLabel("Bio"), bio);
        g.addRow(2, formLabel("Employee ID"), employee);

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            DataStore.ActionResult r = dataStore.updateUserProfile(user, fullName.getText().trim(), "", bio.getText().trim(), employee.getText().trim());
            setMessage(msg, r.message(), r.success());
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
            g.addRow(r++, formLabel("Profile Picture"), avatarRow);
        }
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
            if (result.success() && currentUser.getRole() == Role.AUTHOR) {
                currentUser.setAvatarPath(avatarPath.getText().trim());
                dataStore.saveData();
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
        category.getItems().addAll("All", "Due Reminder", "Book Deletion", "Book Approval", "Book Rejection", "Submission", "Account", "General", "Borrowing");
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
                if (empty || n == null) { setText(null); setGraphic(null); return; }
                Label m = new Label((n.isRead() ? "" : "[UNREAD] ") + n.getMessage());
                m.setWrapText(true);
                Label meta = new Label(n.getCategory() + " | " + n.getUrgency() + " | " + formatDateTime(n.getTimestamp()));
                meta.getStyleClass().add("muted-text");
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

    private TableColumn<User, String> userCol(String title, javafx.util.Callback<User, String> mapper, double width) {
        TableColumn<User, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(mapper.call(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private void setMessage(Label label, String text, boolean success) {
        label.setText(text);
        label.getStyleClass().setAll("form-message", success ? "success-text" : "error-text");
    }

    private String formatDate(LocalDate d) { return d == null ? "-" : d.toString(); }
    private String formatDateTime(LocalDateTime d) { return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
    private String safe(String s) { return s == null ? "" : s; }

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
