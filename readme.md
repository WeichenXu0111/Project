# Library Management System for HKUST (E-Library System)

## Project Overview

In this project, you need to develop a Library Management System for HKUST (E-Library System). The system is useful for students/staff, authors, and librarians.

From this system:

- Students/Staff can log in/register into the system, see the available books, borrow and return the book, read the book, manage their own profile, and so on...
- Authors can log in/register into the system, view/modify/delete a book published by them, upload a new book, view book stats, manage their own profile, and so on...
- Librarians can log in/register into the system, manage all books, manage all users, manage their own profile, and so on...

---

## Project Details

- Group of 3 students (max)
- Each student will be responsible for 1 main task. And each main task is divided into sub-tasks.
- Three phases of incremental development
    - Each phase will introduce a set of new features
    - A complete system must be built at the end of each phase
    - Must complete both requirements, modeling and implementation
    - Must be incremental, not a new application
- Crazy customer mode
    - One week before the deadline of each phase, the crazy customer will ask for a new (reasonable) feature to be built
    - Evaluation is the same as regular features

---

## Important Note

- UI design/style should be consistent across all three main tasks (Task 1, 2, 3)
- All the functionalities should be in working form for each subtask.
- Test the edge cases based on your knowledge from the real world
- For all the tasks, you need to handle exceptional handling (edge cases) by yourself (Means: You need to think about how to implement logically perfect functionality)

---

## Main Tasks in all Phases

### Task 1

- Handle the student/staff login and registration
- Available book screen (to borrow a book)
- Borrowed book screen
- Read the book as a PDF (add bookmarks and watermarks)
- Return books
- Reading history
- Review/Rate books
- Manage profile screen
- Request for a new book
- Notification board

### Task 2

- Handle the author login and registration
- Published book screen
- Publish new book screen
- LLM model to generate a book summary
- View stats screen
- Review and feedback handling
- Manage profile screen
- Notification board

### Task 3

- Handle the librarian login and registration
- Pending approval, book screen
- Manage all users' screens
- Manage own profile screen
- Borrowed books record screen
- Manage Published Books screen
- Modify the book details published by any author
- Manage new book requests and Download the requested books
- Using a web crawler or ML tools
- Notification board

---

## Tentative Important Dates to Remember

- Group formation starts from 16th February (after add/drop)
- Group formation deadline: 22nd February (1 week)
- Release Phase 1: 23rd February
- Phase 1 deadline: 12th March 06:00 PM
- Phase 1 project demo: 12th March 06:00-08:00 PM
- Release Phase 2: 13th March
- Phase 2 deadline: 02nd April 06:00 PM
- Phase 2 project demo: 02nd April 06:00-08:00 PM
- Release Phase 3: 03rd April
- Phase 3 deadline: 07th May 06:00 PM
- Final Project Demo: 07th May 06:00-08:00 PM

---

## Technical Requirements

- You can use any programming language to build this project. There is no restriction.
- Attention: As announced by Prof. Charles, you must obtain his approval before using any programming language for your project; Java is exempt.
- There is no skeleton code provided to any group. You need to build your project from scratch (frontend + backend).
- You are allowed to use AI tools, but not recommended.

---

## Project Discussion Rules

- You should not discuss things between different teams related to project requirements
- You should not share code with other teams (as there will be a plagiarism check)
- You should not post your code on Canvas discussion or other public resources (e.g., GitHub)
- You should not disscuss project related things in telegram group (except looking for teammates)
- You can ask questions to repective TA assigned to your team if any confusion

---

## CHANGELOG

Updated information in 'Technical Requirements' @ 23-02-2026

### Requirements for All Tasks (Task 1, 2, 3) and for All Phases (Phase 1, 2, 3)

#### Non-functional Requirements

- The application should have a user-friendly interface.
- The application should have a consistent interface across all users (student, staff, author, librarian)
- It should be responsive and functional across different devices (if you are developing a Web, Android, and iOS-based application system).
- Ensure that data is securely stored, particularly all user’s credentials and book’s information.

#### Technical Requirements

- Use best coding practices, including readability and maintainability.
- Implement appropriate data structures for managing users and books.

#### Submission Guidelines

- Submit your project as a ZIP file containing all source code and relevant documentation.
- Include a README file with setup instructions and any additional requirements that are useful for TA for grading purposes.
- Code should be well-commented to clarify functionality.

---

# Phase 1 – Main Features

