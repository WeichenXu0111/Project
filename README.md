# HKUST E-Library System

A JavaFX-based Library Management System developed as a direct Phase 2 extension of the original Phase 1 project. The application provides an integrated platform for students/staff, authors, and librarians to interact with the same persistent digital library system.

---

## Overview

The HKUST E-Library System is a desktop application that streamlines book management and distribution across an academic institution. Phase 2 continues the Phase 1 architecture and extends the same application with richer reading, profile, notification, approval, and crash-recovery related features. It supports three distinct user roles with tailored functionalities:

- **Students/Staff**: Browse and borrow books from the approved catalog
- **Authors**: Submit and manage their published works
- **Librarians**: Oversee the entire book approval process and user management

---

## Key Features

### 🎓 Student/Staff Portal

#### Authentication, Profile & Notifications
- User registration with username, full name, password, and role selection
- Secure login with credential validation
- Profile management with password re-authentication before saving changes
- Password update support with automatic logout after a successful password change
- Dedicated notification board with unread counter, archive support, and search/filter by category
<img width="1650" height="1002" alt="image" src="https://github.com/user-attachments/assets/faa5dcbb-99e8-4322-a152-d01011f1b1d3" />

#### Book Browsing & Discovery
- **Available Books Catalog**: Browse all approved books with detailed information
- **Book Details Display**: 
  - Title, Author name, Genre, Publication date
  - Availability status
  - Book abstract/summary
- **Advanced Search & Filtering**:
  - Search by title or author name
  - Filter by genre
  - Filter by availability status (Available/Borrowed)
  - Filter by publication date
  - Real-time filtering with instant results

#### Book Borrowing & Management
- **Borrow Books**:
  - Support multiple-book selection in the catalog
  - Enforce a logical borrow limit for each user
  - Validate borrow duration with configured min/max day limits
- **Borrowed Books Management**:
  - View all currently borrowed books with due dates
  - Return one or multiple borrowed books early with confirmation
  - Track borrow and due dates
  - Auto-return expired books and refresh availability status
<img width="1655" height="1008" alt="image" src="https://github.com/user-attachments/assets/60042c04-7167-44e5-bd46-bcb629a4c24b" />


#### Borrowed Book Reading
- **PDF Reader**: Open borrowed PDF books inside the application
- **Bookmark Support**: Save reading progress and restore the bookmarked page later
- **Interactive Highlighting**: Highlight selected regions while reading PDF pages
- **Expiry Handling While Reading**: Close the reading dialog if the borrowing period expires and auto-return the book
- **Catalog Search & Filtering**:
  - Search by title or author name
  - Filter by genre, publish date, and availability

#### Personalized Recommendations
- **Recommendation System**: AI-based book recommendations tailored to user
- **Refresh Recommendations**: Get new recommendations on demand
- **Interactive Selection**: Click recommendations to view details in catalog

#### Dashboard Statistics
- **Stats Dashboard**: View key metrics
  - Total available books
  - Personal borrowed count
  - Access level information

---

### ✍️ Author Portal

#### Authentication, Profile & Notifications
- Author registration with username, full name, password
- Bio/profile information (optional)
- Secure login and session management
- Profile management with full name, password, bio, and optional profile picture upload
- Password re-authentication before profile updates
- Dedicated notification board with search/filter, mark-as-read, delete, archive, and unread counter support

#### Book Publishing
- **Publish New Book Form**:
  - Book title input
  - Author name (auto-filled from profile)
  - Multiple genre selection (multi-select)
  - Book summary/description (shown to readers)
  - Book file attachment (PDF support)
  - Optional cover image selection with JPG/PNG validation and size limit checking
<img width="1904" height="969" alt="image" src="https://github.com/user-attachments/assets/349fc641-8e82-4a58-ba11-b9d8db5ed9a4" />

<img width="1904" height="964" alt="image" src="https://github.com/user-attachments/assets/769e72fd-1366-45a6-b335-4c5af7fd4739" />

#### Draft Persistence
- **Draft Saving**: Save unfinished submissions for later completion
- **Draft Persistence**: Restore previously saved draft on login
- **Draft Management**: Clear draft after successful submission

