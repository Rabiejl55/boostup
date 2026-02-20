import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import entities.GAccompagnement.Session;
import services.AccompagnementService.SessionNotificationService;
import services.AccompagnementService.NotificationScheduler;
import services.AccompagnementService.SessionService;

import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // ----- Charger le FXML -----
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/accompagnement.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setTitle("BoostUp - Accompagnement");
            primaryStage.setScene(scene);
            primaryStage.show();

            // ----- Récupérer les sessions depuis la base -----
            SessionService sessionService = new SessionService();
            List<Session> sessions = sessionService.afficherAll(); // toutes les sessions avec coach/domaine

            // ----- Créer le service de notification -----
            SessionNotificationService notificationService = new SessionNotificationService(sessions);

            // ----- Lancer le scheduler -----
            NotificationScheduler scheduler = new NotificationScheduler(notificationService);
            scheduler.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}