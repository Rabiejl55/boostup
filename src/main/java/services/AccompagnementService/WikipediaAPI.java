package services.AccompagnementService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikipediaAPI {
    public static String getSummary(String domainName) {
        try {
            String encodedName = URLEncoder.encode(domainName, StandardCharsets.UTF_8);
            String url = "https://fr.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&titles="
                    + encodedName + "&format=json";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode pages = root.path("query").path("pages");

            if (!pages.elements().hasNext()) return "Description non disponible.";

            JsonNode page = pages.elements().next();
            String extract = page.path("extract").asText();

            return extract.isEmpty() ? "Description non disponible." : extract;

        } catch (Exception e) {
            e.printStackTrace();
            return "Description non disponible.";
        }
    }
}