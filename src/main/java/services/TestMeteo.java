package services;

import services.WeatherService.WeatherData;

/**
 * 🧪 TEST MÉTÉO - OpenWeatherMap
 * Lance ce fichier pour tester l'API météo
 */
public class TestMeteo {

    public static void main(String[] args) {
        System.out.println("\n\n");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🌤️ TEST API MÉTÉO - OpenWeatherMap");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // ⚠️ AVANT DE LANCER CE TEST
        System.out.println("⚠️  CONFIGURATION REQUISE :");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("1️⃣  Inscris-toi sur : https://openweathermap.org/api");
        System.out.println("2️⃣  Confirme ton email");
        System.out.println("3️⃣  Récupère ta clé API sur : https://home.openweathermap.org/api_keys");
        System.out.println("4️⃣  Colle la clé dans WeatherService.java ligne 18");
        System.out.println("    private static final String API_KEY = \"ta-clé-ici\";");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Vérification
        if (!WeatherService.estConfigurer()) {
            System.err.println("❌ SERVICE MÉTÉO NON CONFIGURÉ !");
            System.err.println("   Ajoute ta clé API dans WeatherService.java avant de relancer.\n");
            return;
        }

        System.out.println("✅ Service météo configuré, lancement des tests...\n");

        // 🧪 Test 1 : Tunis
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🧪 TEST 1 : Tunis");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        WeatherData meteoTunis = WeatherService.getMeteo("Tunis");
        afficherResultat(meteoTunis);

        pause(2000);

        // 🧪 Test 2 : Paris
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🧪 TEST 2 : Paris");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        WeatherData meteoParis = WeatherService.getMeteo("Paris");
        afficherResultat(meteoParis);

        pause(2000);

        // 🧪 Test 3 : Lieu complexe (comme dans l'app)
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🧪 TEST 3 : ESPRIT, Tunis");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        WeatherData meteoEsprit = WeatherService.getMeteo("ESPRIT, Tunis");
        afficherResultat(meteoEsprit);

        System.out.println("\n\n═══════════════════════════════════════════════════════════");
        System.out.println("🏁 TESTS TERMINÉS");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("\n💡 PROCHAINE ÉTAPE :");
        System.out.println("   Si tout marche, on intègre la météo dans EventDetailPremium !");
        System.out.println("\n");
    }

    private static void afficherResultat(WeatherData meteo) {
        if (meteo.isSuccess()) {
            String emoji = WeatherService.getEmojiMeteo(meteo.getIcon());
            System.out.println("\n✅ SUCCÈS !");
            System.out.println("   " + emoji + " Température : " + meteo.getTemperature());
            System.out.println("   ☁️ Conditions   : " + meteo.getDescription());
            System.out.println("   🎨 Icône        : " + meteo.getIcon());
        } else {
            System.out.println("\n❌ ÉCHEC");
            System.out.println("   " + meteo.getDescription());
        }
    }

    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

