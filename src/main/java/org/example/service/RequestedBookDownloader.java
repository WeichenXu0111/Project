package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.BookRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestedBookDownloader {
    private static final int MAX_TEXT_CHARS = 24_000;
    private static final Pattern BOOK_LINK = Pattern.compile("href=[\"']([^\"']+\\.(?:pdf|txt|epub))(?:[?#][^\"']*)?[\"']",
            Pattern.CASE_INSENSITIVE);

    private RequestedBookDownloader() { }

    public record DownloadResult(boolean success, String message, Path filePath) { }

    public static DownloadResult download(String source, BookRequest request) {
        if (source == null || source.isBlank()) {
            return new DownloadResult(false, "Provide a direct PDF/TXT URL or a webpage URL to crawl.", null);
        }
        try {
            URI uri = URI.create(source.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return new DownloadResult(false, "Only HTTP/HTTPS URLs are supported.", null);
            }

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(12))
                    .build();
            HttpResponse<byte[]> response = fetch(client, uri);
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);

            if (contentType.contains("html")) {
                String html = new String(response.body(), StandardCharsets.UTF_8);
                Optional<URI> linkedBook = findBookLink(uri, html);
                if (linkedBook.isPresent()) {
                    response = fetch(client, linkedBook.get());
                    uri = linkedBook.get();
                    contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
                } else {
                    return saveTextAsPdf(stripHtml(html), request, "Crawled page text converted to PDF.");
                }
            }

            boolean pdf = contentType.contains("pdf") || uri.getPath().toLowerCase(Locale.ROOT).endsWith(".pdf")
                    || startsWithPdfMagic(response.body());
            if (pdf) {
                Path path = targetPath(request, ".pdf");
                Files.write(path, response.body());
                return new DownloadResult(true, "PDF downloaded.", path);
            }

            String text = new String(response.body(), StandardCharsets.UTF_8);
            return saveTextAsPdf(text, request, contentType.contains("text") ? "Text downloaded and converted to PDF." : "Downloaded content converted to PDF.");
        } catch (IllegalArgumentException ex) {
            return new DownloadResult(false, "Invalid URL.", null);
        } catch (IOException ex) {
            return new DownloadResult(false, "Download failed: " + ex.getMessage(), null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new DownloadResult(false, "Download interrupted.", null);
        }
    }

    private static HttpResponse<byte[]> fetch(HttpClient client, URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "HKUST-LMS/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response;
    }

    private static Optional<URI> findBookLink(URI base, String html) {
        Matcher matcher = BOOK_LINK.matcher(html);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(base.resolve(matcher.group(1)));
    }

    private static boolean startsWithPdfMagic(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    private static DownloadResult saveTextAsPdf(String text, BookRequest request, String message) throws IOException {
        Path path = targetPath(request, ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 11);
            content.setLeading(14);
            content.newLineAtOffset(54, 740);
            int lineCount = 0;
            for (String line : wrap(stripHtml(text), 88).split("\\R")) {
                if (lineCount >= 48) {
                    content.endText();
                    content.close();
                    page = new PDPage();
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 11);
                    content.setLeading(14);
                    content.newLineAtOffset(54, 740);
                    lineCount = 0;
                }
                content.showText(line.replaceAll("[^\\x20-\\x7E]", " "));
                content.newLine();
                lineCount++;
            }
            content.endText();
            content.close();
            document.save(path.toFile());
        }
        return new DownloadResult(true, message, path);
    }

    private static Path targetPath(BookRequest request, String extension) throws IOException {
        Files.createDirectories(Path.of("data", "downloads"));
        String base = safeFileName((request == null ? "requested-book" : request.getTitle() + "-" + request.getAuthor()));
        return Path.of("data", "downloads", base + "-" + System.currentTimeMillis() + extension);
    }

    private static String stripHtml(String value) {
        if (value == null) return "";
        String stripped = value.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        return stripped.length() > MAX_TEXT_CHARS ? stripped.substring(0, MAX_TEXT_CHARS) : stripped;
    }

    private static String wrap(String value, int width) {
        String text = value == null || value.isBlank() ? "Downloaded book content." : value.trim();
        StringBuilder out = new StringBuilder();
        for (String paragraph : text.split("\\R+")) {
            String[] words = paragraph.trim().split("\\s+");
            int col = 0;
            for (String word : words) {
                if (word.isBlank()) continue;
                if (col + word.length() + 1 > width) {
                    out.append('\n');
                    col = 0;
                }
                if (col > 0) {
                    out.append(' ');
                    col++;
                }
                out.append(word);
                col += word.length();
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static String safeFileName(String value) {
        String name = value == null ? "requested-book" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return name.isBlank() ? "requested-book" : name;
    }
}
