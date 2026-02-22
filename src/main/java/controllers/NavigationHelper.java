package controllers;

import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class NavigationHelper {
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    private static final String APP_TITLE = "BoostUp";

    public static void navigateTo(Stage stage, String fxmlPath, String pageTitle) {
        try {
            double x = stage.getX();
            double y = stage.getY();

            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(NavigationHelper.class.getResource("/css/style.css").toExternalForm());

            // ── Animation d'entrée : fade in + slide up ──
            root.setOpacity(0);
            root.setTranslateY(15);

            stage.setScene(scene);
            stage.setTitle(APP_TITLE + " - " + pageTitle);
            stage.setX(x);
            stage.setY(y);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(350), root);
            slideUp.setFromY(15);
            slideUp.setToY(0);
            slideUp.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition parallel = new ParallelTransition(fadeIn, slideUp);
            parallel.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigation avec effet de sortie (fade out + blur) puis entrée
     */
    public static void navigateWithExit(Stage stage, Parent currentRoot, String fxmlPath, String pageTitle) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        GaussianBlur blur = new GaussianBlur(0);
        currentRoot.setEffect(blur);

        Timeline blurTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(blur.radiusProperty(), 5))
        );

        ParallelTransition exitTransition = new ParallelTransition(fadeOut, blurTimeline);
        exitTransition.setOnFinished(e -> navigateTo(stage, fxmlPath, pageTitle));
        exitTransition.play();
    }

    public static void navigateWithFade(Stage stage, Parent root, String title) {
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(NavigationHelper.class.getResource("/css/style.css").toExternalForm());

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();

        stage.setScene(scene);
        stage.setTitle(APP_TITLE + " - " + title);
    }
}