package Controllers;

import entities.GCandidature.DossierCandidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.DossierCandidatureService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DossierCandidatureBackofficeController implements Initializable {

    @FXML private TableView<DossierCandidature>                  dossierTable;
    @FXML private TableColumn<DossierCandidature, String>        nomCandidatureColumn;
    @FXML private TableColumn<DossierCandidature, String>        nomDossierColumn;
    @FXML private TableColumn<DossierCandidature, String>        descriptionColumn;
    @FXML private TableColumn<DossierCandidature, String>        businessPlanColumn;
    @FXML private TableColumn<DossierCandidature, java.sql.Date> dateCreationColumn;
    @FXML private TableColumn<DossierCandidature, String>        etatColumn;
    @FXML private Button backButton;

    // ✨ NOUVEAU — bouton Analyser IA
    @FXML private Button btnAnalyserIA;

    private final DossierCandidatureService service = new DossierCandidatureService();
    private final ObservableList<DossierCandidature> dossierList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        nomDossierColumn    .setCellValueFactory(new PropertyValueFactory<>("nomDossier"));
        descriptionColumn   .setCellValueFactory(new PropertyValueFactory<>("descriptionProjet"));
        businessPlanColumn  .setCellValueFactory(new PropertyValueFactory<>("businessPlan"));
        dateCreationColumn  .setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        etatColumn          .setCellValueFactory(new PropertyValueFactory<>("etat"));
        dossierTable.setItems(dossierList);
        refreshTable();

        // Désactiver le bouton IA si aucune sélection
        if (btnAnalyserIA != null) {
            btnAnalyserIA.setDisable(true);
            dossierTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, selected) -> btnAnalyserIA.setDisable(selected == null));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🤖 ANALYSE IA — NOUVEAU
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void analyserAvecIA() {
        DossierCandidature selected = dossierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner un dossier à analyser.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AnalyseIAPanel.fxml"));
            Parent root = loader.load();
            AnalyseIAPanelController iaCtrl = loader.getController();

            // Passer les données du dossier sélectionné à l'IA
            iaCtrl.setContexte(
                    selected.getNomCandidature(),   // nom candidature (startup)
                    selected.getDescriptionProjet(), // description du projet
                    selected.getBusinessPlan()       // business plan (chemin ou URL)
            );

            // Ouvrir en modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(dossierTable.getScene().getWindow());
            stage.setTitle("🤖 Analyse IA  —  " + selected.getNomDossier());
            stage.setScene(new Scene(root));
            stage.setMinWidth(700);
            stage.setMinHeight(560);
            stage.show();

        } catch (Exception e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur IA",
                    "Impossible d'ouvrir le panneau IA", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EXISTANT — INCHANGÉ
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void hideSelected() {
        DossierCandidature selected = dossierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner un dossier à cacher.");
            return;
        }
        try {
            service.hideDossier(selected.getIdDossier());
            AlertUtils.showInfo("Succès", "Dossier caché avec succès");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de cacher le dossier", e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        dossierList.clear();
        try {
            dossierList.addAll(service.getAllDossiers(true));
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les dossiers", e.getMessage());
        }
    }

    // ── Navigation sidebar (inchangé) ────────────────────────────

    @FXML private void goToCandidatures() {
        openView("/fxml/CandidatureBackofficeView.fxml", "Candidatures");
    }

    @FXML private void goToDossiers() { /* déjà ici */ }

    @FXML private void goToEvaluations() {
        openView("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
    }

    @FXML private void goBackToMenu() {
        ((Stage) dossierTable.getScene().getWindow()).close();
    }

    private void openView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            HBox root = loader.load();
            Scene scene = new Scene(root, 1200, 750);
            loadCss(scene);
            Stage stage = new Stage();
            stage.setTitle("BOOSTUP — Back Office — " + title);
            stage.setScene(scene);
            stage.setMinWidth(900); stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();
            ((Stage) dossierTable.getScene().getWindow()).close();
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la vue", e.getMessage());
        }
    }

    private void loadCss(Scene scene) {
        for (String p : new String[]{"/style.css", "/css/style.css"}) {
            URL u = getClass().getResource(p);
            if (u != null) { scene.getStylesheets().add(u.toExternalForm()); return; }
        }
    }
}