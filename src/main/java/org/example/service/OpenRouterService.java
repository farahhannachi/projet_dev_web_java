package org.example.service;

import org.example.config.AIConfig;
import org.example.model.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class OpenRouterService {

    public static final String TEMPORARY_UNAVAILABLE_MESSAGE =
            "⚠️ AI service temporarily unavailable. Please try again later.";

    private static final int MAX_USER_MESSAGE_LENGTH = 550;
    private static final int MAX_PROMPT_LENGTH = 1100;
    private static final long[] RETRY_DELAYS_MS = {2000L, 4000L, 8000L};

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String getAssistantName() {
        return AIConfig.ASSISTANT_NAME;
    }

    public String ask(String userMessage, User currentUser) throws IOException, InterruptedException {
        String normalizedMessage = normalizeUserMessage(userMessage);
        if (normalizedMessage.isBlank()) {
            return "Ask me anything about CuraVita, products, profile management, or how to use the app.";
        }

        if (!AIConfig.isConfigured()) {
            return buildFallbackAnswer(normalizedMessage, currentUser);
        }

        if (!"openrouter".equalsIgnoreCase(AIConfig.PROVIDER)) {
            return "The selected AI provider is not implemented yet. Set PROVIDER to openrouter in AIConfig.java.";
        }

        try {
            return askOpenRouter(normalizedMessage, currentUser);
        } catch (Exception exception) {
            System.err.println("[OpenRouter] Request failed after retries: " + simplifyError(exception.getMessage()));
            return TEMPORARY_UNAVAILABLE_MESSAGE;
        }
    }

    private String askOpenRouter(String userMessage, User currentUser) throws IOException, InterruptedException {
        String prompt = trimPrompt(buildSystemPrompt(currentUser) + "\nUser question: " + userMessage);
        String json = "{" +
                "\"model\":\"" + AIConfig.MODEL + "\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]}";
        String endpoint = "https://openrouter.ai/api/v1/chat/completions";
        String lastError = null;
        for (int attempt = 0; attempt <= RETRY_DELAYS_MS.length; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(40))
                    .header("Authorization", "Bearer " + AIConfig.API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logOpenRouterResponse(attempt + 1, response.statusCode(), response.body());
            if (response.statusCode() < 400) {
                String text = extractOpenRouterText(response.body());
                if (text != null && !text.isBlank()) {
                    System.out.println("Using model: " + AIConfig.MODEL);
                    return text;
                }
                lastError = "HTTP " + response.statusCode() + " - empty AI response";
                break;
            }
            String error = extractOpenRouterError(response.body());
            lastError = "HTTP " + response.statusCode()
                    + (error == null || error.isBlank() ? "." : " - " + error);
            System.err.println("[OpenRouter] Error status=" + response.statusCode() + ", body=" + response.body());
            if (response.statusCode() == 429) {
                if (attempt == RETRY_DELAYS_MS.length) {
                    break;
                }
                long delay = RETRY_DELAYS_MS[attempt];
                System.err.println("[OpenRouter] Rate limit hit. Retrying in " + delay + " ms.");
                Thread.sleep(delay);
                continue;
            }
            break;
        }
        if (lastError == null || lastError.isBlank()) {
            lastError = "unknown OpenRouter error";
        }
        return "⚠️ AI service unavailable. Please try again later.";
    }

    private String buildFallbackAnswer(String userMessage, User currentUser) {
        String message = userMessage.toLowerCase(Locale.ROOT);
        if (message.contains("buy") || message.contains("product") || message.contains("acheter") || message.contains("produit")) {
            return "To buy a product, open the products section, choose the item you want, review its details, then continue through the order flow. If you tell me which product you need, I can guide you step by step.";
        }
        if (message.contains("profile") || message.contains("profil")) {
            return "From your profile page, you can edit your personal information, change your avatar, and manage 2FA. Use the profile action buttons and cards to update your account.";
        }
        if (message.contains("2fa") || message.contains("code") || message.contains("authenticator")) {
            return "You can enable 2FA from the profile page. Generate a secret, add it to Google Authenticator or Microsoft Authenticator, then enter the 6-digit code to confirm activation.";
        }
        if (message.contains("dashboard") || message.contains("admin") || message.contains("manage")) {
            return "If you are an admin, the dashboard helps you manage users, products, and operational actions. Tell me what you want to manage, and I will point you to the right part of the app.";
        }

        String name = currentUser != null ? currentUser.getNom() : "there";
        if (AIConfig.isConfigured()) {
            return "Hi " + name + ". I can help with products, orders, profile settings, 2FA, and navigation in CuraVita.";
        }
        return "Hi " + name + ". I can help with products, orders, profile settings, and navigation in CuraVita. Set OPENROUTER_API_KEY environment variable to enable live AI replies.";
    }

    private String buildSystemPrompt(User currentUser) {
        String userContext = currentUser == null ? "The user is not signed in." :
                "Current signed-in user: " + currentUser.getNom() + ", role: " + currentUser.getType() + ".";

        return "You are " + AIConfig.ASSISTANT_NAME + ", an in-app assistant for the CuraVita JavaFX application. "
                + "Answer clearly and briefly. Help users understand how to navigate the app, buy products, manage their profile, understand 2FA, and use admin features when relevant. "
                + "Do not invent unavailable features. " + userContext;
    }

    private String normalizeUserMessage(String userMessage) {
        if (userMessage == null) {
            return "";
        }

        String collapsed = userMessage.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_USER_MESSAGE_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_USER_MESSAGE_LENGTH).trim();
    }

    private String trimPrompt(String prompt) {
        if (prompt.length() <= MAX_PROMPT_LENGTH) {
            return prompt;
        }
        return prompt.substring(0, MAX_PROMPT_LENGTH).trim();
    }

    private void logOpenRouterResponse(int attempt, int statusCode, String body) {
        System.out.println("[OpenRouter] attempt=" + attempt + ", status=" + statusCode);
        System.out.println("[OpenRouter] body=" + (body == null ? "" : body));
    }

    private String extractOpenRouterText(String body) {
        // Parse JSON response: {"choices":[{"message":{"content":"..."}}]}
        String marker = "\"content\":";
        int index = body.indexOf(marker);
        if (index < 0) {
            return null;
        }

        int start = body.indexOf('"', index + marker.length());
        if (start < 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < body.length(); i++) {
            char character = body.charAt(i);
            if (escaping) {
                switch (character) {
                    case 'n': builder.append('\n'); break;
                    case 'r': builder.append('\r'); break;
                    case 't': builder.append('\t'); break;
                    case '"': builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    default: builder.append(character);
                }
                escaping = false;
                continue;
            }
            if (character == '\\') {
                escaping = true;
                continue;
            }
            if (character == '"') {
                break;
            }
            builder.append(character);
        }
        return builder.toString().trim();
    }

    private String extractOpenRouterError(String body) {
        String marker = "\"message\":";
        int index = body.indexOf(marker);
        if (index < 0) {
            return null;
        }

        int start = body.indexOf('"', index + marker.length());
        if (start < 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < body.length(); i++) {
            char character = body.charAt(i);
            if (escaping) {
                switch (character) {
                    case 'n': builder.append(' '); break;
                    case 'r': break;
                    case 't': builder.append(' '); break;
                    case '"': builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    default: builder.append(character);
                }
                escaping = false;
                continue;
            }
            if (character == '\\') {
                escaping = true;
                continue;
            }
            if (character == '"') {
                break;
            }
            builder.append(character);
        }
        return builder.toString().trim();
    }

    private String simplifyError(String message) {
        if (message == null || message.isBlank()) {
            return "unknown error";
        }
        if (message.length() > 140) {
            return message.substring(0, 140) + "...";
        }
        return message;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
