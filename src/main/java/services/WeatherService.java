package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 🌤️ SERVICE MÉTÉO - OpenWeatherMap API
 * Récupère la météo prévue pour un événement
 */
public class WeatherService {

    // 🔑 Clé API OpenWeatherMap
    private static final String API_KEY = "2a0fca180295852e6e7150687ab016d3";

    // 🌍 URL de l'API
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    /**
     * 🌤️ Récupère les données météo pour une ville
     * @param lieu Nom de la ville (ex: "Tunis", "Paris")
     * @return Objet WeatherData avec température, description, icône
     */
    public static WeatherData getMeteo(String lieu) {
        if (lieu == null || lieu.trim().isEmpty()) {
            return new WeatherData("N/A", "Lieu non spécifié", "01d", false);
        }

        // Nettoyage du lieu (garder juste la ville)
        String ville = extraireVille(lieu);

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("🌤️ RÉCUPÉRATION MÉTÉO");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📍 Lieu brut: " + lieu);
        System.out.println("🏙️ Ville extraite: " + ville);
        System.out.println("🔑 API Key: " + API_KEY.substring(0, 8) + "...");
        System.out.println("═══════════════════════════════════════════════\n");

        try {
            // 🌐 Construction de l'URL avec paramètres
            String urlString = String.format(
                "%s?q=%s&appid=%s&units=metric&lang=fr",
                API_URL,
                URLEncoder.encode(ville, StandardCharsets.UTF_8),
                API_KEY
            );

            System.out.println("🌐 URL API: " + urlString);

            // 📡 Requête HTTP
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("📊 Code réponse: " + responseCode);

            if (responseCode == 200) {
                // ✅ Lecture de la réponse
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                // 📋 Parsing JSON
                JSONObject json = new JSONObject(response.toString());

                // Température
                double temp = json.getJSONObject("main").getDouble("temp");
                String temperature = Math.round(temp) + "°C";

                // Description (ex: "ciel dégagé", "nuageux")
                JSONArray weatherArray = json.getJSONArray("weather");
                String description = weatherArray.getJSONObject(0).getString("description");
                String icon = weatherArray.getJSONObject(0).getString("icon");

                // Capitaliser la première lettre
                description = description.substring(0, 1).toUpperCase() + description.substring(1);

                System.out.println("\n✅ MÉTÉO RÉCUPÉRÉE AVEC SUCCÈS !");
                System.out.println("   🌡️ Température: " + temperature);
                System.out.println("   ☁️ Conditions: " + description);
                System.out.println("   🎨 Icône: " + icon);

                return new WeatherData(temperature, description, icon, true);

            } else if (responseCode == 401) {
                System.err.println("❌ ERREUR 401 : Clé API invalide ou inactive");
                System.err.println("   Vérifie que ta clé est correcte et active (peut prendre 5-10 min)");
                return new WeatherData("N/A", "Clé API invalide", "01d", false);

            } else if (responseCode == 404) {
                System.err.println("❌ ERREUR 404 : Ville non trouvée");
                System.err.println("   Ville recherchée: " + ville);
                return new WeatherData("N/A", "Ville non trouvée", "01d", false);

            } else {
                System.err.println("❌ ERREUR " + responseCode);
                return new WeatherData("N/A", "Erreur API", "01d", false);
            }

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR LORS DE LA RÉCUPÉRATION MÉTÉO");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            return new WeatherData("N/A", "Erreur connexion", "01d", false);
        }
    }

    /**
     * 🏙️ Extrait le nom de la ville depuis un lieu complet
     * Exemples :
     * "Tunis, Tunisie" → "Tunis"
     * "ESPRIT, Tunis" → "Tunis"
     * "36.8625,10.1956" → "Tunis" (si coordonnées)
     */
    private static String extraireVille(String lieu) {
        if (lieu == null) return "Tunis";

        // Nettoyer
        lieu = lieu.trim();

        // Si coordonnées GPS, retourner Tunis par défaut
        if (lieu.matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) {
            return "Tunis";
        }

        // Si contient une virgule, prendre la dernière partie
        if (lieu.contains(",")) {
            String[] parts = lieu.split(",");
            return parts[parts.length - 1].trim();
        }

        // Sinon, retourner tel quel
        return lieu;
    }

    /**
     * ✅ Vérifie si le service météo est configuré
     */
    public static boolean estConfigurer() {
        return !API_KEY.equals("VOTRE_CLE_API");
    }

    /**
     * 🎨 Retourne l'emoji correspondant aux conditions météo
     */
    public static String getEmojiMeteo(String icon) {
        if (icon == null) return "🌤️";

        switch (icon.substring(0, 2)) {
            case "01": return "☀️";  // Ciel dégagé
            case "02": return "🌤️";  // Peu nuageux
            case "03": return "⛅";  // Nuageux
            case "04": return "☁️";  // Très nuageux
            case "09": return "🌧️";  // Pluie
            case "10": return "🌦️";  // Pluie légère
            case "11": return "⛈️";  // Orage
            case "13": return "❄️";  // Neige
            case "50": return "🌫️";  // Brouillard
            default: return "🌤️";
        }
    }

    /**
     * 📦 Classe pour stocker les données météo
     */
    public static class WeatherData {
        private final String temperature;
        private final String description;
        private final String icon;
        private final boolean success;

        public WeatherData(String temperature, String description, String icon, boolean success) {
            this.temperature = temperature;
            this.description = description;
            this.icon = icon;
            this.success = success;
        }

        public String getTemperature() { return temperature; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public boolean isSuccess() { return success; }

        @Override
        public String toString() {
            return temperature + " - " + description;
        }
    }
}


