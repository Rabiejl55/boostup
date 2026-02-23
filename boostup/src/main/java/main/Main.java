package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBackoffice.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);

            // Ajout CSS si tu l'as
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }

            primaryStage.setTitle("BoostUp - Backoffice - Menu");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Impossible de charger MenuBackoffice.fxml");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}