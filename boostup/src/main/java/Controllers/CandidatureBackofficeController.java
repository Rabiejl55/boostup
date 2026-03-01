package Controllers;

import entities.GCandidature.Candidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CandidatureBackofficeController implements Initializable {

    @FXML private TableView<Candidature>                  candidatureTable;
    @FXML private TableColumn<Candidature, String>        nomCandidatureColumn;
    @FXML private TableColumn<Candidature, String>        nomStartupColumn;
    @FXML private TableColumn<Candidature, java.sql.Date> dateDepotColumn;
    @FXML private TableColumn<Candidature, String>        statutColumn;
    @FXML private TableColumn<Candidature, Double>        scoreColumn;
    @FXML private TableColumn<Candidature, String>        commentaireColumn;
    @FXML private TableColumn<Candidature, String>        emailContactColumn; // ✅ NOUVEAU
    @FXML private Button backButton;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<Candidature> candidatureList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        nomStartupColumn    .setCellValueFactory(new PropertyValueFactory<>("nomStartup"));
        dateDepotColumn     .setCellValueFactory(new PropertyValueFactory<>("dateDepot"));
        statutColumn        .setCellValueFactory(new PropertyValueFactory<>("statut"));
        scoreColumn         .setCellValueFactory(new PropertyValueFactory<>("score"));
        commentaireColumn   .setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        emailContactColumn  .setCellValueFactory(new PropertyValueFactory<>("emailContact")); // ✅
        candidatureTable.setItems(candidatureList);
        refreshTable();
    }

    // ── Masquer ───────────────────────────────────────────────────────────────

    @FXML
    private void hideSelected() {
        Candidature selected = candidatureTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner une candidature à cacher.");
            return;
        }
        try {
            service.hideCandidature(selected.getIdCandidature());
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Succès",
                    null, "Candidature cachée avec succès.");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de cacher la candidature", e.getMessage());
        }
    }

    @FXML
    public void refreshTable() {
        candidatureList.clear();
        try {
            candidatureList.addAll(service.getAllCandidatures(true));
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les candidatures", e.getMessage());
        }
    }

    // ── Navigation sidebar ────────────────────────────────────────────────────

    @FXML private void goToCandidatures() {}

    @FXML private void goToDossiers() {
        openView("/fxml/DossierCandidatureBackofficeView.fxml", "Dossiers");
    }

    @FXML private void goToEvaluations() {
        openView("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
    }

    @FXML private void goBackToMenu() {
        ((Stage) candidatureTable.getScene().getWindow()).close();
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
            ((Stage) candidatureTable.getScene().getWindow()).close();
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