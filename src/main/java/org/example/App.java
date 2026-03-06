package org.example;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.example.data.DataStore;
import org.example.model.AuthorDraft;
import org.example.model.Book;
import org.example.model.BookStatus;
import org.example.model.Role;
import org.example.model.User;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App extends Application {
    private final DataStore dataStore = new DataStore();
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    private User currentUser;

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
        root.getStyleClass().add("app-shell");

        scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("HKUST E-Library System - Phase 1");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildHeader() {
        Label title = new Label("HKUST Library");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("E-Library System");
        subtitle.getStyleClass().add("brand-subtitle");

        VBox brand = new VBox(2, title, subtitle);
        brand.getStyleClass().add("brand-block");

        ImageView logoView = buildLogoView();
        HBox brandRow = logoView == null ? new HBox(brand) : new HBox(12, logoView, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.getStyleClass().add("brand-row");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button home = new Button("Home");
        home.getStyleClass().add("nav-button");
        home.setOnAction(event -> {
            currentUser = null;
            root.setCenter(buildLanding());
        });

        Button logout = new Button("Logout");
        logout.getStyleClass().addAll("nav-button", "ghost-button");
        logout.setOnAction(event -> {
            currentUser = null;
            root.setCenter(buildLanding());
        });

        HBox header = new HBox(16, brandRow, spacer, home, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 28, 16, 28));
        header.getStyleClass().add("app-header");
        return header;
    }

    private ImageView buildLogoView() {
        Image logo = loadLogoImage();
        if (logo == null) {
            return null;
        }
        ImageView view = new ImageView(logo);
        view.getStyleClass().add("brand-logo");
        view.setFitHeight(52);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private Image loadLogoImage() {
        URL resource = getClass().getResource("/logo.png");
        if (resource != null) {
            return new Image(resource.toExternalForm());
        }
        File fallback = new File("logo.png");
        if (fallback.exists()) {
            return new Image(fallback.toURI().toString());
        }
        return null;
    }

    private VBox buildLanding() {
        Label pill = new Label("Phase 1 Portal");
        pill.getStyleClass().add("hero-pill");

        Label hero = new Label("Welcome to HKUST Library");
        hero.getStyleClass().add("hero-title");

        Label subtitle = new Label("Access student/staff, author, and librarian portals with a consistent workflow.");
        subtitle.getStyleClass().add("hero-subtitle");

        VBox heroPanel = new VBox(8, pill, hero, subtitle);
        heroPanel.getStyleClass().add("hero-panel");

        VBox studentCard = buildPortalCard(
                "Student / Staff",
                "Register, log in, browse approved books, and borrow instantly.",
                "Enter Portal",
                () -> root.setCenter(buildAuthView(Role.STUDENT))
        );
        VBox authorCard = buildPortalCard(
                "Author",
                "Submit new books, track approval status, and manage submissions.",
                "Enter Portal",
                () -> root.setCenter(buildAuthView(Role.AUTHOR))
        );
        VBox librarianCard = buildPortalCard(
                "Librarian",
                "Approve submissions, review users, and manage the catalog.",
                "Enter Portal",
                () -> root.setCenter(buildAuthView(Role.LIBRARIAN))
        );

        HBox cards = new HBox(16, studentCard, authorCard, librarianCard);
        cards.setAlignment(Pos.CENTER);
        HBox.setHgrow(studentCard, Priority.ALWAYS);
        HBox.setHgrow(authorCard, Priority.ALWAYS);
        HBox.setHgrow(librarianCard, Priority.ALWAYS);

        VBox content = new VBox(24, heroPanel, cards);
        content.setPadding(new Insets(28, 32, 32, 32));
        content.getStyleClass().add("landing-wrap");

        return new VBox(content);
    }

    private VBox buildPortalCard(String titleText, String descriptionText, String buttonText, Runnable action) {
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("muted-text");
        description.setWrapText(true);

        Button button = new Button(buttonText);
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> action.run());

        VBox card = new VBox(10, title, description, button);
        card.getStyleClass().add("portal-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(280);
        return card;
    }

    private BorderPane buildAuthView(Role role) {
        Label title = new Label(role.getDisplayName() + " Portal");
        title.getStyleClass().add("section-title");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(buildLoginTab(role));
        tabPane.getTabs().add(buildRegisterTab(role));
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane layout = new BorderPane();
        layout.setTop(title);
        BorderPane.setMargin(title, new Insets(24, 24, 12, 24));
        layout.setCenter(tabPane);
        BorderPane.setMargin(tabPane, new Insets(0, 24, 24, 24));
        return layout;
    }

    private Tab buildLoginTab(Role role) {
        Tab tab = new Tab("Login");

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Label message = new Label();
        message.getStyleClass().add("form-message");

        GridPane grid = buildFormGrid();
        grid.addRow(0, buildFormLabel("Username"), usernameField);
        grid.addRow(1, buildFormLabel("Password"), passwordField);

        Button submit = new Button("Login");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            User user = dataStore.authenticate(username, password, role);
            if (user == null) {
                message.setText("Invalid credentials or role mismatch.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            currentUser = user;
            message.setText("Login successful.");
            message.getStyleClass().setAll("form-message", "success-text");
            showDashboard(user);
        });

        VBox content = new VBox(16, grid, submit, message);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("card");

        tab.setContent(content);
        return tab;
    }

    private Tab buildRegisterTab(Role role) {
        Tab tab = new Tab("Register");

        TextField usernameField = new TextField();
        TextField fullNameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<Role> studentStaffRole = new ComboBox<>();
        TextField bioField = new TextField();
        TextField employeeIdField = new TextField();
        Label message = new Label();
        message.getStyleClass().add("form-message");

        if (role == Role.STUDENT) {
            studentStaffRole.getItems().addAll(Role.STUDENT, Role.STAFF);
            studentStaffRole.getSelectionModel().select(Role.STUDENT);
        }

        GridPane grid = buildFormGrid();
        int row = 0;
        grid.addRow(row++, buildFormLabel("Username"), usernameField);
        grid.addRow(row++, buildFormLabel("Full Name"), fullNameField);
        grid.addRow(row++, buildFormLabel("Password"), passwordField);
        if (role == Role.STUDENT) {
            grid.addRow(row++, buildFormLabel("Role"), studentStaffRole);
        }
        if (role == Role.AUTHOR) {
            grid.addRow(row++, buildFormLabel("Bio (optional)"), bioField);
        }
        if (role == Role.LIBRARIAN) {
            grid.addRow(row++, buildFormLabel("Employee ID (optional)"), employeeIdField);
        }

        Button submit = new Button("Create Account");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(event -> {
            Role finalRole = role == Role.STUDENT ? studentStaffRole.getValue() : role;
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = passwordField.getText();

            DataStore.RegistrationResult result = dataStore.registerUser(
                    username,
                    fullName,
                    password,
                    finalRole,
                    bioField.getText().trim(),
                    employeeIdField.getText().trim()
            );

            if (!result.success()) {
                message.setText(result.message());
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            message.setText("Registration successful. You can now log in.");
            message.getStyleClass().setAll("form-message", "success-text");
        });

        VBox content = new VBox(16, grid, submit, message);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("card");

        tab.setContent(content);
        return tab;
    }

    private GridPane buildFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.getStyleClass().add("form-grid");
        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(160);
        left.setPrefWidth(200);
        left.setMaxWidth(240);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private Label buildFormLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(160);
        return label;
    }

    private void showDashboard(User user) {
        if (user.getRole() == Role.AUTHOR) {
            root.setCenter(buildAuthorDashboard());
        } else if (user.getRole() == Role.LIBRARIAN) {
            root.setCenter(buildLibrarianDashboard());
        } else {
            root.setCenter(buildStudentDashboard());
        }
    }

    private BorderPane buildStudentDashboard() {
        Label title = new Label("Student / Staff Dashboard");
        title.getStyleClass().add("section-title");

        int availableCount = dataStore.getAvailableBooks().size();
        int borrowedCount = dataStore.getBorrowedBooksBy(currentUser.getUsername()).size();
        HBox stats = buildStatsRow(
                buildStatCard("Available Books", String.valueOf(availableCount), "accent-teal"),
                buildStatCard("My Borrowed", String.valueOf(borrowedCount), "accent-gold"),
                buildStatCard("Access Level", currentUser.getRole().getDisplayName(), "accent-slate")
        );

        ObservableList<Book> catalog = dataStore.getCatalogBooks();
        FilteredList<Book> filtered = new FilteredList<>(catalog, book -> true);

        TableView<Book> availableTable = buildAvailableBooksTable();
        SortedList<Book> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(availableTable.comparatorProperty());
        availableTable.setItems(sorted);

        ObservableList<Book> recommendationItems = FXCollections.observableArrayList(
                dataStore.getRecommendations(currentUser.getUsername(), 6)
        );
        Runnable refreshRecommendations = () -> recommendationItems.setAll(
                dataStore.getRecommendations(currentUser.getUsername(), 6)
        );

        VBox filters = buildCatalogFilters(filtered);
        VBox availableActions = buildAvailableActions(availableTable, refreshRecommendations);

        Label availableTitle = new Label("Catalog & Availability");
        availableTitle.getStyleClass().add("card-title");
        VBox left = new VBox(12, availableTitle, filters, availableTable, availableActions);
        left.getStyleClass().add("card");
        left.setPadding(new Insets(16));

        TableView<Book> borrowedTable = buildBorrowedBooksTable();
        Label borrowedTitle = new Label("My Borrowed Books");
        borrowedTitle.getStyleClass().add("card-title");
        VBox borrowedCard = new VBox(12, borrowedTitle, borrowedTable);
        borrowedCard.getStyleClass().add("card");
        borrowedCard.setPadding(new Insets(16));

        ListView<Book> recommendations = buildRecommendationList(recommendationItems, availableTable);
        Button refreshRec = new Button("Refresh Recommendations");
        refreshRec.getStyleClass().add("secondary-button");
        refreshRec.setOnAction(event -> refreshRecommendations.run());

        Label recTitle = new Label("Recommended for You");
        recTitle.getStyleClass().add("card-title");
        VBox recCard = new VBox(12, recTitle, recommendations, refreshRec);
        recCard.getStyleClass().add("card");
        recCard.setPadding(new Insets(16));

        VBox right = new VBox(16, borrowedCard, recCard);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.NEVER);

        HBox content = new HBox(16, left, right);
        content.setPadding(new Insets(24));

        VBox body = new VBox(16, stats, content);
        body.setPadding(new Insets(0, 24, 24, 24));

        BorderPane layout = new BorderPane();
        layout.setTop(title);
        BorderPane.setMargin(title, new Insets(24, 24, 12, 24));
        layout.setCenter(body);
        return layout;
    }

    private TableView<Book> buildAvailableBooksTable() {
        TableView<Book> table = new TableView<>();
        TableColumn<Book, String> titleColumn = buildColumn("Title", Book::getTitle, 170);
        titleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("unavailable-title");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    Book book = getTableRow() == null ? null : getTableRow().getItem();
                    if (book != null && !book.isAvailable()) {
                        getStyleClass().add("unavailable-title");
                    }
                }
            }
        });

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("unavailable-row");
                if (!empty && item != null && !item.isAvailable()) {
                    getStyleClass().add("unavailable-row");
                }
            }
        });

        table.getColumns().addAll(
                titleColumn,
                buildColumn("Author", Book::getAuthorFullName, 140),
                buildColumn("Genres", Book::getGenre, 120),
                buildColumn("Publish Date", book -> formatDate(book.getApprovedDate()), 120),
                buildColumn("Status", book -> book.getStatus().getDisplayName(), 130),
                buildColumn("Summary", Book::getDescription, 280)
        );
        table.setPlaceholder(new Label("No approved books yet."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private TableView<Book> buildBorrowedBooksTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getBorrowedBooksBy(currentUser.getUsername()));
        table.getColumns().addAll(
                buildColumn("Title", Book::getTitle, 180),
                buildColumn("Author", Book::getAuthorFullName, 160),
                buildColumn("Borrowed Date", book -> formatDate(book.getBorrowedDate()), 130),
                buildColumn("Due Date", book -> formatDate(book.getDueDate()), 120)
        );
        table.setPlaceholder(new Label("You have not borrowed any books."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildBorrowActions(TableView<Book> table) {
        Label message = new Label();
        message.getStyleClass().add("form-message");

        Button borrow = new Button("Borrow Selected Book");
        borrow.getStyleClass().add("primary-button");
        borrow.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a book to borrow.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            DataStore.ActionResult result = dataStore.borrowBook(selected.getId(), currentUser.getUsername());
            if (!result.success()) {
                message.setText(result.message());
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            message.setText("Book borrowed successfully.");
            message.getStyleClass().setAll("form-message", "success-text");
        });

        return new VBox(8, borrow, message);
    }

    private VBox buildAvailableActions(TableView<Book> table, Runnable onBorrowSuccess) {
        Label message = new Label();
        message.getStyleClass().add("form-message");

        Button preview = new Button("Quick Preview");
        preview.getStyleClass().add("secondary-button");
        preview.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a book to preview.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            showQuickPreviewDialog(selected);
        });

        Button summary = new Button("Read Summary");
        summary.getStyleClass().add("secondary-button");
        summary.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a book to read its summary.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            showSummaryDialog(selected);
        });

        Button borrow = new Button("Borrow Selected Book");
        borrow.getStyleClass().add("primary-button");
        borrow.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a book to borrow.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            if (!selected.isAvailable()) {
                message.setText("This book is not available right now.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            if (!showBorrowConfirmation(selected)) {
                return;
            }
            DataStore.ActionResult result = dataStore.borrowBook(selected.getId(), currentUser.getUsername());
            if (!result.success()) {
                message.setText(result.message());
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            if (onBorrowSuccess != null) {
                onBorrowSuccess.run();
            }
            message.setText("Book borrowed successfully.");
            message.getStyleClass().setAll("form-message", "success-text");
        });

        HBox actions = new HBox(10, preview, summary, borrow);
        actions.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, actions, message);
    }

    private VBox buildCatalogFilters(FilteredList<Book> filtered) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search title or author...");

        ComboBox<String> genreBox = new ComboBox<>();
        genreBox.getItems().add("All Genres");
        genreBox.getItems().addAll(dataStore.getGenreOptions());
        genreBox.getSelectionModel().selectFirst();

        ComboBox<String> availabilityBox = new ComboBox<>();
        availabilityBox.getItems().addAll("All", "Available", "Borrowed");
        availabilityBox.getSelectionModel().selectFirst();

        DatePicker publishDate = new DatePicker();
        publishDate.setPromptText("Publish date");

        Runnable applyFilter = () -> filtered.setPredicate(book -> {
            if (book == null) {
                return false;
            }
            String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                String title = book.getTitle() == null ? "" : book.getTitle().toLowerCase();
                String author = book.getAuthorFullName() == null ? "" : book.getAuthorFullName().toLowerCase();
                if (!title.contains(search) && !author.contains(search)) {
                    return false;
                }
            }
            String genre = genreBox.getValue();
            if (genre != null && !"All Genres".equals(genre)) {
                boolean match = book.getGenres().stream().anyMatch(item -> item.equalsIgnoreCase(genre));
                if (!match) {
                    return false;
                }
            }
            String availability = availabilityBox.getValue();
            if ("Available".equals(availability) && !book.isAvailable()) {
                return false;
            }
            if ("Borrowed".equals(availability) && book.isAvailable()) {
                return false;
            }
            LocalDate filterDate = publishDate.getValue();
            if (filterDate != null) {
                LocalDate approved = book.getApprovedDate();
                if (approved == null || !approved.equals(filterDate)) {
                    return false;
                }
            }
            return true;
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter.run());
        genreBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter.run());
        availabilityBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter.run());
        publishDate.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter.run());

        Button clear = new Button("Clear Filters");
        clear.getStyleClass().add("ghost-button");
        clear.setOnAction(event -> {
            searchField.clear();
            genreBox.getSelectionModel().selectFirst();
            availabilityBox.getSelectionModel().selectFirst();
            publishDate.setValue(null);
            applyFilter.run();
        });

        HBox row = new HBox(10, searchField, genreBox, availabilityBox, publishDate, clear);
        row.getStyleClass().add("filter-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label hint = new Label("Filter by title, author, genre, publish date, and availability.");
        hint.getStyleClass().add("muted-text");

        return new VBox(8, row, hint);
    }

    private ListView<Book> buildRecommendationList(ObservableList<Book> items, TableView<Book> linkedTable) {
        ListView<Book> listView = new ListView<>(items);
        listView.getStyleClass().add("recommendation-list");
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.getTitle());
                title.getStyleClass().add("card-title");
                Label meta = new Label(item.getAuthorFullName() + " • " + item.getGenreDisplay());
                meta.getStyleClass().add("muted-text");
                VBox box = new VBox(2, title, meta);
                setGraphic(box);
            }
        });
        listView.setOnMouseClicked(event -> {
            Book selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && linkedTable != null) {
                linkedTable.getSelectionModel().select(selected);
                linkedTable.scrollTo(selected);
            }
        });
        return listView;
    }

    private boolean showBorrowConfirmation(Book book) {
        LocalDate dueDate = LocalDate.now().plusDays(14);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Borrow Confirmation");
        alert.setHeaderText("Confirm borrowing this book?");
        alert.setContentText("Title: " + book.getTitle()
                + "\nBorrow duration: 14 days"
                + "\nDue date: " + dueDate
                + "\nAvailability: " + book.getStatus().getDisplayName());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showSummaryDialog(Book book) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Book Summary");
        alert.setHeaderText(book.getTitle());
        TextArea area = new TextArea(book.getDescription());
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefRowCount(10);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void showQuickPreviewDialog(Book book) {
        String filePath = book.getFilePath();
        if (filePath != null && filePath.toLowerCase().endsWith(".pdf")) {
            showPdfPreviewDialog(book);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quick Preview");
        alert.setHeaderText(book.getTitle());
        TextArea area = new TextArea(readPreviewText(filePath, 30));
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefRowCount(12);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void showPdfPreviewDialog(Book book) {
        String filePath = book.getFilePath();
        if (filePath == null || filePath.isBlank() || filePath.startsWith("seed://")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "This item has no local PDF attached.");
            alert.showAndWait();
            return;
        }
        List<Image> pages = renderPdfPreview(filePath, 3);
        if (pages.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to render PDF preview. Please open the file directly.");
            alert.showAndWait();
            return;
        }

        Pagination pagination = new Pagination(pages.size(), 0);
        pagination.setPageFactory(index -> {
            Image image = pages.get(index);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setFitWidth(560);
            ScrollPane scroll = new ScrollPane(view);
            scroll.setFitToWidth(true);
            scroll.setPannable(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.getStyleClass().add("page-scroll");
            return scroll;
        });

        VBox content = new VBox(10, pagination, new Label("Showing first " + pages.size() + " pages"));
        content.setPadding(new Insets(8));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("PDF Preview");
        alert.setHeaderText(book.getTitle());
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinHeight(420);
        alert.getDialogPane().setMinWidth(620);
        alert.showAndWait();
    }

    private List<Image> renderPdfPreview(String filePath, int maxPages) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return List.of();
        }
        List<Image> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = Math.min(document.getNumberOfPages(), maxPages);
            for (int i = 0; i < pageCount; i++) {
                BufferedImage buffered = renderer.renderImageWithDPI(i, 140, ImageType.RGB);
                images.add(SwingFXUtils.toFXImage(buffered, null));
            }
        } catch (IOException ex) {
            return List.of();
        }
        return images;
    }

    private String readPreviewText(String filePath, int maxLines) {
        if (filePath == null || filePath.isBlank() || filePath.startsWith("seed://")) {
            return "No preview available for this item.";
        }
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return "File not found: " + filePath;
        }
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "Preview not supported for this file type. Use Open File to view the document.";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < maxLines) {
                builder.append(line).append("\n");
                count++;
            }
        } catch (IOException ex) {
            return "Unable to read preview: " + ex.getMessage();
        }
        String preview = builder.toString().trim();
        return preview.isEmpty() ? "Preview is empty." : preview;
    }

    private void openBookFile(String filePath) {
        if (filePath == null || filePath.isBlank() || filePath.startsWith("seed://")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "This item has no local file attached.");
            alert.showAndWait();
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Desktop integration is not supported on this system.");
            alert.showAndWait();
            return;
        }
        try {
            Desktop.getDesktop().open(Path.of(filePath).toFile());
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to open file: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private BorderPane buildAuthorDashboard() {
        Label title = new Label("Author Dashboard");
        title.getStyleClass().add("section-title");

        List<Book> myBooks = dataStore.getBooksByAuthor(currentUser.getUsername());
        long pendingCount = myBooks.stream().filter(book -> book.getStatus() == BookStatus.PENDING_APPROVAL).count();
        long approvedCount = myBooks.stream().filter(book -> book.getStatus() == BookStatus.APPROVED_AVAILABLE).count();
        long rejectedCount = myBooks.stream().filter(book -> book.getStatus() == BookStatus.REJECTED).count();
        HBox stats = buildStatsRow(
                buildStatCard("Submissions", String.valueOf(myBooks.size()), "accent-teal"),
                buildStatCard("Approved", String.valueOf(approvedCount), "accent-gold"),
                buildStatCard("Rejected", String.valueOf(rejectedCount), "accent-slate")
        );

        VBox form = buildAuthorPublishForm();
        TableView<Book> submissions = buildAuthorSubmissionsTable();

        Label submissionsTitle = new Label("My Submissions");
        submissionsTitle.getStyleClass().add("card-title");
        VBox right = new VBox(12, submissionsTitle, submissions);
        right.getStyleClass().add("card");
        right.setPadding(new Insets(16));
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox content = new HBox(16, form, right);
        content.setPadding(new Insets(24));

        VBox body = new VBox(16, stats, content);
        body.setPadding(new Insets(0, 24, 24, 24));

        ScrollPane scrollPane = new ScrollPane(body);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("page-scroll");

        BorderPane layout = new BorderPane();
        layout.setTop(title);
        BorderPane.setMargin(title, new Insets(24, 24, 12, 24));
        layout.setCenter(scrollPane);
        return layout;
    }

    private VBox buildAuthorPublishForm() {
        Label formTitle = new Label("Publish New Book");
        formTitle.getStyleClass().add("card-title");

        Label helper = new Label("Provide the details shown to students: title, genres, and summary. A book file is required for review.");
        helper.getStyleClass().add("muted-text");
        helper.setWrapText(true);

        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setWrapText(true);
        TextField fileField = new TextField();
        fileField.setEditable(false);

        FlowPane genrePane = new FlowPane();
        genrePane.setHgap(8);
        genrePane.setVgap(6);
        List<CheckBox> genreChecks = new ArrayList<>();
        for (String genre : dataStore.getGenreOptions()) {
            CheckBox checkBox = new CheckBox(genre);
            checkBox.getStyleClass().add("chip");
            genreChecks.add(checkBox);
            genrePane.getChildren().add(checkBox);
        }

        Button browse = new Button("Choose File");
        browse.getStyleClass().add("secondary-button");
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Book File");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                fileField.setText(file.getAbsolutePath());
            }
        });

        Label draftStatus = new Label("Draft not saved yet.");
        draftStatus.getStyleClass().add("muted-text");

        PauseTransition autoSave = new PauseTransition(Duration.seconds(2));
        Runnable triggerSave = () -> {
            autoSave.stop();
            autoSave.playFromStart();
        };
        autoSave.setOnFinished(event -> {
            AuthorDraft draft = buildDraftFromForm(titleField, descriptionArea, fileField, genreChecks);
            dataStore.saveDraft(currentUser.getUsername(), draft);
            draftStatus.setText("Draft auto-saved at " + formatDateTime(draft.getLastSaved()));
        });

        titleField.textProperty().addListener((obs, oldValue, newValue) -> triggerSave.run());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> triggerSave.run());
        fileField.textProperty().addListener((obs, oldValue, newValue) -> triggerSave.run());
        genreChecks.forEach(checkBox -> checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> triggerSave.run()));

        GridPane grid = buildFormGrid();
        grid.addRow(0, buildFormLabel("Book Title (shown in catalog)"), titleField);
        grid.addRow(1, buildFormLabel("Author Name (auto-filled)"), new Label(currentUser.getFullName()));
        grid.addRow(2, buildFormLabel("Genres (multi-select)"), genrePane);
        grid.addRow(3, buildFormLabel("Summary (shown to readers)"), descriptionArea);

        HBox fileRow = new HBox(8, fileField, browse);
        HBox.setHgrow(fileField, Priority.ALWAYS);
        grid.addRow(4, buildFormLabel("Book File (PDF)"), fileRow);

        Button preview = new Button("Preview Details");
        preview.getStyleClass().add("secondary-button");
        preview.setOnAction(event -> showAuthorPreviewDialog(titleField, descriptionArea, fileField, genreChecks));

        Button submit = new Button("Submit for Approval");
        submit.getStyleClass().add("primary-button");

        Label message = new Label();
        message.getStyleClass().add("form-message");

        submit.setOnAction(event -> {
            List<String> genres = collectGenres(genreChecks);
            DataStore.ActionResult result = dataStore.submitBook(
                    titleField.getText().trim(),
                    currentUser.getUsername(),
                    currentUser.getFullName(),
                    genres,
                    descriptionArea.getText().trim(),
                    fileField.getText().trim()
            );
            if (!result.success()) {
                message.setText(result.message());
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            titleField.clear();
            descriptionArea.clear();
            fileField.clear();
            genreChecks.forEach(checkBox -> checkBox.setSelected(false));
            dataStore.clearDraft(currentUser.getUsername());
            draftStatus.setText("Draft cleared after submission.");
            message.setText("Book submitted and pending librarian approval.");
            message.getStyleClass().setAll("form-message", "success-text");
        });

        loadDraftIntoForm(titleField, descriptionArea, fileField, genreChecks, draftStatus);

        HBox buttons = new HBox(10, preview, submit);
        VBox form = new VBox(12, formTitle, helper, grid, draftStatus, buttons, message);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        form.setPrefWidth(420);
        return form;
    }

    private TableView<Book> buildAuthorSubmissionsTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getBooksByAuthor(currentUser.getUsername()));
        table.getColumns().addAll(
                buildColumn("Title", Book::getTitle, 200),
                buildColumn("Genre", Book::getGenre, 120),
                buildColumn("Submitted", book -> formatDate(book.getSubmittedDate()), 140),
                buildColumn("Status", book -> book.getStatus().getDisplayName(), 160)
        );
        table.setPlaceholder(new Label("No submissions yet."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private void loadDraftIntoForm(TextField titleField,
                                   TextArea descriptionArea,
                                   TextField fileField,
                                   List<CheckBox> genreChecks,
                                   Label draftStatus) {
        AuthorDraft draft = dataStore.getDraft(currentUser.getUsername());
        if (draft == null) {
            return;
        }
        titleField.setText(draft.getTitle());
        descriptionArea.setText(draft.getDescription());
        fileField.setText(draft.getFilePath());
        for (CheckBox checkBox : genreChecks) {
            checkBox.setSelected(draft.getGenres().contains(checkBox.getText()));
        }
        if (draft.getLastSaved() != null) {
            draftStatus.setText("Draft restored (saved at " + formatDateTime(draft.getLastSaved()) + ")");
        }
    }

    private AuthorDraft buildDraftFromForm(TextField titleField,
                                           TextArea descriptionArea,
                                           TextField fileField,
                                           List<CheckBox> genreChecks) {
        AuthorDraft draft = new AuthorDraft();
        draft.setTitle(titleField.getText().trim());
        draft.setDescription(descriptionArea.getText().trim());
        draft.setFilePath(fileField.getText().trim());
        draft.setGenres(collectGenres(genreChecks));
        return draft;
    }

    private List<String> collectGenres(List<CheckBox> genreChecks) {
        List<String> genres = new ArrayList<>();
        for (CheckBox checkBox : genreChecks) {
            if (checkBox.isSelected()) {
                genres.add(checkBox.getText());
            }
        }
        return genres;
    }

    private void showAuthorPreviewDialog(TextField titleField,
                                         TextArea descriptionArea,
                                         TextField fileField,
                                         List<CheckBox> genreChecks) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Preview Book Details");
        alert.setHeaderText("Confirm your submission details");
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String filePath = fileField.getText().trim();
        String genres = String.join(" / ", collectGenres(genreChecks));
        TextArea area = new TextArea(
                "Title: " + title
                        + "\nAuthor: " + currentUser.getFullName()
                        + "\nGenres: " + (genres.isBlank() ? "(none)" : genres)
                        + "\nFile: " + (filePath.isBlank() ? "(not selected)" : filePath)
                        + "\n\nSummary:\n" + description
        );
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefRowCount(12);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private BorderPane buildLibrarianDashboard() {
        Label title = new Label("Librarian Dashboard");
        title.getStyleClass().add("section-title");

        int pendingCount = dataStore.getPendingBooks().size();
        int availableCount = dataStore.getAvailableBooks().size();
        int userCount = dataStore.getAllUsers().size();
        HBox stats = buildStatsRow(
                buildStatCard("Pending Approvals", String.valueOf(pendingCount), "accent-teal"),
                buildStatCard("Approved Books", String.valueOf(availableCount), "accent-gold"),
                buildStatCard("Registered Users", String.valueOf(userCount), "accent-slate")
        );

        TableView<Book> pendingTable = buildPendingTable();
        VBox actions = buildApprovalActions(pendingTable);

        Label pendingTitle = new Label("Pending Book Approvals");
        pendingTitle.getStyleClass().add("card-title");
        VBox card = new VBox(12, pendingTitle, pendingTable, actions);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        TableView<User> usersTable = buildUsersTable();
        Label usersTitle = new Label("User Directory");
        usersTitle.getStyleClass().add("card-title");
        VBox usersCard = new VBox(12, usersTitle, usersTable);
        usersCard.getStyleClass().add("card");
        usersCard.setPadding(new Insets(16));

        VBox body = new VBox(16, stats, card, usersCard);
        body.setPadding(new Insets(0, 24, 24, 24));

        BorderPane layout = new BorderPane();
        layout.setTop(title);
        BorderPane.setMargin(title, new Insets(24, 24, 12, 24));
        layout.setCenter(body);
        return layout;
    }

    private TableView<Book> buildPendingTable() {
        TableView<Book> table = new TableView<>();
        table.setItems(dataStore.getPendingBooks());
        table.getColumns().addAll(
                buildColumn("Title", Book::getTitle, 180),
                buildColumn("Author Username", Book::getAuthorUsername, 140),
                buildColumn("Author Name", Book::getAuthorFullName, 160),
                buildColumn("Genre", Book::getGenre, 120),
                buildColumn("Submitted", book -> formatDate(book.getSubmittedDate()), 130),
                buildColumn("Status", book -> book.getStatus().getDisplayName(), 140)
        );
        table.setPlaceholder(new Label("No pending submissions."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private VBox buildApprovalActions(TableView<Book> table) {
        Label message = new Label();
        message.getStyleClass().add("form-message");

        Button preview = new Button("Preview File");
        preview.getStyleClass().add("secondary-button");
        preview.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a submission first.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            showQuickPreviewDialog(selected);
        });

        Button openFile = new Button("Open File");
        openFile.getStyleClass().add("secondary-button");
        openFile.setOnAction(event -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                message.setText("Please select a submission first.");
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            openBookFile(selected.getFilePath());
        });

        Button approve = new Button("Approve");
        Button reject = new Button("Reject");
        approve.getStyleClass().add("primary-button");
        reject.getStyleClass().add("danger-button");

        approve.setOnAction(event -> handleApproval(table, true, message));
        reject.setOnAction(event -> handleApproval(table, false, message));

        HBox buttons = new HBox(12, preview, openFile, approve, reject);
        return new VBox(8, buttons, message);
    }

    private void handleApproval(TableView<Book> table, boolean approved, Label message) {
        Book selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message.setText("Please select a submission first.");
            message.getStyleClass().setAll("form-message", "error-text");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Action");
        alert.setHeaderText(approved ? "Approve this submission?" : "Reject this submission?");
        alert.setContentText(selected.getTitle());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                DataStore.ActionResult actionResult = approved
                        ? dataStore.approveBook(selected.getId())
                        : dataStore.rejectBook(selected.getId());
                if (!actionResult.success()) {
                    message.setText(actionResult.message());
                    message.getStyleClass().setAll("form-message", "error-text");
                    return;
                }
                message.setText(approved ? "Book approved." : "Book rejected.");
                message.getStyleClass().setAll("form-message", "success-text");
            }
        });
    }

    private TableView<User> buildUsersTable() {
        TableView<User> table = new TableView<>();
        table.setItems(dataStore.getAllUsers());
        table.getColumns().addAll(
                buildUserColumn("Username", User::getUsername, 160),
                buildUserColumn("Full Name", User::getFullName, 180),
                buildUserColumn("Role", user -> user.getRole().getDisplayName(), 120),
                buildUserColumn("Bio", user -> formatOptional(user.getBio()), 200),
                buildUserColumn("Employee ID", user -> formatOptional(user.getEmployeeId()), 140)
        );
        table.setPlaceholder(new Label("No users found."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private <T> TableColumn<User, T> buildUserColumn(String title, javafx.util.Callback<User, T> mapper, double width) {
        TableColumn<User, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(mapper.call(cellData.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private String formatOptional(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private <T> TableColumn<Book, T> buildColumn(String title, javafx.util.Callback<Book, T> mapper, double width) {
        TableColumn<Book, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(mapper.call(cellData.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private HBox buildStatsRow(VBox... cards) {
        HBox row = new HBox(12, cards);
        row.getStyleClass().add("stats-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildStatCard(String labelText, String valueText, String accentClass) {
        Label value = new Label(valueText);
        value.getStyleClass().add("stat-value");

        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");

        VBox card = new VBox(6, value, label);
        card.getStyleClass().add("stat-card");
        if (accentClass != null && !accentClass.isBlank()) {
            card.getStyleClass().add(accentClass);
        }
        card.setPadding(new Insets(14, 16, 14, 16));
        return card;
    }
}
