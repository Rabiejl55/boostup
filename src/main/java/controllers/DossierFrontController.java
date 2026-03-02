package controllers;

import entities.GCandidature.DossierCandidature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.DossierCandidatureService;

import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DossierFrontController implements Initializable {

    @FXML private FlowPane  cardsFlowPane;
    @FXML private TextField tfSearch;
    @FXML private Label     lblComplets;
    @FXML private Label     lblIncomplets;

    private final DossierCandidatureService service = new DossierCandidatureService();
    private List<DossierCandidature>        all;
    private AppController                   appController;

    public void setAppController(AppController app) { this.appController = app; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        loadAll();
    }

    public void loadAll() {
        try { all = service.getAllDossiers(true); }
        catch (SQLException ex) { all = List.of(); alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        updateStats();
        applyFilters();
        if (appController != null) appController.refreshSidebarStats();
    }

    private void updateStats() {
        if (all == null) return;
        lblComplets  .setText(String.valueOf(all.stream().filter(d -> "COMPLET"  .equals(d.getEtat())).count()));
        lblIncomplets.setText(String.valueOf(all.stream().filter(d -> "INCOMPLET".equals(d.getEtat())).count()));
    }

    private void applyFilters() {
        if (all == null) return;
        String s = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        List<DossierCandidature> filtered = all.stream().filter(d ->
                s.isEmpty()
                        || (d.getNomDossier()     != null && d.getNomDossier().toLowerCase().contains(s))
                        || (d.getNomCandidature() != null && d.getNomCandidature().toLowerCase().contains(s))
        ).collect(Collectors.toList());

        cardsFlowPane.getChildren().clear();
        if (filtered.isEmpty()) cardsFlowPane.getChildren().add(buildEmpty());
        else filtered.forEach(d -> cardsFlowPane.getChildren().add(buildCard(d)));
    }

    // ─── Carte ───────────────────────────────────────────────────────────────
    private VBox buildCard(DossierCandidature d) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPrefWidth(400);

        // Header
        String etatCls = "COMPLET".equals(d.getEtat()) ? "validee" : "attente";
        HBox header = new HBox(12);
        header.getStyleClass().addAll("card-header", "card-header-" + etatCls);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(5);
        HBox.setHgrow(texts, Priority.ALWAYS);

        TextField tfNom = new TextField(nvl(d.getNomDossier()));
        tfNom.getStyleClass().add("card-tf-title");

        Label lblCand = new Label("📋  " + nvl(d.getNomCandidature()));
        lblCand.getStyleClass().add("card-tf-sub-ro");

        texts.getChildren().addAll(tfNom, lblCand);

        ComboBox<String> cbEtat = new ComboBox<>();
        cbEtat.getItems().addAll("COMPLET", "INCOMPLET");
        cbEtat.setValue(d.getEtat());
        cbEtat.getStyleClass().addAll("badge-combo", "badge-combo-" + etatCls);

        header.getChildren().addAll(texts, cbEtat);
        card.getChildren().add(header);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 12, 18));
        body.getStyleClass().add("card-body");

        // Date création
        DatePicker dp = new DatePicker(d.getDateCreation() != null
                ? d.getDateCreation().toLocalDate() : LocalDate.now());
        dp.getStyleClass().add("card-dp");
        dp.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(row("📅", "Date création", dp));

        // ── Business Plan avec champ + bouton Parcourir + bouton Ouvrir ──────
        TextField tfBp = new TextField(nvl(d.getBusinessPlan()));
        tfBp.getStyleClass().add("card-tf-inline");
        tfBp.setPromptText("Chemin fichier ou URL…");
        HBox.setHgrow(tfBp, Priority.ALWAYS);

        // Bouton Parcourir (choisir un fichier)
        Button btnBrowse = new Button("📎");
        btnBrowse.setTooltip(new Tooltip("Choisir un fichier (PDF, DOCX…)"));
        btnBrowse.setStyle("-fx-background-color:#6366f1; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand; -fx-font-size:14px;");
        btnBrowse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir le Business Plan");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF",         "*.pdf"),
                    new FileChooser.ExtensionFilter("Word",        "*.docx", "*.doc"),
                    new FileChooser.ExtensionFilter("PowerPoint",  "*.pptx"),
                    new FileChooser.ExtensionFilter("Tous",        "*.*")
            );
            // Pré-ouvrir dans le dossier du fichier actuel si possible
            String current = tfBp.getText();
            if (current != null && !current.isEmpty()) {
                File currentFile = new File(current);
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists())
                    fc.setInitialDirectory(currentFile.getParentFile());
            }
            Stage stage = (Stage) card.getScene().getWindow();
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                tfBp.setText(file.getAbsolutePath());
                tfBp.setStyle("-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
            }
        });

        // Bouton Ouvrir (ouvrir le fichier/URL avec l'application système)
        Button btnOpen = new Button("🔗");
        btnOpen.setTooltip(new Tooltip("Ouvrir le fichier ou l'URL"));
        btnOpen.setStyle("-fx-background-color:#0ea5e9; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand; -fx-font-size:14px;");
        btnOpen.setOnAction(e -> openBp(tfBp.getText()));

        HBox bpRow = new HBox(6, tfBp, btnBrowse, btnOpen);
        bpRow.setAlignment(Pos.CENTER_LEFT);

        Label lbBp = new Label("📎  Business Plan :");
        lbBp.getStyleClass().add("row-key");
        lbBp.setPrefWidth(130);

        HBox bpFullRow = new HBox(10, lbBp, bpRow);
        bpFullRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bpRow, Priority.ALWAYS);
        body.getChildren().add(bpFullRow);

        // Validation live du champ BP (URL ou fichier)
        tfBp.textProperty().addListener((obs, old, val) -> validateBpLive(tfBp, val));

        // Description
        Label lbDesc = new Label("📝  Description du projet");
        lbDesc.getStyleClass().add("row-key");
        TextArea ta = new TextArea(nvl(d.getDescriptionProjet()));
        ta.setWrapText(true); ta.setPrefRowCount(3);
        ta.getStyleClass().add("card-ta");
        body.getChildren().addAll(lbDesc, ta);
        card.getChildren().add(body);

        // Footer
        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 18, 14, 18));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("card-footer");

        Button btnHide = new Button("Masquer");
        btnHide.getStyleClass().add("btn-ghost-danger");
        btnHide.setOnAction(e -> {
            try {
                service.hideDossier(d.getIdDossier()); all.remove(d); updateStats(); applyFilters();
                if (appController != null) appController.refreshSidebarStats();
            } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });

        Button btnSave = new Button("💾  Sauvegarder");
        btnSave.getStyleClass().add("btn-save");
        btnSave.setOnAction(e -> handleSave(d, tfNom, dp, tfBp, ta, cbEtat, btnSave));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(sp, btnHide, btnSave);
        card.getChildren().add(footer);
        return card;
    }

    // ── Sauvegarde inline avec validations ───────────────────────────────────
    private void handleSave(DossierCandidature d, TextField tfNom, DatePicker dp,
                            TextField tfBp, TextArea ta, ComboBox<String> cbEtat, Button btnSave) {
        String nom  = tfNom.getText() == null ? "" : tfNom.getText().trim();
        String bp   = tfBp.getText()  == null ? "" : tfBp.getText().trim();
        String desc = ta.getText()    == null ? "" : ta.getText().trim();

        // Validations
        if (nom.isEmpty())  { tfNom.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;"); alert("Requis", "Nom du dossier obligatoire.", Alert.AlertType.WARNING); return; }
        if (dp.getValue() == null) { alert("Requis", "Date obligatoire.", Alert.AlertType.WARNING); return; }
        if (bp.isEmpty())   { tfBp.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;"); alert("Requis", "Business Plan obligatoire.", Alert.AlertType.WARNING); return; }
        if (desc.isEmpty()) { alert("Requis", "Description obligatoire.", Alert.AlertType.WARNING); return; }

        // Validation BP
        boolean isFile = new File(bp).exists();
        boolean isUrl  = bp.startsWith("http://") || bp.startsWith("https://");
        boolean isExt  = bp.matches("(?i).*\\.(pdf|docx|doc|pptx|xlsx)$");
        if (!isFile && !isUrl && !isExt) {
            tfBp.setStyle("-fx-border-color:#f59e0b; -fx-border-width:2; -fx-border-radius:8;");
            alert("Business Plan invalide", "Choisissez un fichier via 📎 ou entrez une URL (http/https).", Alert.AlertType.WARNING);
            return;
        }

        d.setNomDossier(nom);
        d.setDescriptionProjet(desc);
        d.setBusinessPlan(bp);
        d.setDateCreation(Date.valueOf(dp.getValue()));
        d.setEtat(cbEtat.getValue());

        try {
            service.updateDossier(d);
            btnSave.setText("✅  Sauvegardé !"); btnSave.setDisable(true);
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> { btnSave.setText("💾  Sauvegarder"); btnSave.setDisable(false); });
            }).start();
            updateStats();
            if (appController != null) appController.refreshSidebarStats();
        } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
    }

    // ── Ouvrir le fichier/URL avec l'application système ─────────────────────
    private void openBp(String bp) {
        if (bp == null || bp.trim().isEmpty()) {
            alert("Aucun fichier", "Aucun Business Plan renseigné.", Alert.AlertType.WARNING);
            return;
        }
        try {
            if (bp.startsWith("http://") || bp.startsWith("https://")) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(bp));
            } else {
                File f = new File(bp);
                if (f.exists()) java.awt.Desktop.getDesktop().open(f);
                else alert("Fichier introuvable", "Le fichier n'existe pas : " + bp, Alert.AlertType.WARNING);
            }
        } catch (Exception ex) {
            alert("Erreur", "Impossible d'ouvrir : " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Validation live BP ────────────────────────────────────────────────────
    private void validateBpLive(TextField tf, String val) {
        if (val == null || val.trim().isEmpty()) { tf.setStyle(""); return; }
        boolean ok = new File(val).exists()
                || val.startsWith("http://") || val.startsWith("https://")
                || val.matches("(?i).*\\.(pdf|docx|doc|pptx|xlsx)$");
        tf.setStyle(ok
                ? "-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;"
                : "-fx-border-color:#f59e0b; -fx-border-width:2; -fx-border-radius:8;");
    }

    @FXML private void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-dossier.fxml"));
            VBox root = loader.load();
            AddDossierController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Nouveau Dossier");
            Scene scene = new Scene(root, 520, 660);
            for (String p : new String[]{"/style.css", "/css/style.css"}) {
                URL css = getClass().getResource(p);
                if (css != null) { scene.getStylesheets().add(css.toExternalForm()); break; }
            }
            stage.setScene(scene); stage.initModality(Modality.APPLICATION_MODAL); stage.setResizable(false);
            stage.showAndWait();
            if (ctrl.isSaved()) loadAll();
        } catch (Exception ex) { ex.printStackTrace(); alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private HBox row(String icon, String key, javafx.scene.Node val) {
        Label k = new Label(icon + "  " + key + " :"); k.getStyleClass().add("row-key"); k.setPrefWidth(130);
        HBox r = new HBox(10, k, val); r.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(val, Priority.ALWAYS);
        return r;
    }

    private VBox buildEmpty() {
        VBox b = new VBox(14); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80)); b.setPrefWidth(700);
        Label i = new Label("📂"); i.setStyle("-fx-font-size:56px;");
        Label t = new Label("Aucun dossier"); t.getStyleClass().add("empty-title");
        Label h = new Label("Créez un dossier via le bouton « Nouveau Dossier »."); h.getStyleClass().add("empty-hint");
        b.getChildren().addAll(i, t, h); return b;
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private void alert(String t, String m, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}