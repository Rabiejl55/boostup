package utils;

import entities.GEvenement.Evenement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 🎯 WindowsToastHelper
 *
 * Affiche des notifications système Windows natives (toast) via PowerShell.
 * Ces notifications apparaissent dans le centre de notifications Windows.
 */
public final class WindowsToastHelper {

    private WindowsToastHelper() {}

    /**
     * Vérifie si le système d'exploitation est Windows
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    /**
     * Affiche une notification toast Windows native pour chaque événement à venir dans 2 jours
     */
    public static void showEventReminders(List<Evenement> events) {
        if (!isWindows() || events == null || events.isEmpty()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        for (Evenement event : events) {
            try {
                String titre = sanitize(event.getTitre());
                String date = dateFormat.format(event.getDateEvenement());
                String lieu = sanitize(event.getLieu());

                String message = String.format(
                    "📅 Date: %s\n📍 Lieu: %s\n⏳ Rappel: Cet événement aura lieu dans 2 jours.",
                    date, lieu
                );

                showToast("🎉 " + titre, message);

                // Petit délai entre les notifications pour éviter qu'elles se chevauchent
                Thread.sleep(300);

            } catch (Exception e) {
                System.err.println("⚠️ Erreur notification pour: " + event.getTitre());
                e.printStackTrace();
            }
        }
    }

    /**
     * Affiche une seule notification toast Windows via PowerShell
     */
    private static void showToast(String title, String message) throws Exception {
        // Créer un script PowerShell temporaire
        File tempScript = File.createTempFile("boostup_toast_", ".ps1");
        tempScript.deleteOnExit();

        // Script PowerShell pour afficher une notification moderne Windows 10/11
        String psScript = String.format(
            "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null\n" +
            "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null\n" +
            "\n" +
            "$APP_ID = 'BoostUp.EventManager'\n" +
            "\n" +
            "$template = @\"\n" +
            "<toast>\n" +
            "    <visual>\n" +
            "        <binding template=\"ToastGeneric\">\n" +
            "            <text>%s</text>\n" +
            "            <text>%s</text>\n" +
            "        </binding>\n" +
            "    </visual>\n" +
            "    <audio src=\"ms-winsoundevent:Notification.Default\" />\n" +
            "</toast>\n" +
            "\"@\n" +
            "\n" +
            "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument\n" +
            "$xml.LoadXml($template)\n" +
            "$toast = New-Object Windows.UI.Notifications.ToastNotification $xml\n" +
            "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($APP_ID).Show($toast)\n",
            escapeXml(title),
            escapeXml(message)
        );

        // Écrire le script
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript))) {
            writer.write(psScript);
        }

        // Exécuter le script PowerShell
        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-ExecutionPolicy", "Bypass",
            "-NoProfile",
            "-WindowStyle", "Hidden",
            "-File", tempScript.getAbsolutePath()
        );

        Process process = pb.start();

        // Attendre max 3 secondes
        if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
    }

    /**
     * Nettoie les caractères spéciaux pour éviter les problèmes d'affichage
     */
    private static String sanitize(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", " ").replace("\r", "");
    }

    /**
     * Échappe les caractères XML spéciaux
     */
    private static String escapeXml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

