package controllers;

import entities.GCandidature.DossierCandidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.DossierCandidatureService;
import utils.AlertUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DossierCandidatureBackofficeController implements Initializable {

    // ── FXML — nouveau layout ─────────────────────────────────────
    @FXML private VBox   dossierListContainer; // conteneur des cartes de liste
    @FXML private VBox   emptyState;           // panneau "rien sélectionné"
    @FXML private VBox   detailContent;        // panneau détail
    @FXML private VBox   detailHeader;

    // Labels détail
    @FXML private Label    detailNomDossier;
    @FXML private Label    detailNomCandidature;
    @FXML private Label    detailBadgeEtat;
    @FXML private Label    detailDateCreation;
    @FXML private TextArea detailDescription;
    @FXML private Label    detailBusinessPlan;
    @FXML private Label    lblCount;

    // Boutons footer
    @FXML private Button btnAnalyserIA;
    @FXML private Button hideButton;

    // Recherche
    @FXML private TextField searchField;

    // ── Tri ───────────────────────────────────────────────────────
    // critère : "nom" | "date" | "etat"  ;  ordre : asc/desc
    private String  sortCritere = "nom";
    private boolean sortAsc     = true;

    // ── État interne ──────────────────────────────────────────────
    private final DossierCandidatureService service = new DossierCandidatureService();
    private final ObservableList<DossierCandidature> allDossiers = FXCollections.observableArrayList();
    private DossierCandidature selectedDossier = null;

    // ── Couleurs par état ─────────────────────────────────────────
    private static final String COLOR_COMPLET   = "#10b981";
    private static final String COLOR_INCOMPLET = "#f59e0b";
    private static final String COLOR_DEFAULT   = "#6b7494";

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Recherche en temps réel
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> filterAndRender(val));
        }
        showDetail(null); // état vide au départ
        refreshTable();
    }

    // ══════════════════════════════════════════════════════════════
    //  CHARGEMENT & RENDU
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void refreshTable() {
        allDossiers.clear();
        try {
            allDossiers.addAll(service.getAllDossiers(true));
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les dossiers", e.getMessage());
        }
        filterAndRender(searchField != null ? searchField.getText() : "");
    }

    private void filterAndRender(String query) {
        // ── Filtrage ──────────────────────────────────────────────
        List<DossierCandidature> filtered = allDossiers.stream()
                .filter(d -> query == null || query.isBlank()
                        || d.getNomDossier().toLowerCase().contains(query.toLowerCase())
                        || d.getNomCandidature().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        // ── Tri ───────────────────────────────────────────────────
        Comparator<DossierCandidature> cmp = switch (sortCritere) {
            case "date" -> Comparator.comparing(
                    d -> d.getDateCreation() != null ? d.getDateCreation() : new java.sql.Date(0));
            case "etat" -> Comparator.comparing(
                    d -> d.getEtat() != null ? d.getEtat() : "");
            default     -> Comparator.comparing(
                    d -> d.getNomDossier() != null ? d.getNomDossier().toLowerCase() : "");
        };
        if (!sortAsc) cmp = cmp.reversed();
        filtered.sort(cmp);

        // ── Compteur ──────────────────────────────────────────────
        if (lblCount != null) {
            int shown = filtered.size(), total = allDossiers.size();
            lblCount.setText(shown == total
                    ? total + " dossier" + (total > 1 ? "s" : "")
                    : shown + " / " + total + " dossier" + (total > 1 ? "s" : ""));
        }

        dossierListContainer.getChildren().clear();
        for (DossierCandidature d : filtered) {
            dossierListContainer.getChildren().add(buildListRow(d));
        }

        // Si le dossier sélectionné n'est plus dans la liste filtrée, on réinitialise
        if (selectedDossier != null &&
                filtered.stream().noneMatch(d -> d.getIdDossier() == selectedDossier.getIdDossier())) {
            showDetail(null);
        }
    }

    // ── Méthodes de tri appelées depuis le FXML ───────────────────

    @FXML
    private void trierParNom() {
        if ("nom".equals(sortCritere)) sortAsc = !sortAsc;
        else { sortCritere = "nom"; sortAsc = true; }
        filterAndRender(searchField != null ? searchField.getText() : "");
    }

    @FXML
    private void trierParDate() {
        if ("date".equals(sortCritere)) sortAsc = !sortAsc;
        else { sortCritere = "date"; sortAsc = false; } // desc par défaut (plus récent en premier)
        filterAndRender(searchField != null ? searchField.getText() : "");
    }

    @FXML
    private void trierParEtat() {
        if ("etat".equals(sortCritere)) sortAsc = !sortAsc;
        else { sortCritere = "etat"; sortAsc = true; }
        filterAndRender(searchField != null ? searchField.getText() : "");
    }

    // ── Construit une ligne/carte dans la liste gauche ──────────
    private HBox buildListRow(DossierCandidature d) {
        // Icône état
        String etat = d.getEtat() != null ? d.getEtat().toUpperCase() : "";
        String dotColor = etat.contains("COMPL") ? COLOR_COMPLET
                : etat.contains("INC")  ? COLOR_INCOMPLET
                : COLOR_DEFAULT;

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size:10px; -fx-text-fill:" + dotColor + ";");

        // Nom du dossier
        Label nomDossier = new Label(d.getNomDossier());
        nomDossier.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#1e2746;");
        nomDossier.setMaxWidth(Double.MAX_VALUE);

        // Nom candidature (sous-titre)
        Label nomCandidature = new Label(d.getNomCandidature());
        nomCandidature.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7494;");

        VBox texts = new VBox(2, nomDossier, nomCandidature);
        HBox.setHgrow(texts, Priority.ALWAYS);

        // Badge état compact
        Label badgeEtat = new Label(d.getEtat() != null ? d.getEtat() : "—");
        badgeEtat.setStyle(buildBadgeStyle(d.getEtat()));

        HBox row = new HBox(10, dot, texts, badgeEtat);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;");

        // Hover + sélection
        row.setOnMouseEntered(e -> {
            if (selectedDossier == null || selectedDossier.getIdDossier() != d.getIdDossier())
                row.setStyle("-fx-background-color:#f0f4ff; -fx-cursor:hand;");
        });
        row.setOnMouseExited(e -> {
            if (selectedDossier == null || selectedDossier.getIdDossier() != d.getIdDossier())
                row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;");
        });
        row.setOnMouseClicked(e -> {
            // Déselectionner visuellement toutes les lignes
            dossierListContainer.getChildren().forEach(node ->
                    node.setStyle("-fx-background-color:transparent; -fx-cursor:hand;"));
            // Sélectionner
            row.setStyle("-fx-background-color:#eef2ff; -fx-border-color:transparent transparent transparent #4f62e8; -fx-border-width:0 0 0 3; -fx-cursor:hand;");
            showDetail(d);
        });

        // Marquer si déjà sélectionné
        if (selectedDossier != null && selectedDossier.getIdDossier() == d.getIdDossier()) {
            row.setStyle("-fx-background-color:#eef2ff; -fx-border-color:transparent transparent transparent #4f62e8; -fx-border-width:0 0 0 3; -fx-cursor:hand;");
        }

        return row;
    }

    // ── Affiche le panneau détail ─────────────────────────────────
    private void showDetail(DossierCandidature d) {
        selectedDossier = d;

        if (d == null) {
            emptyState.setVisible(true);  emptyState.setManaged(true);
            detailContent.setVisible(false); detailContent.setManaged(false);
            return;
        }

        emptyState.setVisible(false);  emptyState.setManaged(false);
        detailContent.setVisible(true); detailContent.setManaged(true);

        // Remplir les champs
        detailNomDossier.setText(d.getNomDossier());
        detailNomCandidature.setText("📋  " + d.getNomCandidature());
        detailDateCreation.setText(d.getDateCreation() != null
                ? "Créé le " + d.getDateCreation().toString() : "Date inconnue");
        detailDescription.setText(d.getDescriptionProjet() != null
                ? d.getDescriptionProjet() : "Aucune description disponible.");
        detailBusinessPlan.setText(d.getBusinessPlan() != null
                ? d.getBusinessPlan() : "Aucun business plan renseigné.");

        // ✅ Clic pour ouvrir le fichier/URL
        String bp = d.getBusinessPlan();
        if (bp != null && !bp.isBlank()) {
            detailBusinessPlan.setStyle(
                    "-fx-font-size:12px; -fx-text-fill:#4f62e8; -fx-font-weight:600; -fx-cursor:hand; -fx-underline:true;");
            detailBusinessPlan.setOnMouseClicked(e -> openBusinessPlan(bp));
        } else {
            detailBusinessPlan.setStyle(
                    "-fx-font-size:12px; -fx-text-fill:#9ca3af; -fx-font-weight:400;");
            detailBusinessPlan.setOnMouseClicked(null);
        }

        // Badge état dans le header
        String etat = d.getEtat() != null ? d.getEtat() : "—";
        detailBadgeEtat.setText(etat);
        detailBadgeEtat.setStyle(buildBadgeStyle(etat));

        // Couleur du header selon état
        String gradientHeader = etat.toUpperCase().contains("COMPL")
                ? "linear-gradient(to right, #e8faf2, #f7fffe)"
                : etat.toUpperCase().contains("INC")
                ? "linear-gradient(to right, #fff8e6, #fffdf7)"
                : "linear-gradient(to right, #f7f8fc, white)";
        detailHeader.setStyle("-fx-background-color:" + gradientHeader + "; -fx-padding:28 32 22 32;");
    }

    // ── Génère le style CSS inline du badge ──────────────────────
    private String buildBadgeStyle(String etat) {
        if (etat == null) etat = "";
        String upper = etat.toUpperCase();
        if (upper.contains("COMPL")) {
            return "-fx-background-color:#e8faf2; -fx-border-color:#10b981; -fx-text-fill:#0d7a57;"
                    + "-fx-background-radius:20; -fx-border-radius:20; -fx-border-width:1.5;"
                    + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:4 10;";
        } else if (upper.contains("INC")) {
            return "-fx-background-color:#fff8e6; -fx-border-color:#f59e0b; -fx-text-fill:#92610a;"
                    + "-fx-background-radius:20; -fx-border-radius:20; -fx-border-width:1.5;"
                    + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:4 10;";
        } else {
            return "-fx-background-color:#eef0f8; -fx-border-color:#dce0f0; -fx-text-fill:#6b7494;"
                    + "-fx-background-radius:20; -fx-border-radius:20; -fx-border-width:1.5;"
                    + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:4 10;";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void analyserAvecIA() {
        if (selectedDossier == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner un dossier à analyser.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AnalyseIAPanel.fxml"));
            Parent root = loader.load();
            AnalyseIAPanelController iaCtrl = loader.getController();
            iaCtrl.setContexte(
                    selectedDossier.getNomCandidature(),
                    selectedDossier.getDescriptionProjet(),
                    selectedDossier.getBusinessPlan()
            );
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(dossierListContainer.getScene().getWindow());
            stage.setTitle("🤖 Analyse IA  —  " + selectedDossier.getNomDossier());
            stage.setScene(new Scene(root));
            stage.setMinWidth(700);
            stage.setMinHeight(560);
            stage.show();
        } catch (Exception e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur IA",
                    "Impossible d'ouvrir le panneau IA", e.getMessage());
        }
    }

    // ── Ouvre le business plan (fichier local ou URL) ─────────────
    private void openBusinessPlan(String path) {
        if (path == null || path.isBlank()) return;

        try {
            if (!Desktop.isDesktopSupported()) {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "Non supporté",
                        null, "L'ouverture de fichiers n'est pas supportée sur ce système.");
                return;
            }
            Desktop desktop = Desktop.getDesktop();

            // ── URL web (http / https) ──
            if (path.startsWith("http://") || path.startsWith("https://")) {
                desktop.browse(new URI(path));
                return;
            }

            // ── Fichier local ──
            File file = new File(path);
            if (!file.exists()) {
                // Tentative avec décodage (espaces %20, etc.)
                file = new File(new URI(path.startsWith("file:") ? path : "file:///" + path.replace("\\", "/")));
            }
            if (file.exists()) {
                desktop.open(file);
            } else {
                AlertUtils.showAlert(Alert.AlertType.WARNING, "Fichier introuvable",
                        null, "Le fichier n'existe pas ou a été déplacé :\n" + path);
            }

        } catch (Exception ex) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur d'ouverture",
                    "Impossible d'ouvrir le business plan", ex.getMessage());
        }
    }

    @FXML
    private void hideSelected() {
        if (selectedDossier == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    null, "Veuillez sélectionner un dossier à masquer.");
            return;
        }
        try {
            service.hideDossier(selectedDossier.getIdDossier());
            AlertUtils.showInfo("Succès", "Dossier masqué avec succès.");
            selectedDossier = null;
            refreshTable();
            showDetail(null);
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de masquer le dossier", e.getMessage());
        }
    }

    // ── Navigation sidebar ────────────────────────────────────────

    @FXML private void goToDashboard() {
        openView("/fxml/DashboardBackofficeView.fxml", "Dashboard");
    }

    @FXML private void goToCandidatures() {
        openView("/fxml/CandidatureBackofficeView.fxml", "Candidatures");
    }
    @FXML private void goToDossiers() { /* déjà ici */ }
    @FXML private void goToEvaluations() {
        openView("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
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
            ((Stage) dossierListContainer.getScene().getWindow()).close();
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