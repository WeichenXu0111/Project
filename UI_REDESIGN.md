# UI Redesign Notes (Phase 1)

## Goal
Align the JavaFX UI with a more refined, university-library style inspired by the HKUST Library site while keeping all Phase 1 features unchanged.

## What Changed

### 1) Landing Page Layout
- Replaced the single card landing layout with a hero panel plus three portal cards (Student/Staff, Author, Librarian).
- Each portal card now shows a short description and a single action button.
- The landing layout keeps the same navigation flow and does not remove any functionality.

Files:
- `src/main/java/org/example/App.java`

### 2) Header Branding
- Updated the header to include a brand block (title + subtitle) for a more institutional look.
- Navigation buttons use a lighter, transparent style to blend with the header gradient.

Files:
- `src/main/java/org/example/App.java`
- `src/main/resources/styles.css`

### 3) Color Palette and Typography
- Introduced a navy/teal palette with soft neutrals for backgrounds and cards.
- Added small caps pill styling for phase labeling and muted text for descriptions.
- Refined button and table styles to match the new theme.

Files:
- `src/main/resources/styles.css`

## New / Updated Style Classes
- `app-shell`, `brand-subtitle`, `nav-button`, `ghost-button`
- `hero-pill`, `hero-panel`, `portal-card`, `muted-text`
- Updated `primary-button`, `secondary-button`, `card`, and table styles

## Functional Coverage
- All Phase 1 flows remain the same: registration, login, book submission, approval, and borrowing.
- Only presentation and layout were adjusted; no data or logic changes were made.

