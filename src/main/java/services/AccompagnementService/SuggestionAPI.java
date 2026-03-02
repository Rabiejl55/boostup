package services.AccompagnementService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SuggestionAPI {

    private static final String API_URL = "https://api.languagetoolplus.com/v2/check";

    /**
     * Retourne les suggestions de correction pour un texte
     * Affiche la phrase corrigée complète
     */
    public static String getCorrectedText(String text) {
        try {
            String data = "text=" + URLEncoder.encode(text, "UTF-8") + "&language=fr-FR";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray matches = json.getAsJsonArray("matches");

            if (matches.size() == 0) return "Aucune suggestion.";

            StringBuilder correctedText = new StringBuilder(text);

            // On applique les corrections du dernier au premier pour ne pas décaler les offsets
            for (int i = matches.size() - 1; i >= 0; i--) {
                JsonObject match = matches.get(i).getAsJsonObject();
                int offset = match.get("offset").getAsInt();
                int length = match.get("length").getAsInt();

                JsonArray replacements = match.getAsJsonArray("replacements");
                if (replacements.size() > 0) {
                    String replacement = replacements.get(0).getAsJsonObject().get("value").getAsString();
                    correctedText.replace(offset, offset + length, replacement);
                }
            }

            return correctedText.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la génération des suggestions.";
        }
    }
}