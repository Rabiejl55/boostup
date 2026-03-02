package controllers;

import entities.GCandidature.Evaluation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.EvaluationPdfService;
import services.CandidatureService.EvaluationService;
import utils.AlertUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EvaluationBackofficeController implements Initializable {

    @FXML private TableView<Evaluation>            evaluationTable;
    @FXML private TableColumn<Evaluation, String>  nomCandidatureColumn;
    @FXML private TableColumn<Evaluation, Integer> noteInnovationColumn;
    @FXML private TableColumn<Evaluation, Integer> noteViabiliteColumn;
    @FXML private TableColumn<Evaluation, Integer> noteMarcheColumn;
    @FXML private TableColumn<Evaluation, Integer> noteEquipeColumn;
    @FXML private TableColumn<Evaluation, Double>  noteGlobaleColumn;
    @FXML private TableColumn<Evaluation, String>  decisionColumn;
    @FXML private Button                           btnExportPdf;
    @FXML private Button                           backButton;

    private final EvaluationService    service    = new EvaluationService();
    private final EvaluationPdfService pdfService = new EvaluationPdfService();
    private final ObservableList<Evaluation> evaluationList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        noteInnovationColumn.setCellValueFactory(new PropertyValueFactory<>("noteInnovation"));
        noteViabiliteColumn .setCellValueFactory(new PropertyValueFactory<>("noteViabilite"));
        noteMarcheColumn    .setCellValueFactory(new PropertyValueFactory<>("noteMarche"));
        noteEquipeColumn    .setCellValueFactory(new PropertyValueFactory<>("noteEquipe"));
        noteGlobaleColumn   .setCellValueFactory(new PropertyValueFactory<>("noteGlobale"));
        decisionColumn      .setCellValueFactory(new PropertyValueFactory<>("decision"));
        evaluationTable.setItems(evaluationList);

        if (btnExportPdf != null) {
            btnExportPdf.setDisable(true);
            evaluationTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> btnExportPdf.setDisable(sel == null));
        }
        refreshTable();
    }

    @FXML private void openAddForm()  { openForm(null); }

    @FXML private void openEditForm() {
        Evaluation sel = evaluationTable.getSelectionModel().getSelectedItem();
        if (sel != null) openForm(sel);
        else AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                null, "Veuillez sélectionner une évaluation à modifier.");
    }

    private void openForm(Evaluation evaluation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EvaluationForm.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            loadCss(scene);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setTitle(evaluation == null ? "Ajouter une Évaluation" : "Modifier l'Évaluation");
            EvaluationFormController ctrl = loader.getController();
            ctrl.setEvaluation(evaluation);
            ctrl.setParentController(this);
            stage.showAndWait();
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire", e.getMessage());
        }
    }

    // ── Export PDF ────────────────────────────────────────────────

    @FXML
    private void exporterPdf() {
        Evaluation selected = evaluationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner une évaluation à exporter.");
            return;
        }

        if (btnExportPdf != null) {
            btnExportPdf.setDisable(true);
            btnExportPdf.setText("⏳  Génération…");
        }

        Thread t = new Thread(() -> {
            try {
                EvaluationPdfService.RapportInfo info = pdfService.genererRapport(selected);
                Platform.runLater(() -> {
                    if (btnExportPdf != null) {
                        btnExportPdf.setDisable(false);
                        btnExportPdf.setText("📄  Exporter PDF");
                    }
                    afficherDialogueSucces(info);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnExportPdf != null) {
                        btnExportPdf.setDisable(false);
                        btnExportPdf.setText("📄  Exporter PDF");
                    }
                    AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur PDF",
                            "Impossible de générer le rapport", e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Boîte de dialogue enrichie ────────────────────────────────

    private void afficherDialogueSucces(EvaluationPdfService.RapportInfo info) {
        boolean acc = "ACCEPTEE".equalsIgnoreCase(info.decision)
                || "VALIDEE".equalsIgnoreCase(info.decision);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(evaluationTable.getScene().getWindow());
        dialog.setTitle("Rapport PDF généré");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f7f8fc;");
        root.setPrefWidth(480);

        // ── Header navy ──────────────────────────────────────────
        HBox header = new HBox(14);
        header.setStyle("-fx-background-color: #0a0f1e; -fx-padding: 22 28;");
        header.setAlignment(Pos.CENTER_LEFT);

        // Icône colorée selon décision
        Label iconLbl = new Label("📄");
        iconLbl.setStyle("-fx-font-size: 26px;");

        VBox headerTexts = new VBox(4);
        Label titleLbl = new Label("Rapport généré avec succès");
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: white;");
        Label filenameLbl = new Label(info.nomFichier);
        filenameLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(150,180,255,0.6);");
        headerTexts.getChildren().addAll(titleLbl, filenameLbl);
        header.getChildren().addAll(iconLbl, headerTexts);

        // ── Badge décision ────────────────────────────────────────
        HBox badgeBar = new HBox();
        badgeBar.setStyle("-fx-background-color: " + (acc ? "#e8faf2" : "#fef2f2")
                + "; -fx-padding: 10 28;");
        badgeBar.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(acc ? "✅  CANDIDATURE ACCEPTÉE" : "❌  CANDIDATURE REFUSÉE");
        badge.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: "
                + (acc ? "#065f46" : "#991b1b") + ";");
        Label scoreLabel = new Label(String.format("   ·   %.2f / 10", info.score));
        scoreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e2746;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        badgeBar.getChildren().addAll(badge, scoreLabel, sp);

        // ── Grille d'infos ────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setStyle("-fx-padding: 22 28; -fx-hgap: 16; -fx-vgap: 14;"
                + "-fx-background-color: white;");
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(150);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPrefWidth(270);
        grid.getColumnConstraints().addAll(c1, c2);

        addGridRow(grid, 0, "Candidature",       info.candidature,           "#1e2746");
        addGridRow(grid, 1, "Taille du fichier", info.tailleFichierKo + " Ko", "#1e2746");
        addGridRow(grid, 2, "Généré le",         info.dateGeneration,        "#1e2746");
        addGridRow(grid, 3, "Dossier de sauvegarde",
                System.getProperty("user.home") + "/Documents/BOOSTUP_Rapports/",
                "#4f62e8");

        // ── Séparateur ────────────────────────────────────────────
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #e8ecf0; -fx-pref-height: 1; -fx-max-height: 1;");

        // ── Boutons ───────────────────────────────────────────────
        HBox btnBar = new HBox(10);
        btnBar.setStyle("-fx-padding: 16 28; -fx-background-color: #f7f8fc;");
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnDossier = new Button("📁  Ouvrir le dossier");
        btnDossier.setStyle(
                "-fx-background-color: #f7f8fc; -fx-text-fill: #4f62e8;"
                        + "-fx-font-weight: 700; -fx-font-size: 12px;"
                        + "-fx-padding: 9 18; -fx-background-radius: 9px;"
                        + "-fx-border-color: #dce0f0; -fx-border-radius: 9px;"
                        + "-fx-border-width: 1.5px; -fx-cursor: hand;");
        btnDossier.setOnAction(e -> ouvrirFichier(new File(info.chemin).getParent()));

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color: #e8ecf0; -fx-text-fill: #1e2746;"
                        + "-fx-font-weight: 600; -fx-font-size: 12px;"
                        + "-fx-padding: 9 18; -fx-background-radius: 9px; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> dialog.close());

        Button btnOuvrir = new Button("📂  Ouvrir le PDF");
        btnOuvrir.setStyle(
                "-fx-background-color: linear-gradient(to right, #4f62e8, #7c3aed);"
                        + "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;"
                        + "-fx-padding: 9 18; -fx-background-radius: 9px; -fx-cursor: hand;");
        btnOuvrir.setOnAction(e -> { dialog.close(); ouvrirFichier(info.chemin); });

        btnBar.getChildren().addAll(btnDossier, btnFermer, btnOuvrir);

        root.getChildren().addAll(header, badgeBar, grid, sep, btnBar);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void addGridRow(GridPane g, int row, String label, String value, String color) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: "
                + color + "; -fx-wrap-text: true;");
        val.setWrapText(true);
        val.setMaxWidth(270);
        GridPane.setConstraints(lbl, 0, row);
        GridPane.setConstraints(val, 1, row);
        g.getChildren().addAll(lbl, val);
    }

    private void ouvrirFichier(String chemin) {
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().open(new File(chemin));
        } catch (Exception e) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Ouverture impossible",
                    null, "Fichier ici :\n" + chemin);
        }
    }

    // ── Cacher ────────────────────────────────────────────────────

    @FXML private void hideSelected() {
        Evaluation sel = evaluationTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner une évaluation à cacher.");
            return;
        }
        try {
            service.hideEvaluation(sel.getIdEvaluation());
            AlertUtils.showInfo("Succès", "Évaluation cachée avec succès");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de cacher l'évaluation", e.getMessage());
        }
    }

    @FXML public void refreshTable() {
        evaluationList.clear();
        try { evaluationList.addAll(service.getAllEvaluations(true)); }
        catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les évaluations", e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────
    @FXML private void goToDashboard()    { openView("/fxml/DashboardBackofficeView.fxml",           "Dashboard");    }
    @FXML private void goToCandidatures() {
        openView("/fxml/CandidatureBackofficeView.fxml", "Candidatures"); }
    @FXML private void goToDossiers() {
        openView("/fxml/DossierCandidatureBackofficeView.fxml", "Dossiers"); }
    @FXML private void goToEvaluations() { /* déjà ici */ }
    @FXML private void goBackToMenu() {
        ((Stage) evaluationTable.getScene().getWindow()).close(); }

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
            stage.centerOnScreen(); stage.show();
            ((Stage) evaluationTable.getScene().getWindow()).close();
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