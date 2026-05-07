package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GroqAiService {
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GroqAiService() {
        this(System.getenv("GROQ_API_KEY"),
                envOrDefault("GROQ_BASE_URL", DEFAULT_BASE_URL),
                envOrDefault("GROQ_MODEL", DEFAULT_MODEL));
    }

    public GroqAiService(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String detectPriority(String objet, String description) throws IOException, InterruptedException {
        String systemPrompt = "Vous classez des tickets support en priorite. " +
                "Repondez uniquement par: basse, normale, ou haute.";
        String userPrompt = "Objet: " + safe(objet) + "\n" +
                "Description: " + safe(description) + "\n" +
                "Donnez la priorite.";
        return chatCompletion(systemPrompt, userPrompt).trim();
    }

    public String summarizeByPriority(String groupedText) throws IOException, InterruptedException {
        String systemPrompt = "Resumez les questions par priorite. " +
                "Donnez une synthese courte par priorite.";
        return chatCompletion(systemPrompt, groupedText).trim();
    }

    public String suggestResponse(String objet, String description, String priorite) throws IOException, InterruptedException {
        String systemPrompt = "Vous etes agent support. " +
                "Proposez une reponse claire, courte et professionnelle en francais.";
        String userPrompt = "Objet: " + safe(objet) + "\n" +
                "Priorite: " + safe(priorite) + "\n" +
                "Description: " + safe(description) + "\n" +
                "Redigez la reponse.";
        return chatCompletion(systemPrompt, userPrompt).trim();
    }

    private String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY environment variable");
        }

        String body = "{" +
                "\"model\":\"" + escapeJson(model) + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}" +
                "]," +
                "\"temperature\":0.2" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofSeconds(40))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Groq API error: " + response.statusCode() + " - " + response.body());
        }
        return extractMessageContent(response.body());
    }

    private static String extractMessageContent(String json) {
        if (json == null) {
            return "";
        }
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) {
            return "";
        }
        int contentIndex = json.indexOf("\"content\"", choicesIndex);
        if (contentIndex < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', contentIndex + 9);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = findStringEnd(json, firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return unescapeJson(json.substring(firstQuote + 1, secondQuote));
    }

    private static int findStringEnd(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

