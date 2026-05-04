# Library Management System
## Phase 3 – Advanced Features

[IMPORTANT] Instruction for Phase 3 Development:
All students are required to continue building upon the existing Library Management System developed in Phase 1 and Phase 2. Phase 3 is not a new project; it is a direct extension of the same application. You must integrate the new features and enhancements into the current system architecture rather than starting from scratch or creating a separate application. Failure to maintain continuity will result in incomplete project progression and may affect evaluation.

---

## Task 1 Student/Staff Portal

In this task, you need to further extend the student/staff functionalities. Particularly, you need to perform 3 main subtasks.

### Task 1.8 Reading History:
Create a screen that displays the reading history of each student/staff.

Show details such as Book Title, Author, Borrow Date, Return Date, and Reading Duration.
Provide filtering and search functionality (e.g., by date range, author, or genre).
Ensure history is updated automatically whenever a book is borrowed and read.
Keep track of reading progress.

### Task 1.9 Review/Rate Books:
Implement functionality for students/staff to review and rate books they have borrowed.

Allow users to submit written reviews and assign ratings (e.g., 1–5 stars).
Display average ratings and reviews on the available book’s screen (to all the users for all books).
Keep record of the review and rating submitted by the user for the books he borrowed.

### Task 1.10 Request for a New Book:
Create a form that allows students/staff to request new books to be added to the library.

Users must provide details such as Title, Author, Genre, and Reason for Request.
Send the request to the librarian portal for review and approval.
Provide confirmation feedback to the user once the request is submitted.
Send notification to the user once the request is approved and book is uploaded to the system by Librarian.

---

## Task 2 Author Portal

In this task, you need to further extend the author functionalities. Particularly, you need to perform 3 main subtasks.

### Task 2.7 LLM Model to Generate Book Summary:
Integrate a Large Language Model (LLM) to automatically generate a book summary when an author uploads a new book.

Ensure the generated summary is concise, accurate, and relevant.
Allow authors to edit or refine the generated summary before submission.
Provide confirmation feedback once the summary is finalized.

### Task 2.8 View Stats Screen:
Create a screen that displays statistics related to the author’s published books.

Show metrics such as number of reads, average ratings, reviews, and borrow counts.
Provide graphical representations (bar charts and Pie chart) for better visualization. You can decide by yourself about which relevant/important information you want to show in graphical form.

### Task 2.9 Review and Feedback Handling:
Implement functionality for authors to view and respond to reviews/feedback on their books.

Display all reviews and ratings submitted by students/staff.
Allow authors to reply to feedback or flag inappropriate reviews.
Send reply as notification to students/staff who submitted the feedback.

---

## Task 3 Librarian Portal

In this task, you need to further extend the librarian functionalities. Particularly, you need to perform 2 main subtasks.

### Task 3.8 Manage Published Books Screen:
Create a screen that allows librarians to manage all published books in the system.

Provide functionality to modify book details published by any author.
Allow librarians to add new books directly into the system.
Require all necessary details (Title, Author Names, Genre, Description (generate using LLM models), File Upload, Cover Upload) as specified in Author Portal tasks.
Ensure proper validation and confirmation dialogs for edits and additions.

### Task 3.9 Manage New Book Requests and Download Requested Books:
Create a screen that allows librarians to manage book requests submitted by students/staff.

Provide functionality to approve or reject requests.
Implement tools (e.g., web crawlers or ML-based tools) to download requested books if available online.
If a downloaded book does not include a summary/description, generate one using the LLM model.
Provide confirmation feedback once the request is processed.

---

# Phase 3 – Nice to have Features

Here is the list of nice-to-have features that can improve our advanced system/tasks.

## For Task 1.5 (Borrowed Book Screen):
Return Reminder Warnings: Send reminders/warnings before the due date to encourage timely returns. Send the warning even if the user is not login into the system.
Auto-return Notifications: Send the notifications to the users once the book is auto-returned after the borrowing period expires. Send the notification even if the user is not login into the system.

## For Task 1.6 (Manage Profile Screen):
Profile Picture Upload (Optional): Allow users to upload a profile picture with validation for format and size.
Password Strength Meter: Show real-time feedback when updating passwords.

## For Task 1.7 (Notification Board):
Priority Notifications: Highlight urgent notifications (e.g., auto-return books, book deletion by librarian) at the top.
Mark as Read & Delete Notifications: Allow users to mark the notification as read and allow them to delete the notifications as well.

