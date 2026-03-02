package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * MenuBackofficeController
 * Gère les clics sur les 3 cartes du menu d'accueil.
 * Chaque clic ouvre une nouvelle fenêtre (avec sidebar navy intégrée).
 */
public class MenuBackofficeController {

    @FXML public void openCandidatures() {
        openWindow("/fxml/CandidatureBackofficeView.fxml", "Candidatures");
    }

    @FXML public void openDossiers() {
        openWindow("/fxml/DossierCandidatureBackofficeView.fxml", "Dossiers");
    }

    @FXML public void openEvaluations() {
        openWindow("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
    }

    private void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            HBox root = loader.load();
            Scene scene = new Scene(root, 1200, 750);
            loadCss(scene);

            Stage stage = new Stage();
            stage.setTitle("BOOSTUP — Back Office — " + title);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur"); a.setHeaderText(null);
            a.setContentText("Impossible d'ouvrir la fenêtre : " + e.getMessage());
            a.showAndWait();
        }
    }

    private void loadCss(Scene scene) {
        for (String path : new String[]{"/style.css", "/css/style.css"}) {
            URL css = getClass().getResource(path);
            if (css != null) { scene.getStylesheets().add(css.toExternalForm()); return; }
        }
    }
}
