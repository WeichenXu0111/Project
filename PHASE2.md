# Phase 2 – Extended Features
## [IMPORTANT] Instruction for Phase 2 Development
All students are required to continue building upon the existing Library Management System developed in Phase 1. Phase 2 is not a new project; it is a direct extension of the same application. You must integrate the new features and enhancements into the current system architecture rather than starting from scratch or creating a separate application. Failure to maintain continuity will result in incomplete project progression and may affect evaluation.

## Task 1 Student/Staff Portal
In this task, you need to extend the student/staff functionalities. Particularly, you need to perform 3 main subtasks.

### Task 1.5 Borrowed Book Screen
Create a screen that displays all books currently borrowed by the student/staff.
- Allow users to read the book in PDF format.
- Implement bookmark functionality so users can save their reading progress.
- Add test highlight functionality to the PDF so users can highlight text for their record.
- Provide book return functionality with two options:
  - Auto-return when the borrowing period expires.
  - Self-return option for users to return books before the due date.
- Update the book’s availability status in real time upon return.

### Task 1.6 Manage Profile Screen
Create a screen where students/staff can manage their personal profile.
- Allow editing of core personal details (Full Name, Password).
- Implement validation rules for all updated credentials.
- Provide clear confirmation feedback for both successful and failed profile updates.

### Task 1.7 Notification Board
Create a dedicated notification board for students/staff users.
- Display system-generated notifications including:
  - Book due date reminders.
  - Book deletion notices (only sent to users who borrowed the specific book).
  - Other important system announcements (if any).
- Ensure all notifications are clear, timestamped, and categorized for easy viewing.

## Task 2 Author Portal
In this task, you need to extend the author functionalities. Particularly, you need to perform 3 main subtasks.

### Task 2.4 Published Book Screen
Create a screen that displays all books published or submitted by the author.
- Show key book details: Title, Genre, Status (Approved/Rejected/Pending).
- Provide functionality to modify and edit book details.
- Allow authors to delete the books they have submitted.
- Implement proper validation and confirmation dialogs for all edit and deletion operations.

### Task 2.5 Manage Profile Screen
Create a screen where authors can manage their personal profile.
- Allow editing of personal details (Full Name, Password, Bio).
- Implement validation rules for all updated credentials.
- Provide clear confirmation feedback for both successful and failed profile updates.

### Task 2.6 Notification Board
Create a dedicated notification board for author users.
- Display system-generated notifications including:
  - Real-time book approval/rejection updates.
  - Other important system announcements (if any).
- Ensure all notifications are clear, timestamped, and categorized for easy viewing.

## Task 3 Librarian Portal
In this task, you need to extend the librarian functionalities. Particularly, you need to perform 4 main subtasks.

### Task 3.4 Manage All Users Screen
Create a screen that allows librarians to manage all registered system users (students, staff, authors, librarians).
- Provide full user management functionality: view, edit, deactivate user accounts.
- Implement proper validation and confirmation dialogs for all account change operations.

### Task 3.5 Manage Own Profile Screen
Create a screen where librarians can manage their personal profile.
- Allow editing of personal details (Full Name, Password, Employee ID).
- Implement validation rules for all updated credentials.
- Provide clear confirmation feedback for both successful and failed profile updates.

### Task 3.6 Borrowed Books Record Screen
Create a screen that displays a comprehensive record of all borrowed books across the entire system.
- Show detailed borrowing information: Book Title, Borrower Username, Borrow Date, Return Date, Status.
- Implement filtering and search functionality for efficient record management.

### Task 3.7 Notification Board
Create a dedicated notification board for librarian users.
- Display system-generated notifications including:
  - New book submission alerts.
  - User account update notifications.
  - Other important system announcements (if any).
- Ensure all notifications are clear, timestamped, and categorized for easy viewing.
# Nice to Have Features (Phase 2)

## Task 1.4
- **Multiple Selections**: Allow users to select multiple books that user wants to borrow from system.
- **Limit Borrow**: The system should have some logical limit of number of books a user can borrow at a time (for example 5 books at max). Show appropriate error message.
- **Max/Min Duration**: The system should restrict the user to borrowing a book for max number of days (for example 10–14 days) also have some limit to check minimum time of borrowing (for example not less than 0 seconds). Show appropriate error message.

