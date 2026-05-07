package org.example.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationGeocodingService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String[] LOCATION_KEYS = {"city", "town", "village", "municipality", "suburb"};
    private static final String[] CITY_KEYS = {"city", "town", "village", "municipality", "county", "state"};

    public GeocodingResult reverseGeocode(double latitude, double longitude) throws IOException, InterruptedException {
        String url = String.format(
                Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
                latitude,
                longitude
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "CuraVita/1.0 (desktop admin module)")
                .header("Accept", "application/json")
                .header("Accept-Language", "fr,en")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Reverse geocoding failed with HTTP " + response.statusCode());
        }

        return parseResponse(response.body(), latitude, longitude);
    }

    public GeocodingResult geocode(String query) throws IOException, InterruptedException {
        String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&addressdetails=1&q=" + encodedQuery;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "CuraVita/1.0 (desktop admin module)")
                .header("Accept", "application/json")
                .header("Accept-Language", "fr,en")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Geocoding failed with HTTP " + response.statusCode());
        }

        String json = response.body();
        double latitude = parseCoordinate(json, "lat");
        double longitude = parseCoordinate(json, "lon");
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            throw new IOException("No location found for query: " + query);
        }

        return parseResponse(json, latitude, longitude);
    }

    private GeocodingResult parseResponse(String json, double latitude, double longitude) {
        String displayName = extractJsonString(json, "display_name");
        String city = extractFirstAddressMatch(json, CITY_KEYS);
        String locationName = firstNonBlank(
                extractFirstAddressMatch(json, LOCATION_KEYS),
                displayName,
                formatCoordinates(latitude, longitude)
        );

        return new GeocodingResult(
                locationName,
                firstNonBlank(city, locationName),
                firstNonBlank(displayName, locationName),
                latitude,
                longitude
        );
    }

    private String extractFirstAddressMatch(String json, String[] keys) {
        for (String key : keys) {
            String value = extractJsonString(json, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return decodeJsonString(matcher.group(1));
    }

    private double parseCoordinate(String json, String key) {
        String value = extractJsonString(json, key);
        if (value.isBlank()) {
            return Double.NaN;
        }
        return Double.parseDouble(value);
    }

    private String decodeJsonString(String rawValue) {
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < rawValue.length(); index++) {
            char current = rawValue.charAt(index);
            if (current != '\\' || index + 1 >= rawValue.length()) {
                decoded.append(current);
                continue;
            }

            char escaped = rawValue.charAt(++index);
            switch (escaped) {
                case '"':
                    decoded.append('"');
                    break;
                case '\\':
                    decoded.append('\\');
                    break;
                case '/':
                    decoded.append('/');
                    break;
                case 'b':
                    decoded.append('\b');
                    break;
                case 'f':
                    decoded.append('\f');
                    break;
                case 'n':
                    decoded.append('\n');
                    break;
                case 'r':
                    decoded.append('\r');
                    break;
                case 't':
                    decoded.append('\t');
                    break;
                case 'u':
                    if (index + 4 < rawValue.length()) {
                        String hex = rawValue.substring(index + 1, index + 5);
                        decoded.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                    break;
                default:
                    decoded.append(escaped);
                    break;
            }
        }
        return decoded.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String formatCoordinates(double latitude, double longitude) {
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
    }

    public record GeocodingResult(String locationName, String city, String displayName, double latitude, double longitude) {
    }
}