#### Submission Management
- **Preview Before Submission**: Review all book details before publishing
- **Book Submission**: Submit books for librarian approval
- **Submissions Tracking Table**:
  - View all submitted books with metadata
  - Track submission date
  - Monitor approval status (Pending/Approved/Rejected)
- **Published / Submitted Book Actions**:
  - Read uploaded books from the author portal
  - Edit submitted/published book information
  - Delete one or multiple submitted books with confirmation dialogs

#### Dashboard Analytics
- **Publishing Statistics**:
  - Total submissions count
  - Approved books count
  - Rejected submissions count
- **Publication Status**: See each book's approval status

---

### 📚 Librarian Dashboard

#### Authentication, Profile & Notifications
- Librarian registration with username, full name, password
- Employee ID (optional)
- Administrative access control
- Manage own profile with full name, password, employee ID, and password strength feedback
- Dedicated notification board with unread counter, archive support, and search/filter tools
<img width="1919" height="967" alt="image" src="https://github.com/user-attachments/assets/fb088fad-2e4f-41fa-9184-dde27cd6c269" />


#### Book Approval Workflow
- **Pending Submissions Queue**:
  - View all books awaiting approval
  - Book details: Title, Author username, Author name, Genre, Submission date, Status
- **Approval Actions**:
  - **Preview File**: Quick 3-page PDF preview of submissions
  - **Open File**: Direct file access for detailed review
  - **Approve**: Accept and publish books to catalog
  - **Reject**: Send rejection with feedback / rejection reason
  - **Search & Filter**: Search pending submissions by title, author, and genre
  - **Bulk Actions**: Approve or reject multiple submissions together
<img width="1919" height="1002" alt="image" src="https://github.com/user-attachments/assets/a24240e5-2391-435b-a138-5b83ee5b3d20" />
<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/5301139e-27d2-4edc-9083-f04c98161c7c" />


#### User Management
- **User Directory**: View all registered users
- **User Information Display**:
  - Username and full name
  - User role (Student/Staff/Author/Librarian)
  - Bio information (if provided)
  - Employee ID (if provided)
- **Account Actions**:
  - Add new users of any supported role
  - Edit selected users
  - Activate/deactivate user accounts
- **User Statistics**: Monitor total registered users

#### Borrowed Books Record
- View system-wide borrowed-book records
- Search borrowed records by book title or borrower username
- Review borrow date, due date, and current status in a centralized librarian view

#### Approval Confirmation
- **Confirmation Dialogs**: Verify approval/rejection actions
- **User Feedback**: Clear messaging on action success/failure

---

## Phase 2 Enhancements

Phase 2 is implemented as an extension of the original Phase 1 Library Management System. Key additions include:

- **Task 1.4 Nice-to-have**:
  - Multiple-book selection when borrowing
  - Borrow-limit enforcement with clear error messaging
  - Min/max borrow duration validation
- **Task 2.3 Nice-to-have**:
  - Optional book cover image selection with format and size checks
- **Task 3.3 Nice-to-have**:
  - Rejection reason input for librarian review
  - Search/filter tools for pending submissions
  - Bulk approval/rejection actions
- **Task 1.5 Borrowed Book Screen**:
  - PDF reading for borrowed books
  - Bookmark persistence
  - Interactive PDF highlight support
  - Partial return for selected books
  - Auto-return handling on expiry
- **Task 1.6 / 2.5 / 3.5 Profile Management**:
  - Password re-authentication before updating profile
  - Success/failure feedback on profile updates
  - Password strength indicator for librarian profile updates
  - Optional profile picture upload support
- **Task 1.7 / 2.6 / 3.7 Notification Boards**:
  - Timestamped and categorized notifications
  - Unread counters
  - Archive / unarchive support
  - Search and filter tools
  - Mark-as-read and delete operations where applicable
- **Whole-System Crash Recovery Support**:
  - Crash Test button in the shared application header
  - Session persistence for reopening the last active user session
  - Persistent storage for drafts, bookmarks, notifications, and reading highlights
  - Restoration feedback when a previous session is recovered

---

## Technical Stack

### Core Technologies
- **Language**: Java 21
- **UI Framework**: JavaFX 21+
- **Build Tool**: Maven 3.9+
- **PDF Processing**: Apache PDFBox

