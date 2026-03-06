# Phase 1 Update Log (2026-03-06)

This document explains the changes made to satisfy the newly added Phase 1 nice-to-have requirements.

## Student/Staff Portal

- Added a filter bar for the catalog with title/author search, genre, publish date, and availability filters.
- Added quick preview and summary dialogs before borrowing.
- Added a borrow confirmation dialog showing duration and due date.
- Added a recommendations panel that suggests books based on borrowing history or popularity.
- Added availability styling so unavailable books are visually marked.

## Author Portal

- Replaced the single-genre input with a multi-select genre list.
- Added a preview dialog that formats title, genres, summary, and file path before submission.
- Added auto-save draft support that restores incomplete submissions on login.

## Librarian Portal

- Added "Preview File" and "Open File" actions for book content review in the approval screen.

## Data and Models

- Added multi-genre support to the `Book` model while preserving existing single-genre data.
- Added a standard genre list to `DataStore` for consistent author selection and filtering.
- Added recommendation logic based on borrowing history and book popularity.
- Ensured draft data is saved and loaded via the existing serialized storage.

## UI Styling

- Added styles for filter rows, genre chips, recommendation list, and unavailable rows.
- Kept the existing visual language and card layout for consistency.

## Files Updated

- `src/main/java/org/example/App.java`
- `src/main/java/org/example/data/DataStore.java`
- `src/main/java/org/example/model/Book.java`
- `src/main/resources/styles.css`
- `PHASE1_IMPLEMENTATION.md`