## Task 1 Student/Staff Portal

In this task, you need to manage the student/staff access and screens related to this user. Particularly, you need to perform 4 main subtasks.

- Student/Staff Registration
- Student/Staff Login
- Available book screen
- Borrow book

### Task 1.1 Student/Staff Registration

- Create a user interface that allows students and staff to register for an account.
- Users must provide the following information during registration:
    - Username (must be unique)
    - Full Name
    - Password (with validation rules, e.g., minimum length)
    - Role (Student or Staff)
- Implement error handling for registration failures (e.g., duplicate username, weak password, etc.).
- Ensure that data is stored securely, particularly user credentials.
- Provide feedback for successful or failed register attempts.

### Task 1.2 Student/Staff Login

- Implement a login screen where registered users can enter their credentials.
- Users must enter the following credentials to enter the system:
    - Username
    - Password
- Validate the credentials against the registered user database.
- Provide feedback for successful or failed login attempts.

### Task 1.3 Available Book Screen

- Create a screen that displays a list of available books (published by authors from task 2).
- Include the following details for each book:
    - Title
    - Author
    - Publish Date (the date when book is approved by librarian)
    - Availability Status
    - Book Abstract/Summary

### Task 1.4 Borrow Book

- Using the available book screen (task 1.3), implement functionality for users to borrow a book.
- Ensure that the system checks for availability before allowing a borrow action.
- Update the book's status to reflect that it has been borrowed.
- Provide confirmation to the user once a book is successfully borrowed.

---

## Task 2 Author Portal

In this task, you need to manage the author’s access and screens related to this user. Particularly, you need to perform 3 main subtasks.

- Author Registration
- Author Login
- Publish New Book

### Task 2.1 Author Registration

- Create a user interface that allows author to register for an account.
- Users must provide the following information during registration:
    - Username (must be unique)
    - Full Name
    - Password (with validation rules, e.g., minimum length)
    - Bio (optional)
- Implement error handling for registration failures (e.g., duplicate username, weak password, etc.).
- Ensure that data is stored securely, particularly user credentials.
- Provide feedback for successful or failed register attempts.

### Task 2.2 Author Login

- Implement a login screen where registered users can enter their credentials.
- Users must enter the following credentials to enter the system:
    - Username
    - Password
- Validate the credentials against the registered user database.
- Provide feedback for successful or failed login attempts.

### Task 2.3 Publish New Book

- Create a form that allows authors to publish new books.
- Authors must provide the following information for each book:
    - Title
    - Author Name (pre-filled with their registered Full Name)
    - Genre
    - Description (Abstract/Summary)
- Authors must upload a book file that they want to publish
- File format can be txt, pdf, word, etc. based on your application support (pdf is recommended)
- Upon submission, validate the book data and send this book publish request to librarian for his approval
- Provide confirmation to the user once a book is successfully submitted

---

## Task 3 Librarian Portal

In this task, you need to manage the librarian’s access and screens related to this user. Particularly, you need to perform 3 main subtasks.

- Librarian Registration
- Librarian Login
- Librarian New Books Approval Screen and Functionalities (approve/reject)

### Task 3.1 Librarian Registration

- Create a user interface that allows librarians to register for an account.
- Users must provide the following information during registration:
    - Username (must be unique)
    - Full Name
    - Password (with validation rules, e.g., minimum length)
    - Employee ID (optional)
- Implement error handling for registration failures (e.g., duplicate username, weak password, etc.).
- Ensure that data is stored securely, particularly user credentials.
- Provide feedback for successful or failed register attempts.

### Task 3.2 Librarian Login

- Implement a login screen where registered users can enter their credentials.
- Users must enter the following credentials to enter the system:
    - Username
    - Password
- Validate the credentials against the registered user database.
- Provide feedback for successful or failed login attempts.

### Task 3.3 Librarian New Books Approval Screen and Functionalities

- Create a screen that displays a list of new book submissions awaiting approval.
- For each submission, show the following details:
    - Title
    - Author Username
    - Author Full Name
    - Genre
    - Submitted Date
    - Status (Pending Approval)
- Provide functionality for the librarian to approve or reject submissions, with a confirmation dialog before finalizing the action.
- Update the status of the book upon approval or rejection and provide feedback to the librarian.

---

## Phase 1 – Nice to have Features

As announced, there will be some nice-to-have features from the crazy customer. These features for phase 1 will be released 1 week before the deadline of phase 1.