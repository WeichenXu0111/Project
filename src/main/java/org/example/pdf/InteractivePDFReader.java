package org.example.pdf;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive PDF reader with support for text selection and highlighting.
 * Provides visual feedback for selections and highlights on PDF pages.
 */
public class InteractivePDFReader {
    private final Image pageImage;
    private final int pageNumber;
    private final StackPane container;
    private final Canvas canvas;
    private final List<PDFHighlightManager.HighlightData> pageHighlights;
    private double scaleFactor = 1.0;
    
    // Selection tracking
    private Point2D selectionStart;
    private Point2D selectionEnd;
    private boolean isSelecting = false;
    
    // Callbacks
    private Runnable onSelectionChanged;
    private SelectionCallback onSelectionComplete;
    
    public interface SelectionCallback {
        void onTextSelected(String text, float x, float y, float width, float height);
    }

    public InteractivePDFReader(Image pageImage, int pageNumber) {
        this.pageImage = pageImage;
        this.pageNumber = pageNumber;
        this.pageHighlights = new ArrayList<>();
        this.container = new StackPane();
        this.canvas = new Canvas(pageImage.getWidth(), pageImage.getHeight());
        initializeUI();
    }

    private void initializeUI() {
        container.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");
        container.setMinSize(160, 120);
        container.setPrefSize(760, 520);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        container.getChildren().add(canvas);

        container.widthProperty().addListener((obs, oldV, newV) -> updateScale());
        container.heightProperty().addListener((obs, oldV, newV) -> updateScale());

        // Fit into a smaller default viewport on first open.
        updateScale();
        
        // Add mouse event handlers
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent event) {
        selectionStart = new Point2D(event.getX(), event.getY());
        isSelecting = true;
    }

    private void handleMouseDragged(MouseEvent event) {
        selectionEnd = new Point2D(event.getX(), event.getY());
        redraw(); // Redraw with selection rectangle
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        isSelecting = false;
        
        if (selectionStart != null && selectionEnd != null) {
            float dx = (float) Math.min(selectionStart.getX(), selectionEnd.getX());
            float dy = (float) Math.min(selectionStart.getY(), selectionEnd.getY());
            float dWidth = (float) Math.abs(selectionEnd.getX() - selectionStart.getX());
            float dHeight = (float) Math.abs(selectionEnd.getY() - selectionStart.getY());

            float x = (float) (dx / scaleFactor);
            float y = (float) (dy / scaleFactor);
            float width = (float) (dWidth / scaleFactor);
            float height = (float) (dHeight / scaleFactor);
            
            if (width > 5 && height > 5) { // Minimum selection size
                String selectedText = extractTextFromSelection(x, y, width, height);
                if (onSelectionComplete != null) {
                    onSelectionComplete.onTextSelected(selectedText, x, y, width, height);
                }
            }
        }
        
        selectionStart = null;
        selectionEnd = null;
        redraw();
    }

    /**
     * Extract approximate text from selection area (simplified)
     * In a real implementation, this would use OCR or PDF text extraction
     */
    private String extractTextFromSelection(float x, float y, float width, float height) {
        // This is a placeholder - real implementation would extract text from PDF
        return String.format("[Selected region: %.0f×%.0f at (%.0f, %.0f)]", width, height, x, y);
    }

    /**
     * Add a highlight to this page
     */
    public void addHighlight(PDFHighlightManager.HighlightData highlight) {
        if (highlight.page != pageNumber) {
            return;
        }
        // Avoid duplicate rectangles when data is reloaded for the same page.
        boolean exists = pageHighlights.stream().anyMatch(h ->
            h.x == highlight.x && h.y == highlight.y && h.width == highlight.width && h.height == highlight.height
        );
        if (!exists) {
            pageHighlights.add(highlight);
            redraw();
        }
    }

    /**
     * Remove a highlight from this page
     */
    public void removeHighlight(float x, float y) {
        pageHighlights.removeIf(h -> Math.abs(h.x - x) < 2 && Math.abs(h.y - y) < 2);
        redraw();
    }

    /**
     * Clear all highlights on this page
     */
    public void clearHighlights() {
        pageHighlights.clear();
        redraw();
    }

    /**
     * Redraw the canvas with page, highlights, and selection
     */
    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double drawWidth = canvas.getWidth();
        double drawHeight = canvas.getHeight();
        if (drawWidth <= 0 || drawHeight <= 0) {
            return;
        }
        
        // Draw original page image
        gc.clearRect(0, 0, drawWidth, drawHeight);
        gc.drawImage(pageImage, 0, 0, drawWidth, drawHeight);
        
