package org.example.pdf;

import java.io.Serial;
import java.io.Serializable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages PDF text highlighting and annotations.
 * Supports adding, removing, and persisting highlights in PDF documents.
 */
public class PDFHighlightManager {
    private final String filePath;
    private final Map<String, List<HighlightData>> highlightsCache = new HashMap<>();

    public static class HighlightData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public final int page;
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        public final String text;
        public final long timestamp;

        public HighlightData(int page, float x, float y, float width, float height, String text) {
            this.page = page;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public PDFHighlightManager(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Add a text highlight to the PDF at the specified location
     */
    public void addHighlight(String username, int pageNum, float x, float y, float width, float height, String text) {
        String key = username + "::" + pageNum;
        highlightsCache.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new HighlightData(pageNum, x, y, width, height, text));
    }

    /**
     * Get all highlights for a specific user and page
     */
    public List<HighlightData> getHighlights(String username, int pageNum) {
        String key = username + "::" + pageNum;
        return new ArrayList<>(highlightsCache.getOrDefault(key, List.of()));
    }

    /**
     * Get all highlights for a specific user across all pages
     */
    public List<HighlightData> getAllHighlights(String username) {
        List<HighlightData> all = new ArrayList<>();
        highlightsCache.forEach((key, highlights) -> {
            if (key.startsWith(username + "::")) {
                all.addAll(highlights);
            }
        });
        return all;
    }

    /**
     * Remove a highlight
     */
    public void removeHighlight(String username, int pageNum, float x, float y) {
        String key = username + "::" + pageNum;
        List<HighlightData> highlights = highlightsCache.get(key);
        if (highlights != null) {
            highlights.removeIf(h -> Math.abs(h.x - x) < 2 && Math.abs(h.y - y) < 2);
        }
    }

    /**
     * Clear all highlights for a user on a specific page
     */
    public void clearPageHighlights(String username, int pageNum) {
        String key = username + "::" + pageNum;
        highlightsCache.remove(key);
    }

    /**
     * Export highlights to a plain text summary file.
     * @param outputPath Path to save the highlight summary
     */
    public void exportHighlightsToPDF(String username, String outputPath) {
        try {
            List<HighlightData> allHighlights = getAllHighlights(username);
            List<String> lines = new ArrayList<>();
            lines.add("Source PDF: " + filePath);
            lines.add("User: " + username);
            lines.add("Total highlights: " + allHighlights.size());
            lines.add("");

            for (HighlightData highlight : allHighlights) {
                lines.add(
                        "Page=" + highlight.page
                                + ", x=" + highlight.x
                                + ", y=" + highlight.y
                                + ", width=" + highlight.width
                                + ", height=" + highlight.height
                                + ", text=" + (highlight.text == null ? "" : highlight.text)
                );
            }

            Files.write(Path.of(outputPath), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load highlights from cache
     */
    public void loadHighlights(Map<String, List<HighlightData>> data) {
        highlightsCache.clear();
        highlightsCache.putAll(data);
    }

    /**
     * Serialize highlights for storage
     */
    public Map<String, List<HighlightData>> getHighlightsData() {
        return new HashMap<>(highlightsCache);
    }

    /**
     * Clear all highlights
     */
    public void clear() {
        highlightsCache.clear();
    }
}
