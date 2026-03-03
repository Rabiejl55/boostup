package controllers;

import entities.GCandidature.Candidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
    @FXML private TableColumn<Candidature, String>        emailContactColumn;
    @FXML private Button                                  backButton;

    // ── Recherche & compteur ──────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     lblResultCount;

    private final CandidatureService          service      = new CandidatureService();
    private final ObservableList<Candidature> masterList   = FXCollections.observableArrayList();
    private       FilteredList<Candidature>   filteredList;
    private       SortedList<Candidature>     sortedList;

    public CandidatureBackofficeController() throws SQLException {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // 1. Cell value factories
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        nomStartupColumn    .setCellValueFactory(new PropertyValueFactory<>("nomStartup"));
        emailContactColumn  .setCellValueFactory(new PropertyValueFactory<>("emailContact"));
        dateDepotColumn     .setCellValueFactory(new PropertyValueFactory<>("dateDepot"));
        statutColumn        .setCellValueFactory(new PropertyValueFactory<>("statut"));
        scoreColumn         .setCellValueFactory(new PropertyValueFactory<>("score"));
        commentaireColumn   .setCellValueFactory(new PropertyValueFactory<>("commentaire"));

        // 2. Rendu coloré de la colonne Statut
        statutColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                String color = switch (val.toUpperCase()) {
                    case "VALIDEE"    -> "#10b981";
                    case "REFUSEE"    -> "#ef4444";
                    case "EN_ATTENTE" -> "#f59e0b";
                    default           -> "#6b7494";
                };
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:700;");
            }
        });

        // 3. Pipeline FilteredList → SortedList → TableView
        filteredList = new FilteredList<>(masterList, p -> true);
        sortedList   = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(candidatureTable.comparatorProperty());
        candidatureTable.setItems(sortedList);

        // 4. Toutes les colonnes triables
        nomCandidatureColumn.setSortable(true);
        nomStartupColumn    .setSortable(true);
        emailContactColumn  .setSortable(true);
        dateDepotColumn     .setSortable(true);
        statutColumn        .setSortable(true);
        scoreColumn         .setSortable(true);
        commentaireColumn   .setSortable(true);

        // 5. Listener recherche temps réel
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));

        refreshTable();
    }

    // ── Filtrage ──────────────────────────────────────────────────

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredList.setPredicate(c -> {
            if (q.isEmpty()) return true;
            return nvl(c.getNomCandidature()).toLowerCase().contains(q)
                    || nvl(c.getNomStartup())     .toLowerCase().contains(q)
                    || nvl(c.getEmailContact())   .toLowerCase().contains(q)
                    || nvl(c.getStatut())         .toLowerCase().contains(q)
                    || nvl(c.getCommentaire())    .toLowerCase().contains(q);
        });
        updateCount();
    }

    private void updateCount() {
        if (lblResultCount == null) return;
        int shown = filteredList.size(), total = masterList.size();
        lblResultCount.setText(shown == total
                ? total + " candidature" + (total > 1 ? "s" : "")
                : shown + " / " + total + " candidature" + (total > 1 ? "s" : ""));
    }

    // ── Masquer ───────────────────────────────────────────────────

    @FXML
    private void hideSelected() {
        Candidature sel = candidatureTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner une candidature à cacher.");
            return;
        }
        try {
            service.hideCandidature(sel.getIdCandidature());
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
        masterList.clear();
        try { masterList.addAll(service.getAllCandidatures(true)); }
        catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les candidatures", e.getMessage());
        }
        if (searchField != null) applyFilter(searchField.getText());
        else updateCount();
    }

    // ── Similarités ───────────────────────────────────────────────

    @FXML
    private void ouvrirSimilarites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SimilaritePanel.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(candidatureTable.getScene().getWindow());
            stage.setTitle("🔍 Détection de similarités — BOOSTUP Admin");
            stage.setScene(new Scene(root));
            stage.setMinWidth(780); stage.setMinHeight(680);
            stage.setResizable(true); stage.centerOnScreen(); stage.show();
        } catch (Exception e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le panneau", e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────

    @FXML private void goToDashboard()    { openView("/fxml/DashboardBackofficeView.fxml",           "Dashboard");    }
    @FXML private void goToCandidatures() { /* déjà ici */ }
    @FXML private void goToDossiers()     { openView("/fxml/DossierCandidatureBackofficeView.fxml",  "Dossiers");     }
    @FXML private void goToEvaluations()  { openView("/fxml/EvaluationBackofficeView.fxml",          "Évaluations"); }

    private void openView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            HBox root = loader.load();
            Scene scene = new Scene(root, 1200, 750);
            loadCss(scene);
            Stage stage = new Stage();
            stage.setTitle("BOOSTUP — Back Office — " + title);
            stage.setScene(scene); stage.setMinWidth(900); stage.setMinHeight(600);
            stage.centerOnScreen(); stage.show();
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

    private String nvl(String s) { return s != null ? s : ""; }
}