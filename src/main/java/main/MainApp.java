package main;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.GEvenement.Evenement;
import services.EvenementService.EvenementService;
import utils.WindowsToastNotifier;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Active l'usage des proxies système si un proxy auto est détecté (PAC/WPAD)
        // Sans ça, Java peut échouer en ConnectException alors que Chrome fonctionne.
        System.setProperty("java.net.useSystemProxies", "true");

        // Diagnostic réseau (console) pour debug si la carte ne se charge pas
        try {
            System.out.println("\n=== NetDiagnostics ===\n" + utils.NetDiagnostics.quick());
        } catch (Exception ignored) {
        }

        // Log toutes les exceptions JavaFX non catchées (FXML/initialize etc.)
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("\n❌ Uncaught exception dans le thread: " + t.getName());
            e.printStackTrace();
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("L'application a rencontré une erreur");
                alert.setContentText(e.toString());
                alert.showAndWait();
            } catch (Exception ignored1) {
                // si JavaFX n'est pas disponible à ce moment, au moins la console contient la trace
            }
        });

        URL fxml = getClass().getResource("/views/login.fxml");
        if (fxml == null) {
            throw new IllegalStateException("FXML introuvable: /views/login.fxml (vérifie qu'il est bien dans src/main/resources/views)");
        }

        Parent root = FXMLLoader.load(fxml);
        Scene scene = new Scene(root, 900, 600);

        primaryStage.setTitle("BoostUp - Connexion");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Reminder: show toast(s) for events in exactly 2 days (after a short delay)
        PauseTransition delay = new PauseTransition(Duration.millis(800));
        delay.setOnFinished(e -> {
            Thread worker = new Thread(() -> {
                try {
                    System.out.println("\n🔔 === VÉRIFICATION DES NOTIFICATIONS ===");
                    EvenementService evenementService = new EvenementService();
                    List<Evenement> inTwoDays = evenementService.getEventsInExactlyTwoDays();

                    System.out.println("📅 Nombre d'événements dans 2 jours : " + inTwoDays.size());

                    if (inTwoDays.isEmpty()) {
                        System.out.println("⚠️ Aucun événement trouvé dans exactement 2 jours.");
                        return;
                    }

                    System.out.println("✅ Événements trouvés :");
                    for (Evenement evt : inTwoDays) {
                        System.out.println("  - " + evt.getTitre() + " le " + evt.getDateEvenement());
                    }

                    // 🔔 ENVOI DES NOTIFICATIONS WINDOWS NATIVES
                    System.out.println("\n🪟 Envoi des notifications système Windows...");

                    for (Evenement evt : inTwoDays) {
                        String titre = evt.getTitre() != null ? evt.getTitre() : "Événement";
                        String lieu = evt.getLieu() != null ? evt.getLieu() : "Lieu non spécifié";

                        // Format de la date (conversion java.sql.Date -> LocalDate)
                        String dateFormatted;
                        if (evt.getDateEvenement() != null) {
                            LocalDate localDate = evt.getDateEvenement().toLocalDate();
                            dateFormatted = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        } else {
                            dateFormatted = "Date non spécifiée";
                        }

                        // Envoi de la notification native Windows
                        WindowsToastNotifier.sendEventReminder(
                            titre,
                            dateFormatted,
                            lieu,
                            2 // 2 jours restants
                        );

                        // Petite pause entre chaque notification pour éviter le spam
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }


                    System.out.println("✅ Notifications envoyées avec succès !\n");

                } catch (Exception ex) {
                    System.err.println("❌ ERREUR lors de la vérification des notifications :");
                    ex.printStackTrace();
                }
            });
            worker.setDaemon(true);
            worker.start();
        });
        delay.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}