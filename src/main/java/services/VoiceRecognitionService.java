package services;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🎤 SERVICE DE RECONNAISSANCE VOCALE
 * Utilise Google Web Speech API via JavaFX WebView
 *
 * Fonctionnalités :
 * ✅ Reconnaissance vocale en français
 * ✅ Extraction intelligente : titre, type, date, lieu
 * ✅ Remplissage automatique du formulaire
 * ✅ Interface visuelle moderne (micro animé)
 */
public class VoiceRecognitionService {

    private WebView webView;
    private WebEngine webEngine;
    private Consumer<Map<String, String>> onResultCallback;
    private Consumer<String> onErrorCallback;
    private boolean isListening = false;
    private boolean isReady = false;

    // ════════════════════════════════════════════════════════
    // 🎤 INITIALISATION
    // ════════════════════════════════════════════════════════

    public VoiceRecognitionService() {
        initWebView();
    }

    /**
     * Initialise le WebView avec le HTML de reconnaissance vocale
     */
    private void initWebView() {
        Platform.runLater(() -> {
            webView = new WebView();
            webEngine = webView.getEngine();

            // HTML + JavaScript pour Web Speech API
            String html = generateSpeechRecognitionHTML();
            webEngine.loadContent(html);

            // Attendre que la page soit chargée
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    // Exposer l'objet Java au JavaScript
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaApp", new JavaScriptBridge());
                    isReady = true;
                    System.out.println("✅ Reconnaissance vocale initialisée");
                }
            });
        });
    }

    /**
     * Génère le HTML avec Web Speech API
     */
    private String generateSpeechRecognitionHTML() {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            margin: 0;
            padding: 20px;
            font-family: 'Segoe UI', sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
        }
        #status {
            font-size: 18px;
            margin-bottom: 20px;
            padding: 15px 30px;
            background: rgba(255,255,255,0.1);
            border-radius: 10px;
            backdrop-filter: blur(10px);
        }
        #result {
            font-size: 16px;
            margin-top: 20px;
            padding: 20px;
            background: rgba(255,255,255,0.15);
            border-radius: 10px;
            min-width: 300px;
            max-width: 500px;
            word-wrap: break-word;
        }
        .pulse {
            animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body>
    <div id="status">🎤 Prêt à écouter...</div>
    <div id="result"></div>

    <script>
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        
        if (!SpeechRecognition) {
            document.getElementById('status').textContent = '❌ Reconnaissance vocale non supportée';
            javaApp.onError('Navigateur non compatible');
        } else {
            const recognition = new SpeechRecognition();
            recognition.lang = 'fr-FR';
            recognition.continuous = false;
            recognition.interimResults = false;
            recognition.maxAlternatives = 1;

            let finalTranscript = '';

            recognition.onstart = function() {
                document.getElementById('status').innerHTML = '🎤 <span class="pulse">Écoute en cours...</span>';
                document.getElementById('result').textContent = '';
            };

            recognition.onresult = function(event) {
                finalTranscript = event.results[0][0].transcript;
                console.log('Transcription: ' + finalTranscript);
                document.getElementById('result').innerHTML = '<strong>Vous avez dit :</strong><br>"' + finalTranscript + '"';
                
                // Envoyer le résultat à Java
                javaApp.onResult(finalTranscript);
            };

            recognition.onerror = function(event) {
                console.error('Erreur:', event.error);
                document.getElementById('status').textContent = '❌ Erreur: ' + event.error;
                javaApp.onError(event.error);
            };

            recognition.onend = function() {
                document.getElementById('status').textContent = '✅ Écoute terminée';
            };

            // Fonction pour démarrer l'écoute (appelée depuis Java)
            window.startListening = function() {
                try {
                    recognition.start();
                    return true;
                } catch(e) {
                    console.error('Erreur start:', e);
                    return false;
                }
            };

            // Fonction pour arrêter l'écoute
            window.stopListening = function() {
                try {
                    recognition.stop();
                    return true;
                } catch(e) {
                    console.error('Erreur stop:', e);
                    return false;
                }
            };
        }
    </script>
</body>
</html>
                """;
    }

    // ════════════════════════════════════════════════════════
    // 🗣️ CONTRÔLE DE LA RECONNAISSANCE
    // ════════════════════════════════════════════════════════

    /**
     * Démarre l'écoute vocale
     */
    public void startListening(Consumer<Map<String, String>> onResult, Consumer<String> onError) {
        this.onResultCallback = onResult;
        this.onErrorCallback = onError;

        Platform.runLater(() -> {
            try {
                // Vérifier que la fonction existe avant de l'appeler
                Object windowObj = webEngine.executeScript("typeof startListening");
                if (windowObj == null || !"function".equals(windowObj.toString())) {
                    System.err.println("⚠️ Fonction startListening pas encore chargée, attente...");
                    // Attendre 500ms et réessayer
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Platform.runLater(() -> startListening(onResult, onError));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                    return;
                }

                Boolean started = (Boolean) webEngine.executeScript("startListening()");
                if (started != null && started) {
                    isListening = true;
                    System.out.println("🎤 Écoute démarrée...");
                } else {
                    System.err.println("⚠️ startListening a retourné false");
                    if (onError != null) {
                        onError.accept("Impossible de démarrer l'écoute");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur démarrage écoute : " + e.getMessage());
                e.printStackTrace();
                if (onError != null) {
                    onError.accept("Erreur démarrage : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Arrête l'écoute vocale
     */
    public void stopListening() {
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("stopListening()");
                isListening = false;
                System.out.println("🛑 Écoute arrêtée");
            } catch (Exception e) {
                System.err.println("❌ Erreur arrêt écoute : " + e.getMessage());
            }
        });
    }

    public boolean isListening() {
        return isListening;
    }

    public WebView getWebView() {
        return webView;
    }

    // ════════════════════════════════════════════════════════
    // 🧠 EXTRACTION INTELLIGENTE DES DONNÉES
    // ════════════════════════════════════════════════════════

    /**
     * Extrait les informations de l'événement depuis le texte vocal
     */
    private Map<String, String> extractEventData(String text) {
        Map<String, String> data = new HashMap<>();

        System.out.println("\n🧠 ANALYSE DU TEXTE VOCAL");
        System.out.println("═══════════════════════════════════════");
        System.out.println("📝 Texte : " + text);

        String textLower = text.toLowerCase();

        // Extraction du TYPE d'événement
        data.put("type", extractType(textLower));

        // Extraction du TITRE (on prend le texte avant "le" ou "à" ou tout)
        data.put("titre", extractTitre(text, textLower));

        // Extraction de la DATE
        data.put("date", extractDate(textLower));

        // Extraction du LIEU
        data.put("lieu", extractLieu(textLower));

        // Capacité par défaut
        data.put("capacite", "100");

        // Description = texte complet
        data.put("description", text);

        System.out.println("\n✅ DONNÉES EXTRAITES :");
        data.forEach((k, v) -> System.out.println("   " + k + " : " + v));
        System.out.println("═══════════════════════════════════════\n");

        return data;
    }

    private String extractType(String text) {
        String[] types = {"conférence", "conference", "atelier", "workshop", "séminaire",
                         "seminaire", "formation", "meetup", "hackathon", "webinaire", "webinar"};

        for (String type : types) {
            if (text.contains(type)) {
                return capitalizeFirst(type);
            }
        }
        return "Événement";
    }

    private String extractTitre(String originalText, String textLower) {
        // Cherche "ajouter un événement XXX" ou "créer un événement XXX"
        Pattern pattern = Pattern.compile("(?:ajouter|créer|cr[ée]er)\\s+(?:un\\s+)?(?:[ée]v[ée]nement\\s+)?(.*?)(?:\\s+le\\s+|\\s+à\\s+|\\s+au\\s+|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(originalText);

        if (matcher.find()) {
            String titre = matcher.group(1).trim();
            if (!titre.isEmpty()) {
                return capitalizeFirst(titre);
            }
        }

        // Sinon prend les premiers mots (jusqu'à 5 mots max)
        String[] words = originalText.split("\\s+");
        StringBuilder titre = new StringBuilder();
        int count = 0;
        for (String word : words) {
            if (count >= 5) break;
            if (!word.matches("(?i)(ajouter|créer|un|événement|le|la|les|de|du|à|au)")) {
                titre.append(word).append(" ");
                count++;
            }
        }

        String result = titre.toString().trim();
        return result.isEmpty() ? "Nouvel événement" : capitalizeFirst(result);
    }

    private String extractDate(String text) {
        // Cherche des patterns de date
        String[] mois = {"janvier", "février", "fevrier", "mars", "avril", "mai", "juin",
                        "juillet", "août", "aout", "septembre", "octobre", "novembre", "décembre", "decembre"};

        for (int i = 0; i < mois.length; i++) {
            if (text.contains(mois[i])) {
                // Cherche un nombre avant le mois
                Pattern pattern = Pattern.compile("(\\d{1,2})\\s+" + mois[i], Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    int jour = Integer.parseInt(matcher.group(1));
                    int moisNum = i + 1;
                    return String.format("%02d/%02d/2026", jour, moisNum);
                }
            }
        }

        // Cherche "le XX" ou "demain" ou "aujourd'hui"
        if (text.contains("demain")) {
            return "Demain";
        }
        if (text.contains("aujourd'hui") || text.contains("aujourdhui")) {
            return "Aujourd'hui";
        }

        Pattern datePattern = Pattern.compile("le\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            return "Le " + dateMatcher.group(1);
        }

        return "À définir";
    }

    private String extractLieu(String text) {
        // Cherche "à XXX" ou "au XXX"
        Pattern pattern = Pattern.compile("(?:à|au|dans|chez)\\s+([\\w\\s-]+?)(?:\\s+le\\s+|\\s+capacit[ée]|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String lieu = matcher.group(1).trim();
            return capitalizeFirst(lieu);
        }

        // Liste de villes connues
        String[] villes = {"paris", "lyon", "marseille", "toulouse", "nice", "nantes",
                          "strasbourg", "montpellier", "bordeaux", "lille", "tunis"};

        for (String ville : villes) {
            if (text.contains(ville)) {
                return capitalizeFirst(ville);
            }
        }

        return "En ligne";
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    // ════════════════════════════════════════════════════════
    // 🌉 PONT JAVA ↔ JAVASCRIPT
    // ════════════════════════════════════════════════════════

    /**
     * Classe exposée au JavaScript pour recevoir les callbacks
     */
    public class JavaScriptBridge {
        public void onResult(String transcript) {
            System.out.println("📢 Transcription reçue : " + transcript);

            // Extraction des données
            Map<String, String> eventData = extractEventData(transcript);

            // Callback vers le controller
            if (onResultCallback != null) {
                Platform.runLater(() -> onResultCallback.accept(eventData));
            }

            isListening = false;
        }

        public void onError(String error) {
            System.err.println("❌ Erreur reconnaissance : " + error);

            if (onErrorCallback != null) {
                Platform.runLater(() -> onErrorCallback.accept(error));
            }

            isListening = false;
        }
    }
}




