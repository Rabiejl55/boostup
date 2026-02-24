package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal lié à MainFrontView.fxml.
 * Joue également le rôle d'AppController en implémentant refreshSidebarStats().
 */
public class MainFrontController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button    btnNavCandidatures;
    @FXML private Button    btnNavDossiers;

    // Référence vers soi-même en tant qu'AppController
    // pour pouvoir l'injecter dans les sous-contrôleurs
    private final AppController selfAsApp = new AppController() {
        @Override
        public void refreshSidebarStats() {
            // Ajoutez ici la logique si vous avez des compteurs
            // dans MainFrontView.fxml (Labels dans la sidebar, etc.)
        }
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showCandidatures(); // Vue par défaut au démarrage
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML
    public void showCandidatures() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/CandidatureFrontView.fxml"));
            Node view = loader.load();

            CandidatureFrontController ctrl = loader.getController();
            ctrl.setAppController(selfAsApp);

            contentArea.getChildren().setAll(view);
            setActive(btnNavCandidatures, btnNavDossiers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showDossiers() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/DossierFrontView.fxml"));
            Node view = loader.load();

            DossierFrontController ctrl = loader.getController();
            ctrl.setAppController(selfAsApp);

            contentArea.getChildren().setAll(view);
            setActive(btnNavDossiers, btnNavCandidatures);
        } catch (IOException e) {
            e.printStackTrace();
        }
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