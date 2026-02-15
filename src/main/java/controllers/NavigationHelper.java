package controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class NavigationHelper {
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    private static final String APP_TITLE = "BoostUp";

    public static void navigateTo(Stage stage, String fxmlPath, String pageTitle) {
        try {
            // Save current window position
            double x = stage.getX();
            double y = stage.getY();

            // Load new scene
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(NavigationHelper.class.getResource("/css/style.css").toExternalForm());

            // Apply smooth transition effect
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            stage.setScene(scene);
            stage.setTitle(APP_TITLE + " - " + pageTitle);

            // Restore position
            stage.setX(x);
            stage.setY(y);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void navigateWithFade(Stage stage, Parent root, String title) {
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(NavigationHelper.class.getResource("/css/style.css").toExternalForm());

        // Add fade animation
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        stage.setScene(scene);
        stage.setTitle(APP_TITLE + " - " + title);
    }
}
