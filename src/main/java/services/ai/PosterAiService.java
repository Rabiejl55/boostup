package services.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'appel HTTP pour générer une affiche via une API externe (Leonardo/Pollo ou autre).
 *
 * Sécurité: la clé est lue uniquement depuis une variable d'environnement.
 */
public final class PosterAiService {

    public static final String ENV_API_KEY = "POLLO_API_KEY";

    private final HttpClient http;
    private final Duration timeout;
    private final URI generateEndpoint;

    public PosterAiService(Duration timeout, URI generateEndpoint) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.generateEndpoint = Objects.requireNonNull(generateEndpoint, "generateEndpoint");
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    public Optional<String> readApiKeyFromEnv() {
        String k = System.getenv(ENV_API_KEY);
        if (k == null) return Optional.empty();
        k = k.trim();
        return k.isEmpty() ? Optional.empty() : Optional.of(k);
    }

    /**
     * @return URL de l'image générée (résolue depuis la réponse JSON).
     */
    public CompletableFuture<URI> generatePosterUrlAsync(String prompt, String modelId, String dimensions, int numImages) {
        String apiKey = readApiKeyFromEnv().orElseThrow(() -> new MissingApiKeyException(
                "Clé API manquante.\n" +
                        "Définis la variable d’environnement " + ENV_API_KEY + " puis relance l’application."));

        String body = buildJson(prompt, modelId, dimensions, numImages);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(generateEndpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new ApiCallException("Erreur API (" + resp.statusCode() + ")\n" + safeTruncate(resp.body(), 1200));
                    }
                    return extractUrl(resp.body()).orElseThrow(() ->
                            new ApiCallException("Réponse API invalide: URL d’image introuvable."));
                });
    }

    public CompletableFuture<byte[]> downloadBytesAsync(URI imageUrl) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(imageUrl)
                .timeout(timeout)
                .GET()
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new ApiCallException("Téléchargement image échoué (" + resp.statusCode() + ").");
                    }
                    return resp.body();
                });
    }

    private static String buildJson(String prompt, String modelId, String dimensions, int numImages) {
        return "{" +
                "\"prompt\":\"" + jsonEscape(prompt) + "\"," +
                "\"model_id\":\"" + jsonEscape(modelId) + "\"," +
                "\"dimensions\":\"" + jsonEscape(dimensions) + "\"," +
                "\"num_images\":" + numImages +
                "}";
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parsing minimaliste sans lib JSON: cherche un champ "url".
     * Adapte cette regex si ton API renvoie un champ différent.
     */
    private static Optional<URI> extractUrl(String json) {
        Pattern p = Pattern.compile("\"url\"\\s*:\\s*\"(https?:\\\\/\\\\/[^\"]+)\"");
        Matcher m = p.matcher(json == null ? "" : json);
        if (!m.find()) return Optional.empty();
        String raw = m.group(1).replace("\\/", "/");
        return Optional.of(URI.create(raw));
    }

    private static String safeTruncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    public static final class MissingApiKeyException extends RuntimeException {
        public MissingApiKeyException(String message) {
            super(message);
        }
    }

    public static final class ApiCallException extends RuntimeException {
        public ApiCallException(String message) {
            super(message);
        }
    }
}
