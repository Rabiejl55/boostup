package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application (AppView.fxml).
 * Gère la navigation sidebar et le rafraîchissement des statistiques globales.
 */
public class AppController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button    btnNavCandidatures;
    @FXML private Button    btnNavDossiers;

    // Labels sidebar optionnels (peuvent être absents du FXML)
    @FXML private Label lblSidebarCandidatures;
    @FXML private Label lblSidebarDossiers;

    // Références aux sous-contrôleurs actuellement chargés
    private CandidatureFrontController candidatureCtrl;
    private DossierFrontController     dossierCtrl;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showCandidatures();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML
    public void showCandidatures() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidatureFrontView.fxml"));
            Node view = loader.load();
            candidatureCtrl = loader.getController();
            candidatureCtrl.setAppController(this);  // ← injection du parent
            dossierCtrl = null;

            contentArea.getChildren().setAll(view);
            setActive(btnNavCandidatures, btnNavDossiers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showDossiers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DossierFrontView.fxml"));
            Node view = loader.load();
            dossierCtrl = loader.getController();
            dossierCtrl.setAppController(this);  // ← injection du parent
            candidatureCtrl = null;

            contentArea.getChildren().setAll(view);
            setActive(btnNavDossiers, btnNavCandidatures);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Stats sidebar ───────────────────────────────────────────────────────

    /**
     * Appelé par les sous-contrôleurs après chaque modification
     * pour rafraîchir les compteurs affichés dans la sidebar.
     */
    public void refreshSidebarStats() {
        // Si votre sidebar affiche des compteurs globaux,
        // implémentez la logique ici.
        // Exemple (si vous avez des Labels dans AppView.fxml) :
        //
        // try {
        //     long nbCand = new CandidatureService().getAllCandidatures(true).size();
        //     long nbDoss = new DossierCandidatureService().getAllDossiers(true).size();
        //     if (lblSidebarCandidatures != null) lblSidebarCandidatures.setText(String.valueOf(nbCand));
        //     if (lblSidebarDossiers    != null) lblSidebarDossiers.setText(String.valueOf(nbDoss));
        // } catch (SQLException ignored) {}
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setActive(Button active, Button... others) {
        active.getStyleClass().removeAll("nav-item", "nav-item-active");
        active.getStyleClass().addAll("nav-item", "nav-item-active");
        for (Button b : others) {
            b.getStyleClass().removeAll("nav-item", "nav-item-active");
            b.getStyleClass().add("nav-item");
        }
    }
}