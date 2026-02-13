package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
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
            } catch (Exception ignored) {
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}