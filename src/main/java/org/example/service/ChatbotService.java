package org.example.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ChatbotService {
    public record ChatbotResult(boolean success, String reply, String error) {}
    public record ProfanityResult(boolean success, boolean hasProfanity, String original, String censored, String error) {}

    private static final String PROFANITY_URL = "https://profanity-filter-by-api-ninjas.p.rapidapi.com/v1/profanityfilter";
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private final String chatbotApiUrl = env("CHATBOT_API_URL", "");
    private final String chatbotApiKey = env("CHATBOT_API_KEY", "");
    private final String chatbotModel = env("CHATBOT_MODEL", "gpt-4o-mini");
    private final String rapidApiKey = env("RAPIDAPI_KEY", "");

    public ChatbotResult ask(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new ChatbotResult(false, "", "Message vide.");
        }

        if (chatbotApiUrl.isBlank() || chatbotApiKey.isBlank()) {
            return new ChatbotResult(true, fallbackReply(userMessage), "");
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", chatbotModel);

            JsonArray messages = new JsonArray();
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", "You are CuraVita assistant. Reply in French, concise and helpful.");
            messages.add(system);

            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", userMessage);
            messages.add(user);

            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder(URI.create(chatbotApiUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + chatbotApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new ChatbotResult(false, "", "Chatbot API indisponible (" + response.statusCode() + ").");
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = extractContent(root);
            if (content == null || content.isBlank()) {
                return new ChatbotResult(false, "", "Reponse chatbot invalide.");
            }
            return new ChatbotResult(true, content, "");
        } catch (IOException | InterruptedException | RuntimeException e) {
            return new ChatbotResult(true, fallbackReply(userMessage), "");
        }
    }

    public ProfanityResult checkProfanity(String text) {
        if (text == null || text.isBlank()) {
            return new ProfanityResult(true, false, text == null ? "" : text, text == null ? "" : text, "");
        }

        if (rapidApiKey.isBlank()) {
            return new ProfanityResult(true, false, text, text, "");
        }

        try {
            String apiUrl = PROFANITY_URL + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", "profanity-filter-by-api-ninjas.p.rapidapi.com")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new ProfanityResult(false, false, text, text, "API non disponible (" + response.statusCode() + ").");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            boolean hasProfanity = json.has("has_profanity") && json.get("has_profanity").getAsBoolean();
            String original = json.has("original") ? json.get("original").getAsString() : text;
            String censored = json.has("censored") ? json.get("censored").getAsString() : text;
            return new ProfanityResult(true, hasProfanity, original, censored, "");
        } catch (IOException | InterruptedException | RuntimeException e) {
            return new ProfanityResult(false, false, text, text, "Erreur chatbot/profanity: " + e.getMessage());
        }
    }

    private String extractContent(JsonObject root) {
        if (!root.has("choices")) {
            return null;
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices.isEmpty()) {
            return null;
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        if (!first.has("message")) {
            return null;
        }
        JsonObject message = first.getAsJsonObject("message");
        return message.has("content") ? message.get("content").getAsString() : null;
    }

    private String fallbackReply(String message) {
        String m = message.toLowerCase();
        if (m.contains("coupon")) {
            return "Pour utiliser un coupon, saisissez le code dans le champ coupon puis cliquez sur Appliquer.";
        }
        if (m.contains("livraison")) {
            return "Le delai de livraison estime depend de l'adresse, du score anti-fraude et des frais de livraison.";
        }
        if (m.contains("commande")) {
            return "Vous pouvez suivre votre commande dans l'ecran Mes commandes apres validation.";
        }
        return "Je suis l'assistant CuraVita. Je peux vous aider pour commandes, coupons et livraison.";
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
