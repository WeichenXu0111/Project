# Phase 1 Implementation Notes

This document summarizes what was built to satisfy Phase 1 requirements in `readme.md`.

## 1. Student/Staff Portal

### Registration
- Implemented a registration form that collects username, full name, password, and role (Student or Staff).
- Validation:
  - Username required and unique.
  - Full name required.
  - Password min length 8.
- Passwords are stored securely using PBKDF2 with per-user salt.

### Login
- Login form verifies username/password and matches role (Student/Staff only).
- Clear feedback messages for success and failure.

### Available Book Screen
- Shows approved and available books with title, author, genre, publish date, and summary.
- Table view is styled consistently with the global UI theme.

### Borrow Book
- Borrow button only works on an available, selected book.
- Status updates to Borrowed; borrowing date and borrower are stored.
- Borrowed book list shown in a separate panel.
- Real-time UI update: borrowing a book immediately refreshes the borrowed books table, available books count, and borrowed count in the stats panel without requiring logout/login.

## 2. Author Portal

### Registration
- Registration form collects username, full name, password, and optional bio.
- Same validation and secure password handling.

### Login
- Login form verifies credentials and role match (Author).

### Publish New Book
- Publish form collects title, genre, summary, and a book file path.
- File is selected via `FileChooser` and stored as a path string.
- Selected file can be cleared by pressing Backspace or Delete key.
- Submission status is set to Pending Approval.

## 3. Librarian Portal

### Registration
- Registration form collects username, full name, password, and optional employee ID.

### Login
- Login form verifies credentials and role match (Librarian).

### New Book Approval
- Librarian dashboard lists all pending submissions.
- Approve or Reject actions prompt for confirmation and update status.

## 4. Data Model & Storage

- `User` model stores role-specific optional fields and hashed credentials.
- `Book` model tracks submission, approval, borrow status, and dates.
- `DataStore` loads/saves data using Java serialization to `data/lms-data.dat`.

## 5. UI & UX

- Consistent layout, spacing, and typography across all portals.
- Global styling via `src/main/resources/styles.css`.
- Card-based layout for readability, with primary/secondary action buttons.

## 6. Testing

- Added a small JUnit test to confirm password hashing and verification.

## 7. Files Added / Updated

- Added JavaFX dependencies and plugins in `pom.xml`.
- New JavaFX app entry `org.example.App` with role-based dashboards.
- Models: `Book`, `BookStatus`, `Role`, `User`.
- Data layer: `DataStore`.
- Security: `PasswordUtil`.
- UI styling: `styles.css`.
- Test: `PasswordUtilTest`.
- Setup instructions: `README_PHASE1.md`.

## 8. Phase 1 Nice-to-Have Features

### Student/Staff
- Added catalog search by title/author and filters for genre, publish date, availability.
- Quick preview dialog shows first lines of supported text files.
- Summary dialog allows full abstract reading before borrowing.
- Borrow confirmation displays duration and due date.
- Recommendations panel suggests popular or history-based books.

### Author
- Multi-genre selection with predefined genre list.
- Preview dialog shows a formatted summary before submission.
- Auto-save draft restores unfinished work between sessions.

### Librarian
- Preview and open book files directly from pending approval screen.
