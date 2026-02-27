package services.AccompagnementService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProfanityDetectorAPI {

    private static final String API_KEY = "50d888460dcbb2eb6de478bb976928c0";
    private static final String API_URL = "https://api1.webpurify.com/services/rest/";

    public static boolean hasBadWords(String feedback) {
        try {
            // Encoder le texte
            String url = API_URL + "?method=webpurify.live.check"
                    + "&api_key=" + API_KEY
                    + "&text=" + URLEncoder.encode(feedback, "UTF-8")
                    + "&format=json";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Réponse API : " + response.body());

            // "found":"1" si un mot interdit est détecté
            return response.body().contains("\"found\":\"1\"");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}