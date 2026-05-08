package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/**
 * Service that uses Groq Vision API (llama-3.2-11b-vision-preview)
 * to analyze images and return an AI-generated description.
 *
 * Reuses the existing GROQ_API_KEY environment variable.
 */
public class GeminiVisionService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private final HttpClient httpClient;
    private final String apiKey;

    public GeminiVisionService() {
        this(System.getenv("GROQ_API_KEY"));
    }

    public GeminiVisionService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Analyze an image file and return an AI-generated description.
     *
     * @param imagePath path to the image file on disk
     * @return AI-generated description of the image content
     */
    public String analyzeImage(String imagePath) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY environment variable.");
        }

        Path path = Path.of(imagePath);
        if (!Files.exists(path)) {
            throw new IOException("Image file not found: " + imagePath);
        }

        // Read and encode image to base64
        byte[] imageBytes = Files.readAllBytes(path);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Detect MIME type
        String mimeType = Files.probeContentType(path);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".png")) mimeType = "image/png";
            else if (name.endsWith(".gif")) mimeType = "image/gif";
            else if (name.endsWith(".webp")) mimeType = "image/webp";
            else mimeType = "image/jpeg";
        }

        String dataUri = "data:" + mimeType + ";base64," + base64Image;

        // Build Groq Vision API request (OpenAI-compatible format)
        String body = "{" +
                "\"model\":\"" + VISION_MODEL + "\"," +
                "\"messages\":[{" +
                "\"role\":\"user\"," +
                "\"content\":[" +
                "{\"type\":\"text\",\"text\":\"" + escapeJson(
                "Decrivez le contenu de cette image de maniere detaillee et professionnelle en francais. " +
                "Mentionnez les elements principaux, le contexte, et tout texte visible dans l'image."
                ) + "\"}," +
                "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + dataUri + "\"}}" +
                "]" +
                "}]," +
                "\"temperature\":0.3," +
                "\"max_tokens\":1024" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[VisionAI] Status: " + response.statusCode());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Groq Vision API error " + response.statusCode() + ": " + response.body());
        }

        return extractMessageContent(response.body());
    }

    /**
     * Check if a file is an image based on its file type or extension.
     */
    public static boolean isImageFile(String fileType, String fileName) {
        if (fileType != null && fileType.startsWith("image/")) {
            return true;
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
        }
        return false;
    }

    /**
     * Extract the assistant message content from Groq/OpenAI chat completion response.
     */
    private static String extractMessageContent(String json) {
        if (json == null) return "";

        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx < 0) return "Aucune reponse de l'IA.";

        int contentIdx = json.indexOf("\"content\"", choicesIdx);
        if (contentIdx < 0) return "Aucune reponse de l'IA.";

        int firstQuote = json.indexOf('"', contentIdx + 9);
        if (firstQuote < 0) return "";

        // Skip the colon and whitespace to find the actual value quote
        int colonIdx = json.indexOf(':', contentIdx + 9);
        if (colonIdx < 0) return "";
        firstQuote = json.indexOf('"', colonIdx + 1);
        if (firstQuote < 0) return "";

        int secondQuote = findStringEnd(json, firstQuote + 1);
        if (secondQuote < 0) return "";

        return unescapeJson(json.substring(firstQuote + 1, secondQuote));
    }

    private static int findStringEnd(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
