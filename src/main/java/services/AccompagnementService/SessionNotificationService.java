package services.AccompagnementService;

import entities.GAccompagnement.Session;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.time.LocalDate;
import java.util.List;

public class SessionNotificationService {

    private final List<Session> sessions;

    public SessionNotificationService(List<Session> sessions) {
        this.sessions = sessions;
    }

    // Vérifie toutes les sessions pour les notifications
    public void checkNotifications() {
        try {
            List<Session> sessions = new SessionService().afficherAll(); // recharger depuis DB
            LocalDate today = LocalDate.now();

            for (Session session : sessions) {
                LocalDate dateSession = session.getDateSession();
                if (dateSession != null && !dateSession.isBefore(today) && today.plusDays(3).equals(dateSession)) {
                    sendNotification(session);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Affiche la notification JavaFX
    private void sendNotification(Session session) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🔔 Rappel de session");
            alert.setHeaderText("Session à venir !");

            String coachName = session.getCoach() != null
                    ? session.getCoach().getNom() + " " + session.getCoach().getPrenom()
                    : "Aucun";

            String domaineName = session.getDomaine() != null
                    ? session.getDomaine().getNom()
                    : "Aucun";

            alert.setContentText(
                    "Session : " + session.getTypeSession() +
                            "\nDate : " + session.getDateSession() +
                            "\nLieu : " + session.getLieu() +
                            "\nCoach : " + coachName +
                            "\nDomaine : " + domaineName +
                            "\nObjectif : " + session.getObjectif()
            );

            alert.showAndWait();
        });
    }
}