### Data Persistence
- **Storage**: Local serialization (Java object serialization)
- **Data Location**: `data/lms-data.dat`
- **Security**: Encrypted password storage with validation

### Architecture
- **MVC Pattern**: Clear separation of concerns
- **Model Classes**: `User`, `Book`, `Role`, `BookStatus`, `AuthorDraft`, `Notification`
- **Data Store**: Centralized `DataStore` for all data operations
- **PDF Components**: `InteractivePDFReader` and `PDFHighlightManager` for in-app reading/highlight features
- **UI Components**: Reusable form builders, table views, dialogs, and role-based dashboards

---

## Security Features

- **Password Security**: 
  - Encrypted password storage
  - Password validation on registration
- **User Authentication**: 
  - Credential validation on login
  - Role-based access control
- **Data Protection**: 
  - Secure local storage of all user data
  - Credentials protected with encryption

---

## System Requirements

- **Java**: 21 or higher
- **Maven**: 3.9 or higher
- **Operating System**: Windows, macOS, or Linux (with JavaFX support)
- **Display**: Minimum 1100x700 resolution recommended

---

## Installation & Setup

### Prerequisites
Ensure you have Java 21 and Maven 3.9+ installed:
```bash
java -version
mvn -version
```

### Installation Steps

1. **Clone/Extract the Project**
   ```bash
   cd C:\Users\86158\Desktop\Project New\Project-master
   ```

2. **Build the Project**
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   ```bash
   mvn -q javafx:run
   ```

---

## Running Tests

Execute the test suite to validate functionality:
```bash
mvn -q test
```

---

## Project Structure

```
Project-master/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── App.java                    # Main UI application
│   │   │   ├── Main.java                   # Entry point
│   │   │   ├── data/
│   │   │   │   └── DataStore.java         # Data persistence layer
│   │   │   ├── model/
│   │   │   │   ├── User.java              # User entity
│   │   │   │   ├── Book.java              # Book entity
│   │   │   │   ├── AuthorDraft.java       # Author draft entity
│   │   │   │   ├── Notification.java      # Notification entity
│   │   │   │   ├── Role.java              # User role enum
│   │   │   │   └── BookStatus.java        # Book status enum
│   │   │   ├── pdf/
│   │   │   │   ├── InteractivePDFReader.java
│   │   │   │   └── PDFHighlightManager.java
│   │   │   └── security/
│   │   │       └── PasswordUtil.java      # Password encryption/validation
│   │   └── resources/
│   │       ├── styles.css                 # UI styling
│   │       └── logo.png                   # Application logo
│   └── test/
│       └── java/org/example/
│           └── security/
│               └── PasswordUtilTest.java  # Password utility tests
├── data/
│   └── lms-data.dat                       # Persistent data storage
├── pom.xml                                # Maven configuration
└── README.md                              # This file
```

---

## Usage Guide

### For Students/Staff

1. **Register Account**: Click "Student / Staff" → "Register" tab → Fill details
2. **Login**: Click "Login" tab → Enter credentials
3. **Browse Books**: View available books in the catalog
4. **Borrow Books**: Select a book → Click "Borrow Selected Book"
5. **Read Books**: Open borrowed PDF books with bookmark/highlight support
6. **Return Books**: Go to "My Borrowed Books" → Select one or more books → Click return
7. **Manage Profile / Notifications**: Update profile with password re-authentication and review notification board
8. **Get Recommendations**: View personalized recommendations or click "Refresh Recommendations"

### For Authors

1. **Register Account**: Click "Author" → "Register" tab → Fill details
2. **Login**: Click "Login" tab → Enter credentials
3. **Publish Book**: 
   - Fill book title, select genres
   - Write summary
   - Attach PDF file
   - Optionally choose a cover image
   - Click "Submit for Approval"
4. **Save Draft**: Save unfinished submission data for later continuation
5. **Track / Manage Submissions**: View, read, edit, or delete books from "My Submissions"
6. **Manage Profile / Notifications**: Update profile details and review approval notifications

### For Librarians

1. **Register Account**: Click "Librarian" → "Register" tab → Fill details
2. **Login**: Click "Login" tab → Enter credentials
3. **Review Submissions**: 
   - View pending books in "Pending Book Approvals"
   - Click "Preview File" to see content
   - Click "Open File" for full review
