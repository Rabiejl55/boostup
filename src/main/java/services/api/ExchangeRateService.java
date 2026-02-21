package services.api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExchangeRateService {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static double eurToUsdRate() throws Exception {
        String url = "https://api.frankfurter.app/latest?from=EUR&to=USD";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " => " + resp.body());
        }

        JSONObject json = new JSONObject(resp.body());
        JSONObject rates = json.getJSONObject("rates");

        return rates.getDouble("USD");
    }

    public static double convertEurToUsd(double eur) throws Exception {
        return eur * eurToUsdRate();
    }
}