package utils;

import entities.GEvenement.Evenement;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.text.SimpleDateFormat;

/**
 * 🎯 EventReminderToast - VERSION ALERT GARANTIE
 *
 * Utilise des Alert JavaFX pour GARANTIR l'affichage des notifications
 */
public class EventReminderToast {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public EventReminderToast(Evenement event, int index) {
        // Petit délai pour espacer les alerts
        int delay = index * 300; // 300ms entre chaque

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
            javafx.util.Duration.millis(delay)
        );

        pause.setOnFinished(e -> Platform.runLater(() -> {
            try {
                showNotification(event);
                System.out.println("✅ Notification affichée pour : " + event.getTitre());
            } catch (Exception ex) {
                System.err.println("❌ Erreur affichage notification : " + ex.getMessage());
                ex.printStackTrace();
            }
        }));

        pause.play();
    }

    private void showNotification(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        // Style moderne
        alert.setTitle("🎉 BoostUp - Rappel d'Événement");
        alert.setHeaderText(event.getTitre());

        String dateStr = DATE_FORMAT.format(event.getDateEvenement());
        String message = String.format(
            "📅 Date: %s\n" +
            "📍 Lieu: %s\n\n" +
            "⏳ Rappel: Cet événement aura lieu dans 2 jours !",
            dateStr,
            event.getLieu() != null ? event.getLieu() : "Non spécifié"
        );

        alert.setContentText(message);

        // Personnalisation du bouton
        alert.getButtonTypes().setAll(ButtonType.OK);

        // Affichage NON-BLOQUANT
        alert.show();

        System.out.println("🎊 Alert affichée : " + event.getTitre());
    }
}

