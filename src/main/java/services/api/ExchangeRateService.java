package services.api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeRateService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // petit cache en mémoire (évite spam API)
    private static final Map<String, CachedRates> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_MS = 5 * 60 * 1000; // 5 minutes

    public static double rate(String from, String to) throws Exception {
        from = norm(from);
        to = norm(to);

        JSONObject rates = getRatesJson(from);
        if (!rates.has(to)) {
            throw new IllegalArgumentException("Devise cible non supportée: " + to);
        }
        return rates.getDouble(to);
    }

    public static double convert(double amount, String from, String to) throws Exception {
        return amount * rate(from, to);
    }

    // (compat) si tu veux garder eur->usd
    public static double eurToUsdRate() throws Exception {
        return rate("EUR", "USD");
    }
    public static double convertEurToUsd(double eur) throws Exception {
        return convert(eur, "EUR", "USD");
    }

    // ---------------- internals ----------------

    private static JSONObject getRatesJson(String base) throws Exception {
        CachedRates cached = CACHE.get(base);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timeMs) < CACHE_MS) {
            return cached.rates;
        }

        String url = "https://open.er-api.com/v6/latest/" + base; // ex: /latest/TND => rates.QAR موجودة :contentReference[oaicite:1]{index=1}
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " => " + resp.body());
        }

        JSONObject json = new JSONObject(resp.body());
        if (!"success".equalsIgnoreCase(json.optString("result"))) {
            throw new RuntimeException("API error: " + json.optString("error-type", "unknown"));
        }

        JSONObject rates = json.getJSONObject("rates");
        CACHE.put(base, new CachedRates(rates, now));
        return rates;
    }

    private static String norm(String ccy) {
        if (ccy == null) throw new IllegalArgumentException("Devise null");
        return ccy.trim().toUpperCase();
    }

    private static class CachedRates {
        final JSONObject rates;
        final long timeMs;
        CachedRates(JSONObject rates, long timeMs) { this.rates = rates; this.timeMs = timeMs; }
    }
}