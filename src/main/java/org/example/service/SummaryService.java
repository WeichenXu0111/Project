package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SummaryService {
    private SummaryService() { }

    public static String generateSummary(String title, String author, List<String> genres, String filePath) {
        String sourceText = extractSourceText(filePath);
        if (sourceText.isBlank()) {
            sourceText = String.join(" ",
                    safe(title),
                    safe(author),
                    genres == null ? "" : String.join(" ", genres));
        }

        List<String> keywords = keywords(sourceText, genres);
        String genreText = genres == null || genres.isEmpty() ? "the selected subject area" : String.join(", ", genres);
        String keyText = keywords.isEmpty() ? "core concepts, examples, and practical takeaways" : String.join(", ", keywords);

        return String.format(Locale.US,
                "%s by %s is a %s book focused on %s. The generated summary highlights the main ideas, likely learning outcomes, and practical value for library readers. Readers can expect concise coverage of %s, with enough context to decide whether the book matches their study or research needs.",
                blankAs(title, "This book"),
                blankAs(author, "the listed author"),
                genreText,
                keyText,
                keyText);
    }

    public static String refineSummary(String currentSummary, String title, List<String> genres) {
        String base = safe(currentSummary).isBlank()
                ? generateSummary(title, "", genres, "")
                : currentSummary.trim();
        String firstSentence = base.split("(?<=[.!?])\\s+", 2)[0];
        String genreText = genres == null || genres.isEmpty() ? "its topic" : String.join(", ", genres);
        return firstSentence + " It has been refined into a concise catalog description for " + genreText + " readers.";
    }

    private static String extractSourceText(String filePath) {
        if (filePath == null || filePath.isBlank() || filePath.startsWith("seed://")) return "";
        Path path;
        try {
            path = Path.of(filePath);
        } catch (RuntimeException ex) {
            return "";
        }
        if (!Files.isRegularFile(path)) return "";

        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(path.toFile())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(1);
                    stripper.setEndPage(Math.min(3, document.getNumberOfPages()));
                    return stripper.getText(document);
                }
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private static List<String> keywords(String text, List<String> genres) {
        Set<String> words = new LinkedHashSet<>();
        if (genres != null) {
            genres.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .forEach(words::add);
        }

        String cleaned = safe(text).replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase(Locale.ROOT);
        String[] parts = cleaned.split("\\s+");
        Set<String> stop = Set.of("the", "and", "for", "with", "that", "this", "from", "into", "book", "about",
                "will", "are", "you", "your", "its", "their", "have", "has", "was", "were", "can", "not");
        List<String> candidates = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 5 && !stop.contains(part)) candidates.add(part);
        }
        candidates.stream()
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .forEach(words::add);
        return words.stream().limit(6).collect(Collectors.toList());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankAs(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value.trim();
    }
}
