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
import java.util.ArrayList;
import java.util.List;

public class MapsApiService {
    public record GeocodeResult(boolean found, double latitude, double longitude, String displayName, String error) {}
    public record LocationResult(boolean found, double latitude, double longitude, String cityLabel, String error) {}
    public record ReverseAddressResult(boolean found, String displayName, String line1, String city, String region, String postalCode, String country, String error) {}

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String IP_GEO_URL = "http://ip-api.com/json/?fields=status,message,lat,lon,city,country,query";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public GeocodeResult geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            return new GeocodeResult(false, 0, 0, "", "Adresse vide.");
        }

        try {
            String query = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?format=json&limit=1&q=" + query;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "CuraVita-JavaFX/1.0 (contact: support@curavita.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new GeocodeResult(false, 0, 0, "", "Maps API indisponible (" + response.statusCode() + ").");
            }

            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            if (array.isEmpty()) {
                return new GeocodeResult(false, 0, 0, "", "Adresse introuvable.");
            }

            JsonObject first = array.get(0).getAsJsonObject();
            double lat = first.get("lat").getAsDouble();
            double lon = first.get("lon").getAsDouble();
            String displayName = first.has("display_name") ? first.get("display_name").getAsString() : address;

            return new GeocodeResult(true, lat, lon, displayName, "");
        } catch (IOException | InterruptedException | RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("timeout")) {
                return new GeocodeResult(false, 0, 0, "", "Délai de connexion dépassé. Vérifiez votre connexion Internet.");
            }
            return new GeocodeResult(false, 0, 0, "", "Erreur maps: " + msg);
        }
    }

    public List<GeocodeResult> searchAddresses(String address, int limit) {
        List<GeocodeResult> results = new ArrayList<>();
        if (address == null || address.isBlank()) {
            return results;
        }

        int safeLimit = Math.max(1, Math.min(limit, 10));

        try {
            String query = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?format=json&limit=" + safeLimit + "&q=" + query;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "CuraVita-JavaFX/1.0 (contact: support@curavita.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return results;
            }

            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject item = array.get(i).getAsJsonObject();
                double lat = item.get("lat").getAsDouble();
                double lon = item.get("lon").getAsDouble();
                String displayName = item.has("display_name") ? item.get("display_name").getAsString() : address;
                results.add(new GeocodeResult(true, lat, lon, displayName, ""));
            }
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            return results;
        }

        return results;
    }

    public String buildStaticMapUrl(double lat, double lon, int zoom, int width, int height) {
        int z = Math.max(3, Math.min(18, zoom));
        int w = Math.max(200, Math.min(1200, width));
        int h = Math.max(120, Math.min(1200, height));
        return "https://staticmap.openstreetmap.de/staticmap.php?center="
                + lat + "," + lon
                + "&zoom=" + z
                + "&size=" + w + "x" + h
                + "&markers=" + lat + "," + lon + ",red-pushpin";
    }

    public String buildStaticMapFallbackUrl(double lat, double lon, int zoom, int width, int height) {
        int z = Math.max(3, Math.min(18, zoom));
        int w = Math.max(200, Math.min(650, width));
        int h = Math.max(120, Math.min(450, height));
        return "https://static-maps.yandex.ru/1.x/?lang=fr_FR&ll="
                + lon + "," + lat
                + "&z=" + z
                + "&l=map&size=" + w + "," + h
                + "&pt=" + lon + "," + lat + ",pm2rdm";
    }

    public String buildOpenStreetMapViewUrl(double lat, double lon, int zoom) {
        int z = Math.max(3, Math.min(18, zoom));
        return "https://www.openstreetmap.org/#map=" + z + "/" + lat + "/" + lon;
    }

    public LocationResult approximateCurrentLocation() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(IP_GEO_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "CuraVita-JavaFX/1.0 (contact: support@curavita.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new LocationResult(false, 0, 0, "", "API localisation indisponible (" + response.statusCode() + ")");
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = root.has("status") ? root.get("status").getAsString() : "";
            if (!"success".equalsIgnoreCase(status)) {
                String message = root.has("message") ? root.get("message").getAsString() : "Erreur de localisation";
                return new LocationResult(false, 0, 0, "", message);
            }

            double lat = root.has("lat") ? root.get("lat").getAsDouble() : 0;
            double lon = root.has("lon") ? root.get("lon").getAsDouble() : 0;
            String city = root.has("city") ? root.get("city").getAsString() : "Position detectee";
            String country = root.has("country") ? root.get("country").getAsString() : "";
            String cityLabel = country.isBlank() ? city : city + ", " + country;

            if (lat == 0 && lon == 0) {
                return new LocationResult(false, 0, 0, "", "Coordonnees introuvables");
            }
            return new LocationResult(true, lat, lon, cityLabel, "");
        } catch (IOException | InterruptedException | RuntimeException e) {
            return new LocationResult(false, 0, 0, "", "Erreur localisation: " + e.getMessage());
        }
    }

    public String reverseGeocode(double lat, double lon) {
        try {
            String url = NOMINATIM_REVERSE_URL + "?format=jsonv2&lat=" + lat + "&lon=" + lon;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "CuraVita-JavaFX/1.0 (contact: support@curavita.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "";
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            return root.has("display_name") ? root.get("display_name").getAsString() : "";
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            return "";
        }
    }

    public ReverseAddressResult reverseGeocodeDetailed(double lat, double lon) {
        try {
            String url = NOMINATIM_REVERSE_URL + "?format=jsonv2&addressdetails=1&lat=" + lat + "&lon=" + lon;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "CuraVita-JavaFX/1.0 (contact: support@curavita.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new ReverseAddressResult(false, "", "", "", "", "", "", "API reverse indisponible");
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String displayName = root.has("display_name") ? root.get("display_name").getAsString() : "";

            JsonObject address = root.has("address") ? root.getAsJsonObject("address") : new JsonObject();
            String road = pick(address, "road", "pedestrian", "residential");
            String houseNumber = pick(address, "house_number");
            String city = pick(address, "city", "town", "village", "municipality");
            String region = pick(address, "state", "county");
            String postal = pick(address, "postcode");
            String country = pick(address, "country");

            String line1 = (road + " " + houseNumber).trim();
            if (line1.isBlank()) {
                line1 = displayName;
            }

            return new ReverseAddressResult(true, displayName, line1, city, region, postal, country, "");
        } catch (IOException | InterruptedException | RuntimeException e) {
            return new ReverseAddressResult(false, "", "", "", "", "", "", "Erreur reverse geocode: " + e.getMessage());
        }
    }

    private String pick(JsonObject object, String... keys) {
        if (object == null) {
            return "";
        }
        for (String key : keys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                String value = object.get(key).getAsString();
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return "";
    }
}
