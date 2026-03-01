package main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // ─── Titre ───────────────────────────────────────────────────────────
        Label title = new Label("BOOSTUP");
        title.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:#2d3a8c;");

        Label subtitle = new Label("Choisissez votre espace");
        subtitle.setStyle("-fx-font-size:16px; -fx-text-fill:#6b7280;");

        // ─── Front Office ────────────────────────────────────────────────────
        Button btnFront = createButton("🖥️  Front Office", "#2d3a8c", "#1e2a6e");
        btnFront.setOnAction(e -> openFrontOffice(stage));

        Label descFront = new Label("Candidatures & Dossiers\n(espace candidat)");
        descFront.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af; -fx-text-alignment:center;");
        descFront.setAlignment(Pos.CENTER);

        VBox frontBox = new VBox(10, btnFront, descFront);
        frontBox.setAlignment(Pos.CENTER);

        // ─── Back Office ─────────────────────────────────────────────────────
        Button btnBack = createButton("⚙️  Back Office", "#059669", "#047857");
        btnBack.setOnAction(e -> openBackOffice(stage));

        Label descBack = new Label("Évaluations & Administration\n(espace administrateur)");
        descBack.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af; -fx-text-alignment:center;");
        descBack.setAlignment(Pos.CENTER);

        VBox backBox = new VBox(10, btnBack, descBack);
        backBox.setAlignment(Pos.CENTER);

        // ─── Séparateur ──────────────────────────────────────────────────────
        Label sep = new Label("│");
        sep.setStyle("-fx-font-size:48px; -fx-text-fill:#e5e7eb;");

        // ─── Layout ──────────────────────────────────────────────────────────
        HBox buttons = new HBox(50, frontBox, sep, backBox);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(30, title, subtitle, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color:#f9fafb;");

        Scene scene = new Scene(root, 620, 380);
        loadCss(scene);

        stage.setTitle("BOOSTUP — Sélection de l'espace");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    // ─── Ouverture Front Office ───────────────────────────────────────────────
    private void openFrontOffice(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainFrontView.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 820);
            loadCss(scene);
            stage.setTitle("BOOSTUP — Front Office");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1050);
            stage.setMinHeight(680);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir le Front Office :\n" + e.getMessage());
        }
    }

    // ─── Ouverture Back Office → Dashboard directement ───────────────────────
    private void openBackOffice(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/DashboardBackofficeView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            loadCss(scene);
            stage.setTitle("BOOSTUP — Back Office — Dashboard");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir le Back Office :\n" + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private Button createButton(String text, String colorNormal, String colorHover) {
        String baseStyle = """
                -fx-text-fill:white;
                -fx-font-size:15px;
                -fx-font-weight:bold;
                -fx-padding:18 40;
                -fx-background-radius:12;
                -fx-cursor:hand;
                -fx-pref-width:220px;
                """;
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + colorNormal + ";" + baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + colorHover + ";" + baseStyle));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:" + colorNormal + ";" + baseStyle));
        return btn;
    }

    private void loadCss(Scene scene) {
        for (String path : new String[]{"/style.css", "/css/style.css", "/styles/style.css"}) {
            URL css = getClass().getResource(path);
            if (css != null) { scene.getStylesheets().add(css.toExternalForm()); return; }
        }
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}