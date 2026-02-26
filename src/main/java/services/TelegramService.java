package services;

import entities.GEvenement.Evenement;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 📱 SERVICE TELEGRAM BOT - Envoi de notifications via Telegram
 * Alternative GRATUITE et FIABLE aux SMS
 *
 * Configuration requise :
 * 1. Créer un bot via @BotFather sur Telegram
 * 2. Obtenir le TOKEN du bot
 * 3. Obtenir ton CHAT_ID (envoie un message au bot puis va sur :
 *    https://api.telegram.org/bot<TOKEN>/getUpdates)
 */
public class TelegramService {

    // ════════════════════════════════════════════════════════
    // 🔐 CONFIGURATION TELEGRAM BOT
    // ════════════════════════════════════════════════════════

    // ✅ CONFIGURATION TELEGRAM BOT - CONFIGURÉ ET PRÊT ! ✅
    private static final String BOT_TOKEN = "8758479822:AAHb8xxCsL5dM_OvaLdW7fbWcfsr928oK_U";  // ✅ Token configuré
    private static final String CHAT_ID = "8668259497";      // ✅ Chat ID configuré (Rayen Amri)

    // URL de l'API Telegram
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    // ════════════════════════════════════════════════════════
    // 📤 ENVOI NOTIFICATION ÉVÉNEMENT ANNULÉ
    // ════════════════════════════════════════════════════════

    /**
     * Envoie une notification Telegram pour un événement annulé
     * @param evenement L'événement supprimé
     */
    public static void envoyerNotificationAnnulation(Evenement evenement) {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("📱 ENVOI NOTIFICATION TELEGRAM");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("🤖 Bot Token: " + BOT_TOKEN.substring(0, Math.min(10, BOT_TOKEN.length())) + "...");
        System.out.println("💬 Chat ID: " + CHAT_ID);
        System.out.println("📅 Événement: " + evenement.getTitre());
        System.out.println("═══════════════════════════════════════════════\n");

        // Exécution asynchrone
        new Thread(() -> {
            try {
                System.out.println("🔄 Construction du message...");

                // 📝 Construction du message (avec émojis et formatage Markdown)
                String message = construireMessageAnnulation(evenement);
                System.out.println("📝 Message construit :");
                System.out.println(message);

                // 🚀 Envoi via l'API Telegram
                boolean success = envoyerMessage(message);

                if (success) {
                    System.out.println("\n✅✅✅ NOTIFICATION TELEGRAM ENVOYÉE ! ✅✅✅");

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Notification Telegram envoyée ✅");
                        alert.setHeaderText("Événement annulé - Notification envoyée");
                        alert.setContentText("Une notification Telegram a été envoyée !\n\n" +
                                           "Vérifie ton téléphone 📱");
                        alert.showAndWait();
                    });
                } else {
                    System.err.println("\n❌ Échec de l'envoi Telegram");

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Notification non envoyée");
                        alert.setHeaderText("Telegram non configuré");
                        alert.setContentText("Configure ton bot Telegram dans TelegramService.java\n\n" +
                                           "Voir les instructions dans le code source.");
                        alert.showAndWait();
                    });
                }

            } catch (Exception e) {
                System.err.println("\n❌ ERREUR TELEGRAM");
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur Telegram");
                    alert.setHeaderText("Impossible d'envoyer la notification");
                    alert.setContentText("Erreur : " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * 📝 Construit le message Telegram avec formatage Markdown
     */
    private static String construireMessageAnnulation(Evenement evenement) {
        String dateFormatee = evenement.getDateEvenement() != null
            ? evenement.getDateEvenement().toString()
            : "Date non définie";

        // Message avec émojis et formatage Telegram Markdown
        return "🚫 *ÉVÉNEMENT ANNULÉ*\n\n" +
               "━━━━━━━━━━━━━━━━━━━━\n\n" +
               "Nous sommes désolés de vous informer que l'événement suivant est *annulé* :\n\n" +
               "📌 *Titre :* " + evenement.getTitre() + "\n" +
               "📅 *Date :* " + dateFormatee + "\n" +
               "📍 *Lieu :* " + evenement.getLieu() + "\n\n" +
               "━━━━━━━━━━━━━━━━━━━━\n\n" +
               "❗ _Cet événement a été annulé pour des raisons indépendantes de notre volonté._\n\n" +
               "💡 *Prochaines étapes :*\n" +
               "• Consultez notre plateforme pour d'autres événements\n" +
               "• Vous serez informé de nos prochains événements\n" +
               "• Contactez-nous pour toute question\n\n" +
               "À la prochaine inchallah ! 🙏\n\n" +
               "_BoostUp Team_ 🚀";
    }

    /**
     * 🌐 Envoie un message via l'API Telegram
     * @param message Le message à envoyer
     * @return true si succès, false sinon
     */
    private static boolean envoyerMessage(String message) {
        try {
            // Construction de l'URL de l'API
            String urlString = TELEGRAM_API_URL + BOT_TOKEN + "/sendMessage";
            URL url = new URL(urlString);

            System.out.println("🌐 Connexion à l'API Telegram : " + urlString.replace(BOT_TOKEN, "***"));

            // Connexion HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            // Encodage du message pour URL
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            // Corps de la requête
            String postData = "chat_id=" + CHAT_ID +
                            "&text=" + encodedMessage +
                            "&parse_mode=Markdown";  // Active le formatage Markdown

            System.out.println("📤 Envoi des données...");

            // Envoi de la requête
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lecture de la réponse
            int responseCode = conn.getResponseCode();
            System.out.println("📊 Code de réponse HTTP : " + responseCode);

            if (responseCode == 200) {
                System.out.println("✅ Message envoyé avec succès !");
                return true;
            } else {
                System.err.println("❌ Erreur HTTP " + responseCode);

                // Lire le message d'erreur
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err.println("📋 Réponse API : " + response);
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Exception lors de l'envoi : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ Vérifie si Telegram est correctement configuré
     */
    public static boolean estConfigurer() {
        boolean tokenOk = !BOT_TOKEN.equals("VOTRE_BOT_TOKEN_ICI") && BOT_TOKEN.length() > 10;
        boolean chatIdOk = !CHAT_ID.equals("VOTRE_CHAT_ID_ICI") && CHAT_ID.matches("\\d+");

        if (!tokenOk) {
            System.err.println("⚠️ BOT_TOKEN non configuré dans TelegramService.java");
        }
        if (!chatIdOk) {
            System.err.println("⚠️ CHAT_ID non configuré dans TelegramService.java");
        }

        return tokenOk && chatIdOk;
    }

    /**
     * 🧪 Méthode de test pour vérifier la configuration
     */
    public static void testerConfiguration() {
        System.out.println("\n🧪 TEST CONFIGURATION TELEGRAM");
        System.out.println("═══════════════════════════════════════");
        System.out.println("Bot Token configuré : " + !BOT_TOKEN.equals("VOTRE_BOT_TOKEN_ICI"));
        System.out.println("Chat ID configuré : " + !CHAT_ID.equals("VOTRE_CHAT_ID_ICI"));
        System.out.println("Service prêt : " + estConfigurer());
        System.out.println("═══════════════════════════════════════\n");

        if (estConfigurer()) {
            System.out.println("✅ Configuration OK ! Envoi d'un message de test...\n");
            boolean success = envoyerMessage("🧪 *TEST BoostUp*\n\nSi tu reçois ce message, tout fonctionne ! ✅");

            if (success) {
                System.out.println("\n✅ MESSAGE TEST ENVOYÉ ! Vérifie ton Telegram 📱");
            } else {
                System.out.println("\n❌ Échec de l'envoi. Vérifie ton TOKEN et CHAT_ID.");
            }
        } else {
            System.out.println("❌ Configuration incomplète. Suis les étapes :\n");
            System.out.println("1️⃣ Ouvre Telegram et cherche @BotFather");
            System.out.println("2️⃣ Envoie /newbot et suis les instructions");
            System.out.println("3️⃣ Copie le TOKEN reçu");
            System.out.println("4️⃣ Envoie un message à ton bot");
            System.out.println("5️⃣ Va sur : https://api.telegram.org/bot<TON_TOKEN>/getUpdates");
            System.out.println("6️⃣ Copie ton chat_id depuis la réponse JSON");
            System.out.println("7️⃣ Colle TOKEN et CHAT_ID dans TelegramService.java\n");
        }
    }
}