        // Draw highlights
        for (PDFHighlightManager.HighlightData highlight : pageHighlights) {
            drawHighlightRect(gc, highlight.x, highlight.y, highlight.width, highlight.height);
        }
        
        // Draw current selection rectangle if selecting
        if (isSelecting && selectionStart != null && selectionEnd != null) {
            drawSelectionRect(gc, selectionStart, selectionEnd);
        }
    }

    private void updateScale() {
        double availableWidth = Math.max(1, container.getWidth() - 8);
        double availableHeight = Math.max(1, container.getHeight() - 8);
        double scaleX = availableWidth / pageImage.getWidth();
        double scaleY = availableHeight / pageImage.getHeight();
        double newScale = Math.min(scaleX, scaleY);
        if (!Double.isFinite(newScale) || newScale <= 0) {
            newScale = 1.0;
        }

        scaleFactor = newScale;
        canvas.setWidth(Math.max(1, pageImage.getWidth() * scaleFactor));
        canvas.setHeight(Math.max(1, pageImage.getHeight() * scaleFactor));
        redraw();
    }

    private void drawHighlightRect(GraphicsContext gc, float x, float y, float width, float height) {
        double baseX = x;
        double baseY = y;
        double baseW = width;
        double baseH = height;

        // Compatibility: treat legacy normalized values (0..1) as page-relative ratios.
        if (baseW > 0 && baseW <= 1.0 && baseH > 0 && baseH <= 1.0 && baseX >= 0 && baseX <= 1.0 && baseY >= 0 && baseY <= 1.0) {
            baseX *= pageImage.getWidth();
            baseY *= pageImage.getHeight();
            baseW *= pageImage.getWidth();
            baseH *= pageImage.getHeight();
        }

        double sx = Math.max(0, baseX * scaleFactor);
        double sy = Math.max(0, baseY * scaleFactor);
        double sw = Math.max(0, baseW * scaleFactor);
        double sh = Math.max(0, baseH * scaleFactor);

        // Clip to visible canvas area so off-canvas values do not hide valid parts.
        if (sx >= canvas.getWidth() || sy >= canvas.getHeight()) {
            return;
        }
        sw = Math.min(sw, canvas.getWidth() - sx);
        sh = Math.min(sh, canvas.getHeight() - sy);
        if (sw < 2 || sh < 2) {
            return;
        }

        // Draw a visible semi-transparent yellow rectangle.
        gc.setFill(Color.color(1.0, 0.95, 0.0, 0.45));
        gc.fillRect(sx, sy, sw, sh);
        gc.setStroke(Color.color(0.95, 0.6, 0.0, 0.95));
        gc.setLineWidth(1.5);
        gc.strokeRect(sx, sy, sw, sh);
    }

    private void drawSelectionRect(GraphicsContext gc, Point2D start, Point2D end) {
        float x = (float) Math.min(start.getX(), end.getX());
        float y = (float) Math.min(start.getY(), end.getY());
        float width = (float) Math.abs(end.getX() - start.getX());
        float height = (float) Math.abs(end.getY() - start.getY());
        
        // Draw a blue rectangle for selection
        gc.setStroke(Color.color(0.0, 0.5, 1.0, 0.8)); // Blue border
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);
        gc.setFill(Color.color(0.0, 0.5, 1.0, 0.2)); // Light blue fill
        gc.fillRect(x, y, width, height);
    }

    /**
     * Get the UI node for this reader
     */
    public Node getNode() {
        return container;
    }

    /**
     * Get current page highlights
     */
    public List<PDFHighlightManager.HighlightData> getHighlights() {
        return new ArrayList<>(pageHighlights);
    }

    /**
     * Set callback for when selection completes
     */
    public void setSelectionCallback(SelectionCallback callback) {
        this.onSelectionComplete = callback;
    }

    /**
     * Set callback for when selection changes
     */
    public void setSelectionChangedCallback(Runnable callback) {
        this.onSelectionChanged = callback;
    }

    /**
     * Get current selection bounds
     */
    public String getSelectionInfo() {
        if (selectionStart == null || selectionEnd == null) {
            return "No selection";
        }
        float x = (float) Math.min(selectionStart.getX(), selectionEnd.getX());
        float y = (float) Math.min(selectionStart.getY(), selectionEnd.getY());
        float width = (float) Math.abs(selectionEnd.getX() - selectionStart.getX());
        float height = (float) Math.abs(selectionEnd.getY() - selectionStart.getY());
        return String.format("Selection: %.0f×%.0f pixels", width, height);
    }
}
