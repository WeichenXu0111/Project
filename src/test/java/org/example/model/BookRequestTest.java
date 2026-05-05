package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BookRequestTest {

    @Test
    void requestCanMoveThroughDownloadAndFulfillmentStates() {
        BookRequest request = new BookRequest("student", "Student One", "AI Handbook", "A. Author", "Artificial Intelligence", "Course reading");

        request.markDownloaded("data/downloads/ai-handbook.pdf", "Downloaded from public source.");
        request.fulfill("book-123", "Uploaded by librarian.");

        assertEquals(BookRequest.Status.FULFILLED, request.getStatus());
        assertEquals("data/downloads/ai-handbook.pdf", request.getDownloadedFilePath());
        assertEquals("book-123", request.getUploadedBookId());
        assertEquals("Uploaded by librarian.", request.getLibrarianNote());
        assertNotNull(request.getProcessedAt());
    }
}
