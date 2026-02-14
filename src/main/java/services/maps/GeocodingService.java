package services.maps;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Géocodage gratuit via Nominatim (OpenStreetMap).
 * Important: Nominatim impose un User-Agent explicite.
 */
public class GeocodingService {

    public static final class GeocodeResult {
        public final double lat;
        public final double lon;
        public final String displayName;

        public GeocodeResult(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public CompletableFuture<Optional<GeocodeResult>> geocodeAsync(String address) {
        String q = address == null ? "" : address.trim();
        if (q.isEmpty()) return CompletableFuture.completedFuture(Optional.empty());

        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encoded;

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "BoostUp/1.0 (JavaFX)")
                .header("Accept", "application/json")
                .GET()
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        return Optional.<GeocodeResult>empty();
                    }
                    String body = resp.body() == null ? "" : resp.body();
                    return parseFirst(body);
                })
                .exceptionally(ex -> Optional.empty());
    }

    /**
     * Parse minimal JSON (sans dépendance): récupère lat/lon/display_name du 1er objet.
     * Suffisant pour Nominatim search.
     */
    private Optional<GeocodeResult> parseFirst(String json) {
        // Attendu: [ {"lat":"..","lon":"..","display_name":"..", ...} ]
        if (json == null) return Optional.empty();
        String s = json.trim();
        if (!s.startsWith("[") || s.length() < 5) return Optional.empty();

        String lat = extractStringField(s, "\"lat\"");
        String lon = extractStringField(s, "\"lon\"");
        String dn = extractStringField(s, "\"display_name\"");
        if (lat == null || lon == null) return Optional.empty();

        try {
            double dlat = Double.parseDouble(lat);
            double dlon = Double.parseDouble(lon);
            return Optional.of(new GeocodeResult(dlat, dlon, dn));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String extractStringField(String json, String fieldToken) {
        int idx = json.indexOf(fieldToken);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = findStringEnd(json, firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    private int findStringEnd(String s, int start) {
        boolean esc = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"') return i;
        }
        return -1;
    }
}

