package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 🔔 WindowsToastNotifier - Notifications système Windows natives
 *
 * Cette classe permet d'envoyer des notifications toast Windows 10/11
 * qui apparaissent dans le centre de notifications du système.
 *
 * Compatible avec Windows 10 et Windows 11.
 *
 * @author BoostUp Team
 * @version 2.0
 */
public class WindowsToastNotifier {

    /**
     * Envoie une notification toast Windows native
     *
     * @param title Titre de la notification (ex: "Événement à venir")
     * @param message Message principal (ex: "PIDEV dans 2 jours")
     * @param details Détails supplémentaires (optionnel)
     */
    public static void sendNotification(String title, String message, String details) {
        try {
            // Trouver le chemin du script PowerShell
            String projectRoot = System.getProperty("user.dir");
            String scriptPath = projectRoot + "\\send_notification_direct.ps1";

            // Échapper les guillemets dans les paramètres
            String titleEscaped = title.replace("\"", "`\"");
            String messageEscaped = message.replace("\"", "`\"");
            String detailsEscaped = (details != null ? details : "").replace("\"", "`\"");

            // Exécution du script PowerShell avec paramètres
            ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptPath,
                "-Title", titleEscaped,
                "-Message", messageEscaped,
                "-Details", detailsEscaped
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Lecture de la sortie (pour debug)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Notification Windows envoyée : " + title);
            } else {
                System.err.println("⚠️ Erreur notification (code: " + exitCode + ")");
                if (output.length() > 0) {
                    System.err.println(output.toString());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur notification Windows:");
            e.printStackTrace();

            // Fallback: message console visible
            System.out.println("\n========================================");
            System.out.println("NOTIFICATION : " + title);
            System.out.println("Message : " + message);
            if (details != null && !details.isEmpty()) {
                System.out.println("Details : " + details);
            }
            System.out.println("========================================\n");
        }
    }

    /**
     * Envoie une notification d'événement à venir
     */
    public static void sendEventReminder(String eventTitle, String eventDate, String eventLocation, int daysRemaining) {
        String title = "🎯 Rappel Événement - BoostUp";
        String message = String.format("📅 %s", eventTitle);
        String details = String.format("📍 %s | ⏳ Dans %d jour%s",
            eventLocation,
            daysRemaining,
            daysRemaining > 1 ? "s" : ""
        );

        sendNotification(title, message, details);
    }

    /**
     * Test de la notification
     */
    public static void main(String[] args) {
        System.out.println("🧪 Test de notification Windows...\n");

        // Test 1: Notification simple
        sendNotification(
            "🚀 BoostUp Event Manager",
            "Système de notifications activé!",
            "Les notifications apparaîtront dans le centre de notifications Windows"
        );

        // Test 2: Notification d'événement
        try {
            Thread.sleep(2000); // Pause de 2s entre les notifications
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendEventReminder(
            "Hackathon PIDEV 2026",
            "2026-02-25",
            "Station F, Paris",
            2
        );

        System.out.println("\n✅ Tests terminés! Vérifie ton centre de notifications Windows 🔔");
    }
}




