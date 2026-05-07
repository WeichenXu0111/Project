package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DeepSeekLlmClient {
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private DeepSeekLlmClient() { }

    public static boolean isConfigured() {
        if (Boolean.parseBoolean(value("DEEPSEEK_DISABLED", "deepseek.disabled", "false"))) return false;
        return !apiKey().isBlank();
    }

    public static Optional<String> generateSummary(String title, String author, List<String> genres, String sourceText, String style) {
        String prompt = """
                Generate a %s catalog summary for a library management system.
                Requirements:
                - Be concise, accurate, and relevant.
                - Do not invent facts that are not supported by the metadata or excerpt.
                - Return only the summary text.

                Title: %s
                Author: %s
                Genres: %s
                Source excerpt:
                %s
                """.formatted(
                blankAs(style, "medium"),
                blankAs(title, "Unknown"),
                blankAs(author, "Unknown"),
                genres == null || genres.isEmpty() ? "Unknown" : String.join(", ", genres),
                trimForPrompt(sourceText));
        return chat("You are a careful academic library catalog assistant.", prompt, 700);
    }

    public static Optional<String> refineSummary(String currentSummary, String title, List<String> genres) {
        String prompt = """
                Refine this catalog summary for a library book.
                Requirements:
                - Preserve the meaning.
                - Make it polished, concise, and suitable for a library catalog.
                - Return only the refined summary text.

                Title: %s
                Genres: %s
                Current summary:
                %s
                """.formatted(
                blankAs(title, "Unknown"),
                genres == null || genres.isEmpty() ? "Unknown" : String.join(", ", genres),
                blankAs(currentSummary, ""));
        return chat("You improve book summaries for an academic library system.", prompt, 500);
    }

    private static Optional<String> chat(String systemPrompt, String userPrompt, int maxTokens) {
        String key = apiKey();
        if (key.isBlank()) return Optional.empty();

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.2,
                  "max_tokens": %d,
                  "thinking": {"type": "disabled"}
                }
                """.formatted(model(), json(systemPrompt), json(userPrompt), maxTokens);

        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(12))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return extractContent(response.body())
                    .map(String::trim)
                    .filter(text -> !text.isBlank());
        } catch (IOException ex) {
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static URI endpoint() {
        String base = value("DEEPSEEK_BASE_URL", "deepseek.base.url", DEFAULT_BASE_URL).trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/chat/completions")) return URI.create(base);
        return URI.create(base + "/chat/completions");
    }

    private static String model() {
        return value("DEEPSEEK_MODEL", "deepseek.model", DEFAULT_MODEL).trim();
    }

    private static String apiKey() {
        return value("DEEPSEEK_API_KEY", "deepseek.api.key", "");
    }

    private static String value(String envName, String propertyName, String fallback) {
        String property = System.getProperty(propertyName);
        if (property != null && !property.isBlank()) return property;
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env;
        return fallback;
    }

    private static String json(String value) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) out.append(String.format(Locale.US, "\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        return out.toString();
    }

    private static Optional<String> extractContent(String responseJson) {
        int key = responseJson.indexOf("\"content\"");
        if (key < 0) return Optional.empty();
        int colon = responseJson.indexOf(':', key);
        if (colon < 0) return Optional.empty();
        int quote = responseJson.indexOf('"', colon + 1);
        if (quote < 0) return Optional.empty();

        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = quote + 1; i < responseJson.length(); i++) {
            char ch = responseJson.charAt(i);
            if (escaping) {
                switch (ch) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (i + 4 < responseJson.length()) {
                            String hex = responseJson.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                out.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                    }
                    default -> out.append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '"') {
                return Optional.of(out.toString());
            } else {
                out.append(ch);
            }
        }
        return Optional.empty();
    }

    private static String trimForPrompt(String value) {
        String text = value == null ? "" : value.trim();
        return text.length() <= 5000 ? text : text.substring(0, 5000);
    }

    private static String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
