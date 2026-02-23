package Controllers;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class MenuBackofficeController {

    @FXML
    private void openCandidatures() {
        openView("/fxml/CandidatureBackofficeView.fxml", "Candidatures");
    }

    @FXML
    private void openDossiers() {
        openView("/fxml/DossierCandidatureBackofficeView.fxml", "Dossiers");
    }

    @FXML
    private void openEvaluations() {
        openView("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
    }

    /**
     * Méthode unique et fiable pour charger une vue
     */
    private void openView(String fxmlPath, String title) {
        try {
            // Récupérer la Stage actuelle AVANT de charger le nouveau FXML
            Stage currentStage = (Stage) javafx.stage.Window.getWindows().get(0);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene newScene = new Scene(loader.load(), 1000, 700);

            // Changer la scène
            currentStage.setScene(newScene);
            currentStage.setTitle("BoostUp - Backoffice - " + title);
            currentStage.centerOnScreen();

            // Réappliquer le CSS
            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                newScene.getStylesheets().add(css);
            }

            // Rafraîchissement automatique pour la vue Candidatures
            if (fxmlPath.contains("CandidatureBackofficeView.fxml")) {
                CandidatureBackofficeController ctrl = loader.getController();
                ctrl.refreshTable();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de chargement du FXML : " + fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur inattendue lors du changement de vue");
        }
    }
}