4. **Approve/Reject Books**: 
   - Select one or more submissions
   - Enter rejection reason when needed
   - Click "Approve" or "Reject"
   - Confirm action in dialog
5. **Manage Users**: View, add, edit, activate, or deactivate users in "User Directory"
6. **Review Borrowed Records**: Search system-wide borrowed-book records
7. **Manage Profile / Notifications**: Update own profile and review librarian notifications

---

## Data Management

### Persistent Storage
- All data is automatically saved to `data/lms-data.dat`
- Data is loaded when the application starts
- All user credentials are encrypted and securely stored

### User Data Stored
- Registration information (username, full name, role)
- Encrypted passwords
- User profiles (bio, employee ID, avatar path)
- Author submission history
- Notification records
- Session recovery state
- Bookmark and PDF highlight data
- Draft data for unfinished author submissions

### Book Data Stored
- Book metadata (title, author, genres, summary)
- Book file path and content
- Approval status and timestamps
- Availability information
- Borrowing state, due dates, and borrow counts

---

## Error Handling & Edge Cases

The system handles various edge cases:

- **Duplicate Username**: Prevents duplicate user registrations
- **Weak Passwords**: Validates password strength requirements
- **Unavailable Books**: Prevents borrowing of already-borrowed books
- **File Not Found**: Graceful handling of missing book files
- **Invalid Credentials**: Clear error messages for authentication failures
- **Concurrent Operations**: Data consistency across simultaneous user actions
- **PDF Rendering**: Fallback preview mechanisms if PDF rendering fails

---

## Support & Troubleshooting

### Common Issues

**Application won't start**
- Ensure Java 21 is installed: `java -version`
- Check Maven is installed: `mvn -version`
- Run `mvn clean install` before launching

**UI appears scaled incorrectly**
- The application requires minimum 1100x700 resolution
- Try maximizing the window

**Data not persisting**
- Ensure `data/` directory exists and is writable
- Check file permissions on `data/lms-data.dat`

**PDF Preview not working**
- Verify the PDF file path is correct
- Ensure PDFBox library is properly installed via Maven

### Getting Help

For issues or questions:
1. Check existing error messages in the UI
2. Review the test files in `src/test/`
3. Consult the Project Documentation (`PHASE1_IMPLEMENTATION.md`)

---

## Development Notes

### Key Components

- **App.java**: Main JavaFX application with 1320+ lines containing all UI logic
- **DataStore.java**: Handles all data persistence, retrieval, and business logic
- **Password Security**: PasswordUtil handles secure password operations

### UI Design Principles

- Consistent styling across all portals
- Role-based dashboard customization
- Real-time filtering and search
- Confirmation dialogs for critical actions
- Clear success/error messaging
- Shared header with home/profile/notification/crash-test navigation

### Code Quality

- Well-commented codebase
- Proper error handling throughout
- MVC architectural pattern
- Reusable component builders
- Type-safe data structures

---

## License

This project is part of the COMP3111 course at HKUST.

---

## Contributors

HKUST E-Library System - Phase 2 Implementation

---

## Changelog

### Version 2.0 (Phase 2) - Current
- ✅ Borrowed-book PDF reading with bookmark/highlight support
- ✅ Notification boards for all three portals
- ✅ Shared profile management with validation and password re-authentication
- ✅ Multi-selection borrowing, borrow-limit checks, and duration validation
- ✅ Author published/submitted book management and bulk delete support
- ✅ Librarian bulk review workflow with rejection reason support
- ✅ Add/edit/deactivate user management tools
- ✅ Borrowed books record view for librarians
- ✅ Crash Test button and session restoration support

### Version 1.0 (Phase 1)
- ✅ Student/Staff registration and login
- ✅ Author registration and login  
- ✅ Librarian registration and login
- ✅ Book browsing with advanced filtering
- ✅ Book borrowing and returning
- ✅ Author book submission with draft saving
- ✅ Librarian approval/rejection workflow
- ✅ User management dashboard
- ✅ Book preview and summary viewing
- ✅ Personalized recommendations
- ✅ Comprehensive statistics dashboard
- ✅ Secure data persistence

---

## Contact

For questions or feedback regarding this project, please contact your course instructor or teaching assistant.