## Task 2.3
- **Book Cover Image Upload**: Allow authors to optionally upload a cover image for the book during publication. Validate image format (e.g., JPG, PNG) and size limits to ensure compatibility.

## Task 3.3
- **Rejection Reason**: Require or allow librarians to enter a brief reason for rejection, which can be stored and optionally sent as feedback to the author.
- **Search and Filter Submissions**: Enable librarians to search pending books by title, author, genre, or submitted date, and filter by status to manage large lists efficiently.
- **Bulk Actions**: Allow librarians to select multiple pending submissions and approve or reject them in bulk, with a confirmation step to prevent errors.

---

# Whole System – Persistent Crash Recovery Feature
*(Proposed by Prof. Charles)*

Implemented across: Student/Staff Portal, Author Portal, Librarian Portal.

### Features
- **Random Crash Simulation**: The system should be able to simulate random crashes during runtime to test resilience.
- **Automatic Restoration**: When the application is reopened after a crash, the system should restore:
  - The exact screen the user was on.
  - The data and progress as it were before the crash.
  - Any temporary actions (e.g., highlights, drafts, notifications) that were not yet finalized.

### Validation & Feedback
- Show a confirmation message that the system has restored the last session successfully.
- If restoration fails, provide a clear error message and fall back to the home screen.

### Implementation & Testing
- **Forced Application Exit**
  - Close the application abruptly (e.g., kill the process or use Task Manager) while a user is still logged in into the system.
  - When reopened, check if the last viewed screen and data are restored.
- **Mock Crash Button**
  - Require developers to implement a “Crash Test” button that simulates a crash event.
  - On reopening, the app should reload the last session.

---

# Task 1.5 – Borrowed Book Screen
- **Closed Book Reading Screen**: If the user is reading a book and borrowing period expires, the system must close the reading screen automatically before auto-return.
- **Search and Filter Books**: Allow users to find books faster as the library grows. Users can search by title, author; filter by genre, publish date, availability.
- **Partial Return Option**: If multiple books are borrowed, allow users to return selected ones early. Users can select one book or multiple books to return them back.

# Task 1.6 – Manage Profile Screen
- **Auto logout from system**: If the password is changed, the system must automatically logout the current user.
- **Password Re-authentication**: Ask users to re-enter the password if there are any changes to the profile.

# Task 1.7 – Notification Board
- **Archive Notifications**: Enable users to archive old notifications for better organization.
- **Unread Notification Counter**: Display the number of unread notifications.
- **Search and Filter Notifications**: Enable filtering by category (due reminders, announcements, deletions, etc.).

---

# Task 2.4 – Published Book Screen
- **Delete Books**: Allow authors to delete the book only if the book is under pending approval (not published) OR not borrowed by any students/staff (if published).
- **Bulk Delete**: Allow authors to manage multiple books at once with confirmation dialogs.
- **Read Books**: Allow authors to read the book they have uploaded (both published and un-published books).

# Task 2.5 – Manage Profile Screen
- **Password Re-authentication**: Ask users to re-enter the password if there are any changes to the profile.
- **Profile Picture Upload (Optional)**: Allow users to upload a profile picture with validation for format and size.

# Task 2.6 – Notification Board
- **Search and Filter Notifications**: Enable filtering by category (book acceptance, book rejection, book deletion, etc.).
- **Mark as Read & Delete Notifications**: Allow users to mark the notification as read and allow them to delete the notifications as well.

---

# Task 3.4 – Manage All Users Screen
- **Add New User**: Allow librarian to add a new account of any type of user (e.g., student, staff, author, librarian).

# Task 3.5 – Manage Own Profile Screen
- **Password Strength Meter**: Show real-time feedback when updating passwords.

# Task 3.7 – Notification Board
- **Archive Notifications**: Enable users to archive old notifications for better organization.
- **Unread Notification Counter**: Display the number of unread notifications.
- **Search and Filter Notifications**: Enable filtering by type, date, or urgency.