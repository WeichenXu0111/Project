package org.example.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {

    @Test
    void constructorInitializesPendingBookAndGenreDisplay() {
        Book book = new Book(
                "Clean Architecture",
                "unclebob",
                "Robert C. Martin",
                List.of("Software Engineering", "Programming"),
                "A book about software design.",
                "data/clean-architecture.pdf"
        );

        assertEquals(BookStatus.PENDING_APPROVAL, book.getStatus());
        assertEquals("Software Engineering / Programming", book.getGenreDisplay());
        assertFalse(book.isAvailable());
    }

    @Test
    void approveSetsAvailableStatusAndApprovedDate() {
        Book book = new Book("Refactoring", "martin", "Martin Fowler", "Software Engineering", "desc", "file.pdf");
        LocalDate approvedDate = LocalDate.of(2026, 4, 20);

        book.approve(approvedDate);

        assertTrue(book.isAvailable());
        assertEquals(BookStatus.APPROVED_AVAILABLE, book.getStatus());
        assertEquals(approvedDate, book.getApprovedDate());
    }

    @Test
    void borrowAndReturnBookUpdatesBorrowState() {
        Book book = new Book("Patterns", "gof", "GOF", "Computer Science", "desc", "file.pdf");
        book.approve();

        book.borrow("student1");

        assertEquals(BookStatus.BORROWED, book.getStatus());
        assertEquals("student1", book.getBorrowedBy());
        assertNotNull(book.getBorrowedDate());
        assertEquals(book.getBorrowedDate().plusDays(14), book.getDueDate());
        assertEquals(1, book.getBorrowCount());

        book.returnBook();

        assertEquals(BookStatus.APPROVED_AVAILABLE, book.getStatus());
        assertNull(book.getBorrowedBy());
        assertNull(book.getBorrowedDate());
        assertNull(book.getDueDate());
    }

    @Test
    void setGenresFiltersBlankValuesAndTrimsEntries() {
        Book book = new Book("AI", "alice", "Alice", "Artificial Intelligence", "desc", "file.pdf");
        List<String> inputGenres = new ArrayList<>();
        inputGenres.add("  AI  ");
        inputGenres.add("");
        inputGenres.add("   ");
        inputGenres.add(null);
        inputGenres.add("Machine Learning");

        book.setGenres(inputGenres);

        assertEquals(List.of("AI", "Machine Learning"), book.getGenres());
        assertEquals("AI / Machine Learning", book.getGenre());
    }
}