## For Task 2.4 (Published Book Screen):
Modify/Edit Book Details: Allow authors to modify the book only if the book is under pending approval (not published) OR not borrowed by any students/staff (if published).

## For Task 2.5 (Manage Profile Screen):
Password Strength Meter: Show real-time feedback when updating passwords.
Auto logout from system: If the password is changed, the system must automatically logout the current user.

## For Task 2.6 (Notification Board):
Priority Notifications: Highlight urgent notifications (e.g., rejection feedback, book deletion by librarian) at the top.
Archive Notifications: Enable users to archive old notifications for better organization.
Unread Notification Counter: Display the number of unread notifications.

## For Task 3.4 (Manage All Users Screen):
Role-Based Filters: Allow librarians to filter users by role (student, staff, author, librarian).
Activity Log: Show recent activity of each user (e.g., last login, No. of borrowed books).
Bulk Account Actions: Enable librarians to deactivate or update multiple accounts at once.
Manage Librarians Account: Allow librarians to manage other librarian accounts.

## For Task 3.5 (Manage Own Profile Screen):
Profile Picture Upload (Optional): Allow users to upload a profile picture with validation for format and size.
Password Re-authentication: Ask users to re-enter the password if there are any changes to the profile.
Auto logout from system: If the password is changed, the system must automatically logout the current user.

## For Task 3.6 (Borrowed Books Record Screen):
Advanced Filters: Filter by overdue books, active borrowings, or returned books.
Export Records: Allow exporting borrowed book records to CSV/Excel for reporting.
Overdue Highlighting: Mark overdue books in red for quick identification.

## For Task 3.7 (Notification Board):
Priority Notifications: Highlight urgent notifications (e.g., submissions requests, user profile updates or any special request) at the top.
Mark as Read & Delete Notifications: Allow users to mark the notification as read and allow them to delete the notifications as well.

## For Task 1.8 (Reading History):
Export History: Allow users to export their reading history as PDF/CSV for personal record keeping.
Graphical Insights: Provide charts showing reading trends (e.g., most read genres, average reading duration).
Bookmark Integration: Link reading history with bookmarks to show where the user left off.
Achievements/Badges: Award badges for milestones (e.g., “Read 10 books this semester”).

## For Task 1.9 (Review/Rate Books):
Anonymous Reviews Option: Allow users to submit reviews anonymously.
Review Sorting: Allow sorting reviews by most recent, or most helpful.

## For Task 1.10 (Request for a New Book):
Request Tracking: Allow users to track the status of their request (Pending, Approved, Rejected).
Duplicate Request Detection: Notify users if the same book has already been requested.
Priority Requests: Allow librarians to mark urgent requests (e.g., course-related books).
Request History: Maintain a log of all requests submitted by the user.

## For Task 2.7 (LLM Model to Generate Book Summary):
Multiple Summary Styles: Provide options for short, medium, or detailed summaries.

## For Task 2.8 (View Stats Screen):
Customizable Dashboard: Allow authors to choose which metrics to display.
Download Reports: Enable exporting stats in PDF/Excel format.
Trend Analysis: Show borrowing trends over time (weekly, monthly).

## For Task 2.9 (Review and Feedback Handling):
Sentiment Analysis: Use AI to classify reviews as positive, neutral, or negative.
Reply Templates: Provide quick reply templates for common responses.
Feedback Analytics: Show aggregated statistics on review sentiment and ratings.

## For Task 3.8 (Manage Published Books Screen):
Bulk Edit/Delete: Allow librarians to manage multiple books at once.
Version History: Maintain a log of changes made to book details.
Advanced Filters: Filter books by genre, author, or approval status.

## For Task 3.9 (Manage New Book Requests and Download Requested Books):
Request Prioritization: Highlight requests based on urgency or popularity.
Auto-Suggest Alternatives: If requested book is unavailable, suggest similar titles.
Notify the user: If the similar title books from authors are downloaded, inform the user who made the book request.
Download Progress Indicator: Show progress bar when downloading requested books.
Request Analytics: Provide statistics on most requested genres/authors.
Downloaded book stats: Same as author dashboard book stats screen, make a similar book stats screen where librarian can view the stats for downloaded books only.