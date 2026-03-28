# Phase 1 - HKUST E-Library System (JavaFX)

## What this is
A JavaFX desktop app that covers Phase 1 requirements:
- Student/Staff registration and login
- Author registration and login
- Librarian registration and login
- Author book submission
- Librarian approval/rejection
- Student/Staff available books and borrow flow

## Requirements
- Java 21
- Maven 3.9+

## Run
```powershell
mvn -q javafx:run
```

## Test
```powershell
mvn -q test
```

## Data
Data is stored locally in `data/lms-data.dat` using Java serialization.

