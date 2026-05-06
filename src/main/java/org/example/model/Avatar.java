package org.example.model;

import java.util.Random;

/**
 * Model class for generating and storing avatar configuration
 * Uses DiceBear API for real generated cartoon characters
 */
public class Avatar {
    private String style; // "avataaars", "avataaars-neutral", "pixel-art", "adventurer", "fun-emoji", "lorelei", "bottts"
    private String seed; // Unique seed for generating the avatar
    private String avatarUrl; // DiceBear API URL
    
    private static final String[] STYLES = {
        "avataaars",
        "avataaars-neutral", 
        "pixel-art",
        "adventurer",
        "fun-emoji",
        "lorelei",
        "bottts"
    };
    
    private static final String STYLE_DISPLAY_NAMES[] = {
        "Cartoon",
        "Neutral",
        "Pixel",
        "Adventure",
        "Emoji",
        "Lorelei",
        "Robot"
    };

    public Avatar() {
        this.style = "avataaars";
        this.seed = generateSeed();
        this.avatarUrl = generateUrl();
    }

    public Avatar(String style, String seed) {
        this.style = style;
        this.seed = seed;
        this.avatarUrl = generateUrl();
    }

    // Generate random seed
    private static String generateSeed() {
        Random rand = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    // Generate random avatar
    public static Avatar generateRandom() {
        Random rand = new Random();
        String style = STYLES[rand.nextInt(STYLES.length)];
        return new Avatar(style, generateSeed());
    }
    
    // Generate random avatar with specific style
    public static Avatar generateRandom(String style) {
        // Map display names to style names
        String mappedStyle = mapStyleName(style);
        return new Avatar(mappedStyle, generateSeed());
    }
    
    private static String mapStyleName(String name) {
        String lower = name.toLowerCase();
        switch(lower) {
            case "cartoon": return "avataaars";
            case "neutral": return "avataaars-neutral";
            case "pixel": return "pixel-art";
            case "adventure": return "adventurer";
            case "emoji": return "fun-emoji";
            case "robot": return "bottts";
            case "lorelei": return "lorelei";
            default: return "avataaars";
        }
    }

    // Generate DiceBear URL
    private String generateUrl() {
        return String.format("https://api.dicebear.com/7.x/%s/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", style, seed);
    }
    
    // Generate URL with specific options per style
    private String generateUrlWithOptions() {
        switch(style) {
            case "avataaars":
                return String.format("https://api.dicebear.com/7.x/avataaars/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "avataaars-neutral":
                return String.format("https://api.dicebear.com/7.x/avataaars-neutral/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "pixel-art":
                return String.format("https://api.dicebear.com/7.x/pixel-art/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "adventurer":
                return String.format("https://api.dicebear.com/7.x/adventurer/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "fun-emoji":
                return String.format("https://api.dicebear.com/7.x/fun-emoji/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "lorelei":
                return String.format("https://api.dicebear.com/7.x/lorelei/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            case "bottts":
                return String.format("https://api.dicebear.com/7.x/bottts/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
            default:
                return String.format("https://api.dicebear.com/7.x/avataaars/png?seed=%s&backgroundColor=b6e3f4,c0aede,d1d4f9", seed);
        }
    }

    // Convert to JSON string for storage
    public String toJson() {
        return String.format("{\"style\":\"%s\",\"seed\":\"%s\"}", style, seed);
    }
    
    // Parse from JSON string
    public static Avatar fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return generateRandom();
        }
        
        try {
            String style = extractJsonValue(json, "style");
            String seed = extractJsonValue(json, "seed");
            
            if (style == null || style.isEmpty()) {
                style = "avataaars";
            }
            if (seed == null || seed.isEmpty()) {
                seed = generateSeed();
            }
            
            Avatar avatar = new Avatar(style, seed);
            return avatar;
        } catch (Exception e) {
            return generateRandom();
        }
    }
    
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            searchKey = "\"" + key + "\":";
            start = json.indexOf(searchKey);
            if (start == -1) return "";
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return json.substring(start).split("[,\\}]")[0].trim();
            return json.substring(start, end);
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    // Getters
    public String getStyle() { return style; }
    public String getSeed() { return seed; }
    public String getAvatarUrl() { 
        // Regenerate URL to ensure it's current
        return generateUrlWithOptions(); 
    }
    public static String[] getStyles() { return STYLES; }
    public static String[] getStyleDisplayNames() { return STYLE_DISPLAY_NAMES; }

    // Setters
    public void setStyle(String style) { 
        this.style = style; 
        this.avatarUrl = generateUrlWithOptions();
    }
    public void setSeed(String seed) { 
        this.seed = seed; 
        this.avatarUrl = generateUrlWithOptions();
    }
    
    // Generate new random avatar with same style
    public void regenerate() {
        this.seed = generateSeed();
        this.avatarUrl = generateUrlWithOptions();
    }
    
    // Generate new random seed
    public void randomize() {
        this.seed = generateSeed();
        this.avatarUrl = generateUrlWithOptions();
    }
}
