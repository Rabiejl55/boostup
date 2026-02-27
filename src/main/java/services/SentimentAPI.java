package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject; // nécessite org.json dependency

public class SentimentAPI {

    private static final String API_URL = "http://text-processing.com/api/sentiment/";

    public static String getSentiment(String text) {
        try {
            String data = "text=" + URLEncoder.encode(text, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            return json.getString("label"); // "pos", "neg", "neutral"

        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}