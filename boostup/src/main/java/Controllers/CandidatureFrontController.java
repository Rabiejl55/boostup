package Controllers;

import entities.GCandidature.Candidature;
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
import services.CandidatureService.CandidatureService;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CandidatureFrontController implements Initializable {

    @FXML private FlowPane     cardsFlowPane;
    @FXML private Button       btnAdd;
    @FXML private TextField    tfSearch;
    @FXML private ComboBox<String> cbFilter;
    @FXML private Label        lblEnAttente;
    @FXML private Label        lblValidees;
    @FXML private Label        lblRefusees;

    private final CandidatureService service  = new CandidatureService();
    private final DateTimeFormatter  fmt      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LocalDate          MIN_DATE = LocalDate.of(2026, 2, 23);
    private List<Candidature>        all;
    private AppController            appController;

    public void setAppController(AppController app) {
        this.appController = app;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbFilter.getItems().addAll("Tous", "EN_ATTENTE", "VALIDEE", "REFUSEE");
        cbFilter.setValue("Tous");
        cbFilter.setOnAction(e -> applyFilters());
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        loadAll();
    }

    // ─── Data ────────────────────────────────────────────────────────────────
    public void loadAll() {
        try { all = service.getAllCandidatures(true); }
        catch (SQLException ex) { all = List.of(); alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        updateStats();
        applyFilters();
        if (appController != null) appController.refreshSidebarStats();
    }

    private void updateStats() {
        if (all == null) return;
        lblEnAttente.setText(String.valueOf(all.stream().filter(c -> "EN_ATTENTE".equals(c.getStatut())).count()));
        lblValidees.setText(String.valueOf(all.stream().filter(c -> "VALIDEE".equals(c.getStatut())).count()));
        lblRefusees.setText(String.valueOf(all.stream().filter(c -> "REFUSEE".equals(c.getStatut())).count()));
    }

    private void applyFilters() {
        if (all == null) return;
        String s = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        String f = cbFilter.getValue();
        List<Candidature> filtered = all.stream().filter(c -> {
            boolean ms = "Tous".equals(f) || f == null || f.equals(c.getStatut());
            boolean mq = s.isEmpty()
                    || (c.getNomCandidature() != null && c.getNomCandidature().toLowerCase().contains(s))
                    || (c.getNomStartup()     != null && c.getNomStartup().toLowerCase().contains(s));
            return ms && mq;
        }).collect(Collectors.toList());

        cardsFlowPane.getChildren().clear();
        if (filtered.isEmpty()) cardsFlowPane.getChildren().add(buildEmptyState("📭", "Aucune candidature", "Ajoutez une candidature ou modifiez la recherche."));
        else filtered.forEach(c -> cardsFlowPane.getChildren().add(buildCard(c)));
    }

    // ─── Carte ───────────────────────────────────────────────────────────────
    private VBox buildCard(Candidature c) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPrefWidth(360);

        // Header coloré selon statut
        HBox header = new HBox(12);
        header.getStyleClass().addAll("card-header", "card-header-" + cls(c.getStatut()));
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(5);
        HBox.setHgrow(texts, Priority.ALWAYS);

        TextField tfNom = new TextField(nvl(c.getNomCandidature()));
        tfNom.getStyleClass().add("card-tf-title");

        TextField tfStartup = new TextField(nvl(c.getNomStartup()));
        tfStartup.getStyleClass().add("card-tf-sub");

        texts.getChildren().addAll(tfNom, tfStartup);

        Label badge = new Label(emoji(c.getStatut()) + "  " + label(c.getStatut()));
        badge.getStyleClass().addAll("badge", "badge-" + cls(c.getStatut()));
        badge.setTooltip(new Tooltip("Statut mis à jour après évaluation back-office"));

        header.getChildren().addAll(texts, badge);
        card.getChildren().add(header);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 12, 18));
        body.getStyleClass().add("card-body");

        // Date — éditable
        DatePicker dp = new DatePicker(c.getDateDepot().toLocalDate());
        dp.getStyleClass().add("card-dp");
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(MIN_DATE));
            }
        });
        body.getChildren().add(row("📅", "Date de dépôt", dp));

        // Score — lecture seule
        Label scoreLabel = new Label(c.getScore() == null ? "En attente d'évaluation" : String.format("%.2f / 10", c.getScore()));
        scoreLabel.getStyleClass().add("score-ro");
        scoreLabel.setTooltip(new Tooltip("Calculé automatiquement par le back-office"));
        body.getChildren().add(row("⭐", "Score", scoreLabel));

        // Commentaire — éditable
        Label lbCmt = new Label("💬  Commentaire");
        lbCmt.getStyleClass().add("row-key");
        TextArea ta = new TextArea(nvl(c.getCommentaire()));
        ta.setWrapText(true); ta.setPrefRowCount(2);
        ta.getStyleClass().add("card-ta");
        ta.setPromptText("Votre commentaire…");
        body.getChildren().addAll(lbCmt, ta);
        card.getChildren().add(body);

        // Footer
        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 18, 14, 18));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("card-footer");

        Button btnHide = new Button("Masquer");
        btnHide.getStyleClass().add("btn-ghost-danger");
        btnHide.setOnAction(e -> {
            try { service.hideCandidature(c.getIdCandidature()); all.remove(c); updateStats(); applyFilters(); if (appController != null) appController.refreshSidebarStats(); }
            catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });

        Button btnSave = new Button("💾  Sauvegarder");
        btnSave.getStyleClass().add("btn-save");
        btnSave.setOnAction(e -> handleSave(c, tfNom, tfStartup, dp, ta, btnSave));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(sp, btnHide, btnSave);
        card.getChildren().add(footer);
        return card;
    }

    private void handleSave(Candidature c, TextField tfNom, TextField tfStartup, DatePicker dp, TextArea ta, Button btn) {
        if (tfNom.getText().trim().isEmpty()) { alert("Requis", "Nom obligatoire.", Alert.AlertType.WARNING); return; }
        if (tfStartup.getText().trim().isEmpty()) { alert("Requis", "Startup obligatoire.", Alert.AlertType.WARNING); return; }
        if (dp.getValue() == null || dp.getValue().isBefore(MIN_DATE)) {
            alert("Date invalide", "Date ≥ " + MIN_DATE.format(fmt), Alert.AlertType.WARNING);
            dp.setValue(c.getDateDepot().toLocalDate()); return;
        }
        c.setNomCandidature(tfNom.getText().trim());
        c.setNomStartup(tfStartup.getText().trim());
        c.setDateDepot(Date.valueOf(dp.getValue()));
        c.setCommentaire(ta.getText().trim());
        try {
            service.updateCandidature(c);
            btn.setText("✅  Sauvegardé !"); btn.setDisable(true);
            new Thread(() -> { try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> { btn.setText("💾  Sauvegarder"); btn.setDisable(false); }); }).start();
        } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
    }

    @FXML private void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-candidature.fxml"));
            VBox root = loader.load();
            AddCandidatureController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Candidature");
            Scene scene = new Scene(root, 500, 540);
            loadCss(scene);
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
    private VBox buildEmptyState(String icon, String title, String hint) {
        VBox b = new VBox(14); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80)); b.setPrefWidth(700);
        Label i = new Label(icon); i.setStyle("-fx-font-size:56px;");
        Label t = new Label(title); t.getStyleClass().add("empty-title");
        Label h = new Label(hint);  h.getStyleClass().add("empty-hint");
        b.getChildren().addAll(i, t, h); return b;
    }
    private String cls(String s)   { return switch(s==null?"":s){case"VALIDEE"->"validee";case"REFUSEE"->"refusee";default->"attente";}; }
    private String label(String s) { return switch(s==null?"":s){case"VALIDEE"->"Validée";case"REFUSEE"->"Refusée";default->"En attente";}; }
    private String emoji(String s) { return switch(s==null?"":s){case"VALIDEE"->"✅";case"REFUSEE"->"❌";default->"⏳";}; }
    private String nvl(String s)   { return s == null ? "" : s; }
    private void loadCss(Scene scene) {
        for (String p : new String[]{"/style.css","/css/style.css"}) {
            URL u = getClass().getResource(p); if (u != null) { scene.getStylesheets().add(u.toExternalForm()); return; }
        }
    }
    private void alert(String t, String m, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}