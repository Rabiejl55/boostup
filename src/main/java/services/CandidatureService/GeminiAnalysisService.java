package services.CandidatureService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service d'analyse IA via l'API Groq (gratuit, sans restrictions géographiques).
 *
 * ──────────────────────────────────────────────────────────────────
 *  SETUP GRATUIT :
 *  1. Créer un compte sur https://console.groq.com
 *  2. "API Keys" → "Create API Key" → copier la clé
 *  3. Coller la clé dans config.properties :
 *     gemini.api.key=gsk_VOTRE_CLE_GROQ
 * ──────────────────────────────────────────────────────────────────
 *
 *  QUOTA GRATUIT Groq :
 *  - 14 400 requêtes / jour
 *  - 30 requêtes / minute
 *  - Modèle : llama-3.3-70b-versatile (très performant)
 * ──────────────────────────────────────────────────────────────────
 */
public class GeminiAnalysisService {

    // ─── CONFIGURATION ────────────────────────────────────────────
    private static final String API_KEY = loadApiKey();
    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private static String loadApiKey() {
        try (InputStream input = GeminiAnalysisService.class
                .getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("gemini.api.key");
        } catch (Exception e) {
            return null;
        }
    }

    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiAnalysisService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    // ─── MODÈLE DE RÉSULTAT (inchangé — compatible avec AnalyseIAPanelController) ──

    public static class AnalysisResult {
        public String resume;
        public String pointsForts;
        public String pointsFaibles;
        public String recommandations;
        public String scoreEstime;
        public String decision;
        public String innovationNote;
        public String viabiliteNote;
        public String marcheNote;
        public String equipeNote;
        public boolean success;
        public String errorMessage;
    }

    // ─── ANALYSE COMPLÈTE ─────────────────────────────────────────

    public AnalysisResult analyserBusinessPlan(String nomStartup,
                                               String descriptionProjet,
                                               String businessPlanTexte) {
        AnalysisResult result = new AnalysisResult();
        try {
            String prompt = buildPrompt(nomStartup, descriptionProjet, businessPlanTexte);
            String responseText = callGroqAPI(prompt);
            parseResponse(responseText, result);
            result.success = true;
        } catch (IOException | InterruptedException e) {
            result.success = false;
            result.errorMessage = "Erreur réseau : " + e.getMessage();
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "Erreur d'analyse : " + e.getMessage();
        }
        return result;
    }

    // ─── ANALYSE RAPIDE ───────────────────────────────────────────

    public AnalysisResult analyseRapide(String descriptionProjet) {
        AnalysisResult result = new AnalysisResult();
        try {
            String prompt = """
                Analyse cette description de projet de startup en 3 points très concis.
                
                Description : %s
                
                Réponds EXACTEMENT dans ce format (sans markdown, sans **) :
                RESUME: [1 phrase résumant le projet]
                DECISION: [PROMETTEUSE ou MOYENNE ou INSUFFISANTE]
                SCORE: [chiffre de 1 à 10]
                RAISON: [1 phrase justifiant le score]
                """.formatted(descriptionProjet != null ? descriptionProjet : "Non fournie");

            String responseText = callGroqAPI(prompt);

            for (String line : responseText.split("\n")) {
                line = line.trim();
                if (line.startsWith("RESUME:"))   result.resume          = line.replace("RESUME:", "").trim();
                if (line.startsWith("DECISION:")) result.decision        = line.replace("DECISION:", "").trim();
                if (line.startsWith("SCORE:"))    result.scoreEstime     = line.replace("SCORE:", "").trim();
                if (line.startsWith("RAISON:"))   result.recommandations = line.replace("RAISON:", "").trim();
            }
            result.success = true;

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
        }
        return result;
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────

    private String buildPrompt(String nomStartup, String description, String businessPlan) {
        return """
            Tu es un expert en évaluation de startups pour un programme d'incubation.
            Analyse le dossier de candidature suivant et fournis une évaluation structurée.
            
            ═══ DOSSIER DE CANDIDATURE ═══
            Startup : %s
            Description du projet : %s
            Business Plan / Informations complémentaires : %s
            
            ═══ INSTRUCTIONS ═══
            Réponds STRICTEMENT dans ce format (sans markdown, sans **, sans #) :
            
            RESUME: [Résumé du projet en 2 phrases maximum]
            POINTS_FORTS: [3 points forts principaux séparés par |]
            POINTS_FAIBLES: [3 risques ou faiblesses séparés par |]
            RECOMMANDATIONS: [2 recommandations concrètes séparées par |]
            SCORE: [note globale estimée de 0 à 10]
            DECISION: [PROMETTEUSE ou MOYENNE ou INSUFFISANTE]
            NOTE_INNOVATION: [note de 0 à 10]
            NOTE_VIABILITE: [note de 0 à 10]
            NOTE_MARCHE: [note de 0 à 10]
            NOTE_EQUIPE: [note de 0 à 10 ou N/A si non mentionné]
            """.formatted(
                nomStartup   != null ? nomStartup   : "Non précisé",
                description  != null ? description  : "Non fournie",
                businessPlan != null ? businessPlan : "Non fourni"
        );
    }

    private String callGroqAPI(String prompt) throws IOException, InterruptedException {
        // Format OpenAI-compatible (utilisé par Groq)
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.3);
        requestBody.addProperty("max_tokens", 1024);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Groq API error " + response.statusCode()
                    + " : " + response.body());
        }

        // Extraction : choices[0].message.content
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson
                .getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    private void parseResponse(String text, AnalysisResult result) {
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.startsWith("RESUME:"))               result.resume          = clean(line, "RESUME:");
            else if (line.startsWith("POINTS_FORTS:"))    result.pointsForts     = clean(line, "POINTS_FORTS:");
            else if (line.startsWith("POINTS_FAIBLES:"))  result.pointsFaibles   = clean(line, "POINTS_FAIBLES:");
            else if (line.startsWith("RECOMMANDATIONS:")) result.recommandations = clean(line, "RECOMMANDATIONS:");
            else if (line.startsWith("SCORE:"))           result.scoreEstime     = clean(line, "SCORE:");
            else if (line.startsWith("DECISION:"))        result.decision        = clean(line, "DECISION:");
            else if (line.startsWith("NOTE_INNOVATION:")) result.innovationNote  = clean(line, "NOTE_INNOVATION:");
            else if (line.startsWith("NOTE_VIABILITE:"))  result.viabiliteNote   = clean(line, "NOTE_VIABILITE:");
            else if (line.startsWith("NOTE_MARCHE:"))     result.marcheNote      = clean(line, "NOTE_MARCHE:");
            else if (line.startsWith("NOTE_EQUIPE:"))     result.equipeNote      = clean(line, "NOTE_EQUIPE:");
        }

        // Valeurs par défaut si réponse incomplète
        if (result.resume == null)      result.resume      = "Analyse non disponible";
        if (result.decision == null)    result.decision    = "MOYENNE";
        if (result.scoreEstime == null) result.scoreEstime = "5";
    }

    private String clean(String line, String prefix) {
        return line.replace(prefix, "").trim();
    }
}