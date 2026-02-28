package services.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenRouterDashboardService {

    private static final String API_KEY = System.getenv("OPENROUTER_API_KEY");
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    public static String generateSummary(String countProjets,
                                         String countInv,
                                         String countTx,
                                         String lastTxText,
                                         String alertsText) throws Exception {

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY manquante.");
        }

        String prompt =
                "Tu es un assistant financier.\n" +
                        "Génère un résumé clair et structuré.\n\n" +
                        "Projets: " + countProjets + "\n" +
                        "Investissements: " + countInv + "\n" +
                        "Transactions: " + countTx + "\n\n" +
                        "Transactions récentes:\n" + lastTxText + "\n" +
                        "Alertes:\n" + alertsText + "\n\n" +
                        "Donne:\n" +
                        "- Résumé court\n" +
                        "- 3 recommandations\n" +
                        "- 2 risques";

        JSONObject body = new JSONObject();
        body.put("model", "openai/gpt-4o-mini"); // gratuit
        body.put("messages", new JSONArray()
                .put(new JSONObject()
                        .put("role", "user")
                        .put("content", prompt)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " => " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        JSONArray choices = json.getJSONArray("choices");
        return choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}