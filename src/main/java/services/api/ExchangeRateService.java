package services.api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

public class ExchangeRateService {

    // ====== DEBUG ======
    // Mets à true si tu veux voir les URLs + status + body en console
    private static final boolean DEBUG = false;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // --- API #1 (ECB/Frankfurter) : devises limitées
    private static final String FRANKFURTER = "https://api.frankfurter.app/latest";

    // Frankfurter supporte une liste limitée (ECB-based)
    // Source: https://api.frankfurter.app/currencies
    private static final Set<String> FRANK_SUPPORTED = Set.of(
            "AUD","BGN","BRL","CAD","CHF","CNY","CZK","DKK","EUR","GBP",
            "HKD","HUF","IDR","ILS","INR","ISK","JPY","KRW","MXN","MYR",
            "NOK","NZD","PHP","PLN","RON","SEK","SGD","THB","TRY","USD","ZAR"
    );

    // --- API #2 (HexaRate) : beaucoup de devises (fallback), sans API key
    private static final String HEXARATE = "https://hexarate.paikama.co/api/rates";

    // ✅ Gardée pour compatibilité
    public static double eurToUsdRate() throws Exception {
        return getRate("EUR", "USD");
    }

    // ✅ Gardée pour compatibilité
    public static double convertEurToUsd(double eur) throws Exception {
        return eur * eurToUsdRate();
    }

    // ✅ Conversion générique
    public static double convert(double amount, String from, String to) throws Exception {
        return amount * getRate(from, to);
    }

    // ✅ Exemple demandé : TND -> QAR
    public static double tndToQarRate() throws Exception {
        return getRate("TND", "QAR");
    }

    /**
     * Retourne le taux "from -> to".
     * 1) Essaie Frankfurter UNIQUEMENT si devises supportées
     * 2) Sinon => HexaRate
     */
    public static double getRate(String from, String to) throws Exception {
        String f = norm(from);
        String t = norm(to);

        if (f.isEmpty() || t.isEmpty()) {
            throw new IllegalArgumentException("Devise invalide: from=" + from + ", to=" + to);
        }
        if (f.equals(t)) return 1.0;

        // 1) Frankfurter seulement si supporté
        if (FRANK_SUPPORTED.contains(f) && FRANK_SUPPORTED.contains(t)) {
            try {
                return getRateFromFrankfurter(f, t);
            } catch (Exception e) {
                if (DEBUG) {
                    System.out.println("[FX] Frankfurter failed: " + e.getMessage());
                }
                // fallback
            }
        } else {
            if (DEBUG) {
                System.out.println("[FX] Skip Frankfurter (unsupported): " + f + " -> " + t);
            }
        }

        // 2) Fallback HexaRate
        return getRateFromHexaRate(f, t);
    }

    private static double getRateFromFrankfurter(String from, String to) throws Exception {
        String url = FRANKFURTER + "?from=" + from + "&to=" + to;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "JavaFX-App/1.0")
                .GET()
                .build();

        if (DEBUG) System.out.println("[FX] Frankfurter URL: " + url);

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        if (DEBUG) {
            System.out.println("[FX] Frankfurter HTTP: " + resp.statusCode());
            System.out.println("[FX] Frankfurter BODY: " + safeBody(resp.body()));
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Frankfurter HTTP " + resp.statusCode() + " => " + safeBody(resp.body()));
        }

        String body = resp.body();
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Frankfurter: réponse vide");
        }

        JSONObject json = new JSONObject(body);

        if (!json.has("rates")) {
            throw new RuntimeException("Frankfurter: champ 'rates' absent => " + safeBody(body));
        }

        JSONObject rates = json.getJSONObject("rates");

        if (!rates.has(to)) {
            throw new RuntimeException("Frankfurter: ne retourne pas " + to + " => " + safeBody(body));
        }

        return rates.getDouble(to);
    }

    private static double getRateFromHexaRate(String from, String to) throws Exception {
        // ex: https://hexarate.paikama.co/api/rates/TND/QAR/latest
        String url = HEXARATE + "/" + from + "/" + to + "/latest";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "JavaFX-App/1.0")
                .GET()
                .build();

        if (DEBUG) System.out.println("[FX] HexaRate URL: " + url);

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        if (DEBUG) {
            System.out.println("[FX] HexaRate HTTP: " + resp.statusCode());
            System.out.println("[FX] HexaRate BODY: " + safeBody(resp.body()));
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HexaRate HTTP " + resp.statusCode() + " => " + safeBody(resp.body()));
        }

        String body = resp.body();
        if (body == null || body.isBlank()) {
            throw new RuntimeException("HexaRate: réponse vide");
        }

        JSONObject json = new JSONObject(body);

        if (!json.has("data")) {
            throw new RuntimeException("HexaRate: champ 'data' absent => " + safeBody(body));
        }

        JSONObject data = json.getJSONObject("data");

        // "mid" = taux de conversion
        if (!data.has("mid")) {
            throw new RuntimeException("HexaRate: champ 'mid' absent => " + safeBody(body));
        }

        return data.getDouble("mid");
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }

    private static String safeBody(String body) {
        if (body == null) return "(null)";
        String b = body.replace("\n", " ").replace("\r", " ").trim();
        return b.length() > 300 ? b.substring(0, 300) + "..." : b;
    }
}