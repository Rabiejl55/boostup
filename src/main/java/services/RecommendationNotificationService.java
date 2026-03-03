package services;

import entities.GEvenement.Evenement;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.sql.SQLException;
import java.util.List;

/**
 * 🤖 SERVICE DE NOTIFICATION DE RECOMMANDATIONS IA
 *
 * Envoie des notifications Windows pour suggérer les meilleurs événements
 */
public class RecommendationNotificationService {

    private static RecommendationNotificationService instance;
    private final RecommendationService recommendationService;

    private RecommendationNotificationService() throws SQLException {
        this.recommendationService = new RecommendationService();
    }

    public static synchronized RecommendationNotificationService getInstance() throws SQLException {
        if (instance == null) {
            instance = new RecommendationNotificationService();
        }
        return instance;
    }

    /**
     * 🔔 Envoie des notifications pour les événements recommandés
     */
    public void sendRecommendationNotifications() {
        try {
            System.out.println("\n🤖 ENVOI NOTIFICATIONS RECOMMANDATIONS IA");
            System.out.println("═══════════════════════════════════════");

            // Récupérer les meilleures recommandations (top 3)
            List<RecommendationService.EventRecommendation> recommendations =
                recommendationService.getRecommendations(1); // User ID fictif

            if (recommendations.isEmpty()) {
                System.out.println("⚠️ Aucune recommandation disponible");
                return;
            }

            // Envoyer une notification pour chaque recommandation (max 3)
            int count = 0;
            for (RecommendationService.EventRecommendation reco : recommendations) {
                if (count >= 3) break; // Max 3 notifications

                if (reco.getScore() >= 0.6) { // Seuil de pertinence
                    sendNotification(reco);
                    count++;
                    Thread.sleep(1500); // Délai entre les notifications
                }
            }

            System.out.println("\n✅ " + count + " notification(s) de recommandation envoyée(s)");
            System.out.println("═══════════════════════════════════════\n");

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notifications recommandations : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie une notification Windows pour une recommandation
     */
    private void sendNotification(RecommendationService.EventRecommendation reco) {
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("⚠️ System Tray non supporté");
                return;
            }

            Evenement event = reco.getEvent();
            double score = reco.getScore();

            // Créer le titre selon le score
            String title;
            if (score >= 0.8) {
                title = "⭐⭐⭐ Événement fortement recommandé !";
            } else if (score >= 0.6) {
                title = "💜 Cet événement pourrait vous plaire";
            } else {
                title = "✨ Suggestion d'événement";
            }

            // Créer le message
            StringBuilder message = new StringBuilder();
            message.append("🎯 ").append(event.getTitre()).append("\n");
            message.append("📅 ").append(event.getDateEvenement()).append("\n");
            message.append("📍 ").append(event.getLieu()).append("\n");
            message.append("💡 Match : ").append(String.format("%.0f%%", score * 100));

            System.out.println("🔔 Envoi notification : " + title);
            System.out.println("   " + event.getTitre() + " (" + String.format("%.0f%%", score * 100) + " match)");

            // Afficher la notification
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");

            TrayIcon trayIcon = new TrayIcon(image, "BoostUp - Recommandations IA");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("BoostUp - Événements recommandés");

            tray.add(trayIcon);

            // Choisir le type de notification selon le score
            MessageType messageType;
            if (score >= 0.8) {
                messageType = MessageType.INFO;
            } else if (score >= 0.6) {
                messageType = MessageType.INFO;
            } else {
                messageType = MessageType.NONE;
            }

            trayIcon.displayMessage(title, message.toString(), messageType);

            // Retirer l'icône après 5 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    tray.remove(trayIcon);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("⚠️ Erreur affichage notification : " + e.getMessage());
        }
    }
}

