package services.api;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HuggingFaceService {

    private static final String API_KEY = System.getenv("HF_API_KEY");
    private static final String ENDPOINT =
            "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";

    public static String summarize(String text) throws Exception {

        if (API_KEY == null) {
            throw new IllegalStateException("HF_API_KEY manquante");
        }

        JSONObject body = new JSONObject();
        body.put("inputs", text);

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
            throw new RuntimeException("Erreur HF: " + response.body());
        }

        // La réponse est un tableau JSON
        return response.body();
    }
}