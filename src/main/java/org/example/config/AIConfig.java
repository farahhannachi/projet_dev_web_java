package org.example.config;

public final class AIConfig {

    private AIConfig() {}

    public static final String ASSISTANT_NAME = "Cura Assistant";
    public static final String PROVIDER = "openrouter";
    public static final String MODEL = "openrouter/auto";
    public static final String API_KEY = System.getenv("OPENROUTER_API_KEY");

    public static boolean isConfigured() {
        return API_KEY != null && !API_KEY.isBlank();
    }
}
