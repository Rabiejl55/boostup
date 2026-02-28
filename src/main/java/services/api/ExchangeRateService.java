package services.api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ExchangeRateService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // --- API #1 (ECB/Frankfurter) : fiable pour EUR, USD, GBP, etc.
    private static final String FRANKFURTER = "https://api.frankfurter.app/latest";

    // --- API #2 (HexaRate) : beaucoup de devises (fallback), sans API key
    private static final String HEXARATE = "https://hexarate.paikama.co/api/rates";

    // ✅ Gardée pour compatibilité avec ton DashboardController
    public static double eurToUsdRate() throws Exception {
        return getRate("EUR", "USD");
    }

    // ✅ Gardée pour compatibilité
    public static double convertEurToUsd(double eur) throws Exception {
        return eur * eurToUsdRate();
    }

    // ✅ Nouveau : conversion générique
    public static double convert(double amount, String from, String to) throws Exception {
        return amount * getRate(from, to);
    }

    // ✅ Exemple demandé : TND -> QAR
    public static double tndToQarRate() throws Exception {
        return getRate("TND", "QAR");
    }

    /**
     * Retourne le taux "from -> to".
     * 1) essaie Frankfurter
     * 2) si non supporté / erreur => fallback HexaRate
     */
    public static double getRate(String from, String to) throws Exception {
        String f = norm(from);
        String t = norm(to);

        // 1) Try Frankfurter
        try {
            return getRateFromFrankfurter(f, t);
        } catch (Exception ignored) {
            // 2) fallback
            return getRateFromHexaRate(f, t);
        }
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

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Frankfurter HTTP " + resp.statusCode() + " => " + resp.body());
        }

        JSONObject json = new JSONObject(resp.body());
        JSONObject rates = json.getJSONObject("rates");

        if (!rates.has(to)) {
            throw new RuntimeException("Frankfurter ne retourne pas " + to);
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

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HexaRate HTTP " + resp.statusCode() + " => " + resp.body());
        }

        JSONObject json = new JSONObject(resp.body());
        JSONObject data = json.getJSONObject("data");

        // "mid" = taux de conversion
        return data.getDouble("mid");
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }
}