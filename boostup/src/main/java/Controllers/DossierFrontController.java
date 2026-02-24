package Controllers;

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
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.DossierCandidatureService;

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

    public void setAppController(AppController app) {
        this.appController = app;
    }

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
        lblComplets.setText(String.valueOf(all.stream().filter(d -> "COMPLET".equals(d.getEtat())).count()));
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
        card.setPrefWidth(380);

        // Header
        String etatCls = "COMPLET".equals(d.getEtat()) ? "validee" : "attente";
        HBox header = new HBox(12);
        header.getStyleClass().addAll("card-header", "card-header-" + etatCls);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(5);
        HBox.setHgrow(texts, Priority.ALWAYS);

        // Nom dossier — éditable
        TextField tfNom = new TextField(nvl(d.getNomDossier()));
        tfNom.getStyleClass().add("card-tf-title");

        // Candidature associée — lecture seule
        Label lblCand = new Label("📋  " + nvl(d.getNomCandidature()));
        lblCand.getStyleClass().add("card-tf-sub-ro");

        texts.getChildren().addAll(tfNom, lblCand);

        // Badge état — éditable via ComboBox
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

        // Date création — éditable
        DatePicker dp = new DatePicker(d.getDateCreation() != null ? d.getDateCreation().toLocalDate() : LocalDate.now());
        dp.getStyleClass().add("card-dp");
        dp.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(row("📅", "Date création", dp));

        // Business plan — éditable
        TextField tfBp = new TextField(nvl(d.getBusinessPlan()));
        tfBp.getStyleClass().add("card-tf-inline");
        tfBp.setPromptText("Nom de fichier ou lien…");
        body.getChildren().add(row("📎", "Business Plan", tfBp));

        // Description — éditable
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
            try { service.hideDossier(d.getIdDossier()); all.remove(d); updateStats(); applyFilters(); if (appController != null) appController.refreshSidebarStats(); }
            catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });

        Button btnSave = new Button("💾  Sauvegarder");
        btnSave.getStyleClass().add("btn-save");
        btnSave.setOnAction(e -> {
            if (tfNom.getText().trim().isEmpty()) { alert("Requis", "Nom du dossier obligatoire.", Alert.AlertType.WARNING); return; }
            if (dp.getValue() == null)            { alert("Requis", "Date obligatoire.",            Alert.AlertType.WARNING); return; }
            d.setNomDossier(tfNom.getText().trim());
            d.setDescriptionProjet(ta.getText().trim());
            d.setBusinessPlan(tfBp.getText().trim());
            d.setDateCreation(Date.valueOf(dp.getValue()));
            d.setEtat(cbEtat.getValue());
            try {
                service.updateDossier(d);
                btnSave.setText("✅  Sauvegardé !"); btnSave.setDisable(true);
                new Thread(() -> { try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> { btnSave.setText("💾  Sauvegarder"); btnSave.setDisable(false); }); }).start();
                updateStats();
            } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(sp, btnHide, btnSave);
        card.getChildren().add(footer);
        return card;
    }

    @FXML private void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-dossier.fxml"));
            VBox root = loader.load();
            AddDossierController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Nouveau Dossier");
            Scene scene = new Scene(root, 500, 640);
            for (String p : new String[]{"/style.css", "/css/style.css"}) {
                URL css = getClass().getResource(p); if (css != null) { scene.getStylesheets().add(css.toExternalForm()); break; }
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