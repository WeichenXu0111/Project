package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.data.DataStore;
import org.example.model.Book;
import org.example.model.BookStatus;
import org.example.model.Role;
import org.example.model.User;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

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

        HBox header = new HBox(16, brand, spacer, home, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 28, 16, 28));
        header.getStyleClass().add("app-header");
        return header;
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

        TableView<Book> availableTable = buildAvailableBooksTable();
        TableView<Book> borrowedTable = buildBorrowedBooksTable();

        Label availableTitle = new Label("Available Books");
        availableTitle.getStyleClass().add("card-title");
        VBox left = new VBox(12, availableTitle, availableTable, buildBorrowActions(availableTable));
        left.getStyleClass().add("card");
        left.setPadding(new Insets(16));

        Label borrowedTitle = new Label("My Borrowed Books");
        borrowedTitle.getStyleClass().add("card-title");
        VBox right = new VBox(12, borrowedTitle, borrowedTable);
        right.getStyleClass().add("card");
        right.setPadding(new Insets(16));

        HBox content = new HBox(16, left, right);
        content.setPadding(new Insets(24));
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

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
        table.setItems(dataStore.getAvailableBooks());
        table.getColumns().addAll(
                buildColumn("Title", Book::getTitle, 160),
                buildColumn("Author", Book::getAuthorFullName, 140),
                buildColumn("Genre", Book::getGenre, 100),
                buildColumn("Publish Date", book -> formatDate(book.getApprovedDate()), 120),
                buildColumn("Status", book -> book.getStatus().getDisplayName(), 120),
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
                buildColumn("Borrowed Date", book -> formatDate(book.getBorrowedDate()), 140)
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

        BorderPane layout = new BorderPane();
        layout.setTop(title);
        BorderPane.setMargin(title, new Insets(24, 24, 12, 24));
        layout.setCenter(body);
        return layout;
    }

    private VBox buildAuthorPublishForm() {
        Label formTitle = new Label("Publish New Book");
        formTitle.getStyleClass().add("card-title");

        Label helper = new Label("Provide the details shown to students: title, genre, and summary. A book file is required for review.");
        helper.getStyleClass().add("muted-text");
        helper.setWrapText(true);

        TextField titleField = new TextField();
        TextField genreField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setWrapText(true);
        TextField fileField = new TextField();
        fileField.setEditable(false);

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

        GridPane grid = buildFormGrid();
        grid.addRow(0, buildFormLabel("Book Title (shown in catalog)"), titleField);
        grid.addRow(1, buildFormLabel("Author Name (auto-filled)"), new Label(currentUser.getFullName()));
        grid.addRow(2, buildFormLabel("Genre / Category"), genreField);
        grid.addRow(3, buildFormLabel("Summary (shown to readers)"), descriptionArea);

        HBox fileRow = new HBox(8, fileField, browse);
        HBox.setHgrow(fileField, Priority.ALWAYS);
        grid.addRow(4, buildFormLabel("Book File (PDF)"), fileRow);

        Label message = new Label();
        message.getStyleClass().add("form-message");

        Button submit = new Button("Submit for Approval");
        submit.getStyleClass().add("primary-button");
        submit.setOnAction(event -> {
            DataStore.ActionResult result = dataStore.submitBook(
                    titleField.getText().trim(),
                    currentUser.getUsername(),
                    currentUser.getFullName(),
                    genreField.getText().trim(),
                    descriptionArea.getText().trim(),
                    fileField.getText().trim()
            );
            if (!result.success()) {
                message.setText(result.message());
                message.getStyleClass().setAll("form-message", "error-text");
                return;
            }
            titleField.clear();
            genreField.clear();
            descriptionArea.clear();
            fileField.clear();
            message.setText("Book submitted and pending librarian approval.");
            message.getStyleClass().setAll("form-message", "success-text");
        });

        VBox form = new VBox(12, formTitle, helper, grid, submit, message);
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

        Button approve = new Button("Approve");
        Button reject = new Button("Reject");
        approve.getStyleClass().add("primary-button");
        reject.getStyleClass().add("danger-button");

        approve.setOnAction(event -> handleApproval(table, true, message));
        reject.setOnAction(event -> handleApproval(table, false, message));

        HBox buttons = new HBox(12, approve, reject);
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
