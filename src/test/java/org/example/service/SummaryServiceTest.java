package org.example.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryServiceTest {

    @Test
    void generateSummaryUsesMetadataWhenNoFileIsAvailable() {
        String summary = SummaryService.generateSummary(
                "Distributed Systems",
                "Jane Author",
                List.of("Distributed Computing", "Cloud Computing"),
                "");

        assertTrue(summary.contains("Distributed Systems"));
        assertTrue(summary.contains("Jane Author"));
        assertTrue(summary.contains("Distributed Computing"));
    }

    @Test
    void refineSummaryReturnsConciseEditableText() {
        String refined = SummaryService.refineSummary(
                "This book introduces secure software design. It has many examples.",
                "Security Design",
                List.of("Security"));

        assertFalse(refined.isBlank());
        assertTrue(refined.contains("concise catalog description"));
    }

    @Test
    void generateSummarySupportsDifferentStyles() {
        String shortSummary = SummaryService.generateSummary("Algorithms", "A. Author", List.of("Computer Science"), "", "Short");
        String detailedSummary = SummaryService.generateSummary("Algorithms", "A. Author", List.of("Computer Science"), "", "Detailed");

        assertTrue(detailedSummary.length() > shortSummary.length());
        assertTrue(detailedSummary.contains("detailed version"));
    }

    @Test
    void analyzeSentimentClassifiesSimpleFeedback() {
        assertTrue(SummaryService.analyzeSentiment("Excellent and helpful book").equals("Positive"));
        assertTrue(SummaryService.analyzeSentiment("Confusing and terrible structure").equals("Negative"));
        assertTrue(SummaryService.analyzeSentiment("It covers the topic").equals("Neutral"));
    }
}
