package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookReviewTest {

    @Test
    void replyStoresAuthorResponseTimestamp() {
        BookReview review = new BookReview("book-1", "student", "Student One", 5, "Very useful.", false);

        review.reply("Thanks for the thoughtful feedback.");

        assertEquals("Thanks for the thoughtful feedback.", review.getAuthorReply());
        assertNotNull(review.getRepliedAt());
    }

    @Test
    void flagStoresReasonAndDisplayState() {
        BookReview review = new BookReview("book-1", "student", "Student One", 2, "Spam text", false);

        review.flag("Contains inappropriate language.");

        assertTrue(review.isFlagged());
        assertEquals("Contains inappropriate language.", review.getFlagReason());
        assertEquals("Flagged", review.getFlagDisplay());
    }
}
