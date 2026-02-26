package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;
import utils.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class FinancementFrontController {

    // ===== Top bar =====
    @FXML private TextField tfSearch;
    @FXML private Label lbUserName;
    @FXML private Label lbUserRole;

    // ===== Quick stats =====
    @FXML private Label lbActiveRequests;
    @FXML private Label lbAvailableOffers;
    @FXML private Label lbTotalAmount;

    // ===== Role switch =====
    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton rbStartup;
    @FXML private RadioButton rbInvestor;

    // ===== Panels to show/hide =====
    @FXML private VBox startupPanel;
    @FXML private VBox investorPanel;

    // ===== Actions =====
    @FXML private Button btnNewRequest;
    @FXML private Button btnNewOffer;

    // ===== Filters =====
    @FXML private ComboBox<String> cbSector;
    @FXML private ComboBox<String> cbStage;
    @FXML private ComboBox<String> cbRange;
    @FXML private ComboBox<String> cbType;

    // ===== Lists =====
    @FXML private ListView<FinItem> lvStartupItems;
    @FXML private ListView<FinItem> lvInvestorItems;

    // ===== Data (DB loaded) =====
    private final ObservableList<FinItem> startupAll = FXCollections.observableArrayList();
    private final ObservableList<FinItem> investorAll = FXCollections.observableArrayList();

    private static final DecimalFormat money = new DecimalFormat("#,##0.00");

    // (optionnel) simulation “user connecté”
    private final int CURRENT_USER_ID = 8;

    @FXML
    public void initialize() {

        // ---- User header ----
        lbUserName.setText("Utilisateur");
        setRoleLabel("Startup");

        // ---- Filters (tu peux garder ces valeurs même si DB n'a pas secteur/stage/type) ----
        cbSector.setItems(FXCollections.observableArrayList("Tous", "FinTech", "HealthTech", "EdTech", "AgriTech", "SaaS", "E-commerce"));
        cbStage.setItems(FXCollections.observableArrayList("Tous", "Idea", "Pre-seed", "Seed", "Series A"));
        cbRange.setItems(FXCollections.observableArrayList("Tous", "0–10k", "10k–50k", "50k–100k", "100k+"));
        cbType.setItems(FXCollections.observableArrayList("Tous", "Equity", "Loan", "Grant", "Convertible"));

        cbSector.getSelectionModel().selectFirst();
        cbStage.getSelectionModel().selectFirst();
        cbRange.getSelectionModel().selectFirst();
        cbType.getSelectionModel().selectFirst();

        // ---- ListView cards ----
        lvStartupItems.setCellFactory(lv -> new FinItemCardCell(true));
        lvInvestorItems.setCellFactory(lv -> new FinItemCardCell(false));

        // ---- Default mode ----
        rbStartup.setSelected(true);
        applyRoleUI(true);

        // ---- Listeners ----
        roleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isStartup = (newT == rbStartup);
            applyRoleUI(isStartup);
        });

        tfSearch.textProperty().addListener((obs, oldV, newV) -> applySearchAndFilters());

        cbSector.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        cbStage.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        cbRange.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        cbType.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());

        // ---- Load DB data ----
        reloadAllFromDbAsync();
    }

    // ===============================
    // UI behavior
    // ===============================

    private void applyRoleUI(boolean isStartupMode) {
        startupPanel.setManaged(isStartupMode);
        startupPanel.setVisible(isStartupMode);

        investorPanel.setManaged(!isStartupMode);
        investorPanel.setVisible(!isStartupMode);

        btnNewRequest.setManaged(isStartupMode);
        btnNewRequest.setVisible(isStartupMode);

        btnNewOffer.setManaged(!isStartupMode);
        btnNewOffer.setVisible(!isStartupMode);

        setRoleLabel(isStartupMode ? "Startup" : "Investisseur");
        applySearchAndFilters();
    }

    private void setRoleLabel(String role) {
        lbUserRole.setText(role);
    }

    // ===============================
    // DB load
    // ===============================

    private void reloadAllFromDbAsync() {
        Task<Pair<List<FinItem>, List<FinItem>>> task = new Task<>() {
            @Override
            protected Pair<List<FinItem>, List<FinItem>> call() throws Exception {
                Map<Integer, ProjetRow> projets = fetchProjets();
                Map<Integer, AggInvest> agg = fetchAggInvestissements();

                List<FinItem> startup = new ArrayList<>();
                List<FinItem> investor = new ArrayList<>();

                for (ProjetRow p : projets.values()) {
                    AggInvest a = agg.getOrDefault(p.idProjet, new AggInvest(0.0, 0));
                    double totalLeve = a.totalFinance;
                    double budget = p.budget;
                    double reste = Math.max(0, budget - totalLeve);
                    double prog = (budget <= 0) ? 0 : (totalLeve * 100.0 / budget);

                    // Startup card = projet (demande)
                    String title = "PROJ-" + p.idProjet + " • Projet";
                    String subtitle = "Budget : " + money.format(budget) + " DT  •  Levé : " + money.format(totalLeve) + " DT (" + money.format(prog) + "%)";
                    String meta = (p.statut == null ? "EN_ATTENTE" : p.statut.toUpperCase(Locale.ROOT)) + " • Reste : " + money.format(reste) + " DT";
                    String desc = safe(p.description);

                    startup.add(new FinItem(FinKind.PROJET, p.idProjet, title, subtitle, meta, desc, budget, totalLeve));

                    // Investor card = opportunité (même projet mais angle investisseur)
                    String ititle = "OPP-" + p.idProjet + " • Opportunité";
                    String isub = "Besoin restant : " + money.format(reste) + " DT";
                    String imeta = "Statut projet : " + (p.statut == null ? "EN_ATTENTE" : p.statut.toUpperCase(Locale.ROOT)) + " • Nb invest : " + a.nbInvest;
                    String idesc = safe(p.titre) + " — " + cut(safe(p.description), 140);

                    investor.add(new FinItem(FinKind.OPPORTUNITE, p.idProjet, ititle, isub, imeta, idesc, budget, totalLeve));
                }

                // tri “joli”
                startup.sort(Comparator.comparingInt(a -> a.refId));
                investor.sort(Comparator.comparingDouble((FinItem x) -> (x.budget - x.totalLeve)).reversed());

                return new Pair<>(startup, investor);
            }
        };

        task.setOnSucceeded(e -> {
            Pair<List<FinItem>, List<FinItem>> res = task.getValue();
            startupAll.setAll(res.getKey());
            investorAll.setAll(res.getValue());

            lvStartupItems.setItems(startupAll);
            lvInvestorItems.setItems(investorAll);

            refreshStatsAsync(); // stats en DB
            applySearchAndFilters();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            // fallback visuel simple
            startupAll.clear();
            investorAll.clear();
            lvStartupItems.getItems().setAll(new FinItem(FinKind.INFO, -1, "Erreur DB", "", "", ex.getMessage(), 0, 0));
        });

        new Thread(task, "db-front-load").start();
    }

    private void refreshStatsAsync() {
        Task<FrontStats> task = new Task<>() {
            @Override
            protected FrontStats call() throws Exception {
                return fetchFrontStats();
            }
        };

        task.setOnSucceeded(e -> {
            FrontStats s = task.getValue();
            lbActiveRequests.setText(String.valueOf(s.nbProjetsAttente));
            lbAvailableOffers.setText(String.valueOf(s.nbInvestFinance));
            lbTotalAmount.setText(money.format(s.totalFinance) + " DT");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            lbActiveRequests.setText("—");
            lbAvailableOffers.setText("—");
            lbTotalAmount.setText("—");
        });

        new Thread(task, "db-front-stats").start();
    }

    // ===============================
    // Search + filters
    // ===============================

    private void applySearchAndFilters() {
        String q = safeLower(tfSearch.getText());

        String sector = valOrTous(cbSector);
        String stage = valOrTous(cbStage);
        String range = valOrTous(cbRange);
        String type  = valOrTous(cbType);

        ObservableList<FinItem> startupFiltered = startupAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        lvStartupItems.setItems(startupFiltered);

        ObservableList<FinItem> investorFiltered = investorAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        lvInvestorItems.setItems(investorFiltered);
    }

    private boolean matches(FinItem it, String q, String sector, String stage, String range, String type) {
        boolean ok = true;

        if (q != null && !q.isBlank()) {
            String hay = (it.title + " " + it.subtitle + " " + it.meta + " " + it.description).toLowerCase(Locale.ROOT);
            ok = ok && hay.contains(q);
        }

        // Comme DB n'a pas secteur/stage/type, on fait un “soft matching” sur les textes
        if (!"Tous".equalsIgnoreCase(sector)) ok = ok && (it.description.contains(sector) || it.meta.contains(sector));
        if (!"Tous".equalsIgnoreCase(stage))  ok = ok && (it.description.contains(stage) || it.meta.contains(stage));
        if (!"Tous".equalsIgnoreCase(type))   ok = ok && (it.description.contains(type) || it.meta.contains(type));

        // Range = basé sur budget (DT)
        if (!"Tous".equalsIgnoreCase(range)) {
            double b = it.budget;
            ok = ok && switch (range) {
                case "0–10k" -> b >= 0 && b < 10_000;
                case "10k–50k" -> b >= 10_000 && b < 50_000;
                case "50k–100k" -> b >= 50_000 && b < 100_000;
                case "100k+" -> b >= 100_000;
                default -> true;
            };
        }

        return ok;
    }

    private String valOrTous(ComboBox<String> cb) {
        String v = cb.getValue();
        return (v == null || v.isBlank()) ? "Tous" : v;
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    // ===============================
    // Sidebar navigation (à brancher avec ton switchScene)
    // ===============================

    @FXML private void goDashboard() { System.out.println("Navigation : Dashboard"); }
    @FXML private void goFinancement() { System.out.println("Vous êtes déjà sur Financement"); }
    @FXML private void goAccompagnement() { System.out.println("Navigation: Accompagnement"); }
    @FXML private void goEvenements() { System.out.println("Navigation: Événements"); }
    @FXML private void goCandidature() { System.out.println("Navigation: Candidature"); }

    @FXML private void logout() { System.out.println("Déconnexion..."); }

    // ===============================
    // Actions Financement (fonctionnelles)
    // ===============================

    @FXML
    private void newRequest() {
        Optional<ProjetForm> formOpt = showNewProjetDialog();
        if (formOpt.isEmpty()) return;

        ProjetForm form = formOpt.get();
        try {
            insertProjet(form);
            reloadAllFromDbAsync();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter projet", e.getMessage());
        }
    }

    @FXML
    private void newOffer() {
        Optional<InvestForm> formOpt = showNewInvestDialog();
        if (formOpt.isEmpty()) return;

        InvestForm form = formOpt.get();
        try {
            insertInvestissement(form);
            reloadAllFromDbAsync();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter investissement", e.getMessage());
        }
    }
    @FXML private void applyFilters() { applySearchAndFilters(); }
    @FXML private void resetFilters() {
        cbSector.getSelectionModel().selectFirst();
        cbStage.getSelectionModel().selectFirst();
        cbRange.getSelectionModel().selectFirst();
        cbType.getSelectionModel().selectFirst();
        tfSearch.clear();
        applySearchAndFilters();
    }

    // ===============================
    // DB queries
    // ===============================

    private Map<Integer, ProjetRow> fetchProjets() throws Exception {
        String sql = "SELECT id_projet, titre, description, budget, statut FROM projet";
        Map<Integer, ProjetRow> map = new LinkedHashMap<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ProjetRow p = new ProjetRow();
                p.idProjet = rs.getInt("id_projet");
                p.titre = rs.getString("titre");
                p.description = rs.getString("description");
                p.budget = rs.getDouble("budget");
                p.statut = rs.getString("statut");
                map.put(p.idProjet, p);
            }
        }
        return map;
    }

    private Map<Integer, AggInvest> fetchAggInvestissements() throws Exception {
        // total “FINANCE” par projet + nb investissements
        String sql = """
                SELECT id_projet,
                       COALESCE(SUM(CASE WHEN UPPER(statut)='FINANCE' THEN montantInvestissement ELSE 0 END),0) AS total_finance,
                       COUNT(*) AS nb_invest
                FROM investissement
                GROUP BY id_projet
                """;

        Map<Integer, AggInvest> map = new HashMap<>();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idProjet = rs.getInt("id_projet");
                double total = rs.getDouble("total_finance");
                int nb = rs.getInt("nb_invest");
                map.put(idProjet, new AggInvest(total, nb));
            }
        }
        return map;
    }

    private FrontStats fetchFrontStats() throws Exception {
        FrontStats s = new FrontStats();

        // projets EN_ATTENTE
        String sql1 = "SELECT COUNT(*) nb FROM projet WHERE UPPER(statut)='EN_ATTENTE'";
        // nb investissements FINANCE
        String sql2 = "SELECT COUNT(*) nb FROM investissement WHERE UPPER(statut)='FINANCE'";
        // total investi FINANCE
        String sql3 = "SELECT COALESCE(SUM(montantInvestissement),0) total FROM investissement WHERE UPPER(statut)='FINANCE'";

        try (Connection c = MyDatabase.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql1);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                s.nbProjetsAttente = rs.getInt("nb");
            }
            try (PreparedStatement ps = c.prepareStatement(sql2);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                s.nbInvestFinance = rs.getInt("nb");
            }
            try (PreparedStatement ps = c.prepareStatement(sql3);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                s.totalFinance = rs.getDouble("total");
            }
        }
        return s;
    }

    private void insertProjet(ProjetForm f) throws Exception {
        String sql = "INSERT INTO projet (titre, description, budget, statut) VALUES (?,?,?,?)";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, f.titre);
            ps.setString(2, f.description);
            ps.setBigDecimal(3, BigDecimal.valueOf(f.budget));
            ps.setString(4, f.statut);

            ps.executeUpdate();
        }
    }

    private void insertInvestissement(InvestForm f) throws Exception {
        String sql = "INSERT INTO investissement (montantInvestissement, date_investissement, statut, id_projet, id_user) VALUES (?,?,?,?,?)";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setBigDecimal(1, BigDecimal.valueOf(f.montant));
            ps.setDate(2, java.sql.Date.valueOf(f.date));
            ps.setString(3, f.statut);
            ps.setInt(4, f.idProjet);
            ps.setInt(5, CURRENT_USER_ID);

            ps.executeUpdate();
        }
    }

    // ===============================
    // Dialogs (forms)
    // ===============================

    private Optional<ProjetForm> showNewProjetDialog() {
        Dialog<ProjetForm> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle demande");
        dialog.setHeaderText("Créer un projet à financer");

        // Boutons
        ButtonType okType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        // Container "card" (style dashboard)
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18;");

        Label hint = new Label("Remplis les informations ci-dessous. Les champs * sont obligatoires.");
        hint.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12;");

        // Champs
        TextField tfTitre = new TextField();
        tfTitre.setPromptText("Ex: Plateforme SaaS de gestion médicale");
        tfTitre.getStyleClass().add("modern-input");

        TextArea taDesc = new TextArea();
        taDesc.setPromptText("Pitch court / description du projet...");
        taDesc.setPrefRowCount(4);
        taDesc.getStyleClass().addAll("modern-input");

        TextField tfBudget = new TextField();
        tfBudget.setPromptText("Ex: 50000");
        tfBudget.getStyleClass().add("modern-input");

        // TextFormatter: autoriser seulement chiffres + . ,
        tfBudget.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change -> {
            String n = change.getControlNewText();
            if (n.isEmpty()) return change;
            return n.matches("\\d{0,12}([\\.,]\\d{0,2})?") ? change : null;
        }));

        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatut.getSelectionModel().selectFirst();
        cbStatut.getStyleClass().add("modern-input");

        // Grille propre
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label lTitre = new Label("Titre *");      lTitre.getStyleClass().add("form-label");
        Label lDesc  = new Label("Description");  lDesc.getStyleClass().add("form-label");
        Label lBud   = new Label("Budget (DT) *");lBud.getStyleClass().add("form-label");
        Label lStat  = new Label("Statut");       lStat.getStyleClass().add("form-label");

        grid.addRow(0, lTitre, tfTitre);
        grid.addRow(1, lDesc,  taDesc);
        grid.addRow(2, lBud,   tfBudget);
        grid.addRow(3, lStat,  cbStatut);

        // Largeur champ
        tfTitre.setPrefWidth(420);
        tfBudget.setPrefWidth(200);
        cbStatut.setPrefWidth(200);

        // Message d’erreur (inline)
        Label lbErr = new Label();
        lbErr.getStyleClass().add("fieldError");
        lbErr.setVisible(false);
        lbErr.setManaged(false);

        card.getChildren().addAll(hint, grid, lbErr);
        dialog.getDialogPane().setContent(card);

        // Style boutons selon ton CSS
        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.getStyleClass().add("btn-add");

        Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-refresh");

        // Validation live
        Runnable validate = () -> {
            String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
            String btxt  = tfBudget.getText() == null ? "" : tfBudget.getText().trim();

            String err = null;
            if (titre.isEmpty()) err = "Le titre est obligatoire.";
            else if (btxt.isEmpty()) err = "Le budget est obligatoire.";
            else {
                try {
                    double b = Double.parseDouble(btxt.replace(",", "."));
                    if (b <= 0) err = "Le budget doit être > 0.";
                } catch (Exception ex) {
                    err = "Budget invalide (ex: 50000 ou 50000.50).";
                }
            }

            boolean ok = (err == null);
            okBtn.setDisable(!ok);

            lbErr.setText(err == null ? "" : "❌ " + err);
            lbErr.setVisible(!ok);
            lbErr.setManaged(!ok);
        };

        tfTitre.textProperty().addListener((o,a,b)-> validate.run());
        tfBudget.textProperty().addListener((o,a,b)-> validate.run());

        // état initial
        okBtn.setDisable(true);

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;

            ProjetForm f = new ProjetForm();
            f.titre = tfTitre.getText().trim();
            f.description = (taDesc.getText() == null) ? "" : taDesc.getText().trim();
            f.budget = Double.parseDouble(tfBudget.getText().trim().replace(",", "."));
            f.statut = cbStatut.getValue();
            return f;
        });

        // force validation au démarrage
        Platform.runLater(validate);

        return dialog.showAndWait();
    }

    private Optional<InvestForm> showNewInvestDialog() {
        Dialog<InvestForm> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle offre (Investissement)");
        dialog.setHeaderText("Investir dans un projet existant");

        // Premium look via CSS existant (si ton financement.css contient .modern-input / .btn-add / etc.)
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/financement.css")).toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("card");

        ButtonType ok = new ButtonType("Investir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        // Champs
        ComboBox<ProjetOption> cbProjet = new ComboBox<>();
        cbProjet.setPrefWidth(420);

        TextField tfMontant = new TextField();
        tfMontant.setPromptText("Ex: 25000");

        DatePicker dpDate = new DatePicker(LocalDate.now());

        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatut.getSelectionModel().selectFirst();

        // Styles (utilise tes classes CSS)
        cbProjet.getStyleClass().add("modern-input");
        tfMontant.getStyleClass().add("modern-input");
        dpDate.getStyleClass().add("modern-input");
        cbStatut.getStyleClass().add("modern-input");

        Label hint = new Label("");
        hint.getStyleClass().add("fieldError");
        hint.setWrapText(true);

        // Layout premium
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label lProjet = new Label("Projet");
        Label lMontant = new Label("Montant (DT)");
        Label lDate = new Label("Date");
        Label lStatut = new Label("Statut");

        lProjet.getStyleClass().add("form-label");
        lMontant.getStyleClass().add("form-label");
        lDate.getStyleClass().add("form-label");
        lStatut.getStyleClass().add("form-label");

        grid.add(lProjet, 0, 0);   grid.add(cbProjet, 1, 0);
        grid.add(lMontant, 0, 1);  grid.add(tfMontant, 1, 1);
        grid.add(lDate, 0, 2);     grid.add(dpDate, 1, 2);
        grid.add(lStatut, 0, 3);   grid.add(cbStatut, 1, 3);
        grid.add(hint, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Charger projets (synchrone ici = simple)
        try {
            List<ProjetOption> options = fetchProjetOptions();
            cbProjet.setItems(FXCollections.observableArrayList(options));
            if (!options.isEmpty()) cbProjet.getSelectionModel().selectFirst();
        } catch (Exception e) {
            hint.setText("Impossible de charger la liste des projets : " + e.getMessage());
        }

        Node okBtn = dialog.getDialogPane().lookupButton(ok);
        okBtn.getStyleClass().add("btn-add");
        okBtn.setDisable(true);

        Runnable validate = () -> {
            hint.setText("");

            ProjetOption p = cbProjet.getValue();
            if (p == null) {
                okBtn.setDisable(true);
                hint.setText("Aucun projet sélectionné.");
                return;
            }

            String m = tfMontant.getText() == null ? "" : tfMontant.getText().trim();
            if (!isDouble(m) || Double.parseDouble(m.replace(",", ".")) <= 0) {
                okBtn.setDisable(true);
                hint.setText("Montant invalide (doit être > 0).");
                return;
            }

            if (dpDate.getValue() == null) {
                okBtn.setDisable(true);
                hint.setText("Veuillez choisir une date.");
                return;
            }

            okBtn.setDisable(false);
        };

        cbProjet.valueProperty().addListener((o,a,b)-> validate.run());
        tfMontant.textProperty().addListener((o,a,b)-> validate.run());
        dpDate.valueProperty().addListener((o,a,b)-> validate.run());
        cbStatut.valueProperty().addListener((o,a,b)-> validate.run());

        // Première validation
        validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != ok) return null;

            ProjetOption p = cbProjet.getValue();
            if (p == null) return null;

            InvestForm f = new InvestForm();
            f.idProjet = p.id;
            f.montant = Double.parseDouble(tfMontant.getText().trim().replace(",", "."));
            f.date = dpDate.getValue();
            f.statut = cbStatut.getValue();
            return f;
        });

        return dialog.showAndWait();
    }

    private List<ProjetOption> fetchProjetOptions() {
        return List.of();
    }

    // ===============================
    // Helpers
    // ===============================

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(header);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String cut(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n"," ").replace("\r"," ").replace("\t"," ");
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private boolean isInt(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    private boolean isDouble(String s) {
        try { Double.parseDouble(s.replace(",", ".")); return true; } catch (Exception e) { return false; }
    }

    // ===============================
    // Models
    // ===============================

    enum FinKind { PROJET, OPPORTUNITE, INFO }

    public static class FinItem {
        public final FinKind kind;
        public final int refId;              // id_projet
        public final String title;
        public final String subtitle;
        public final String meta;
        public final String description;
        public final double budget;
        public final double totalLeve;

        public FinItem(FinKind kind, int refId, String title, String subtitle, String meta, String description, double budget, double totalLeve) {
            this.kind = kind;
            this.refId = refId;
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.description = description;
            this.budget = budget;
            this.totalLeve = totalLeve;
        }
    }

    private static class ProjetRow {
        int idProjet;
        String titre;
        String description;
        double budget;
        String statut;
    }

    private static class AggInvest {
        double totalFinance;
        int nbInvest;
        AggInvest(double totalFinance, int nbInvest) {
            this.totalFinance = totalFinance;
            this.nbInvest = nbInvest;
        }
    }

    private static class FrontStats {
        int nbProjetsAttente;
        int nbInvestFinance;
        double totalFinance;
    }

    private static class ProjetForm {
        String titre;
        String description;
        double budget;
        String statut;
    }

    private static class InvestForm {
        int idProjet;
        double montant;
        LocalDate date;
        String statut;
    }

    // ===============================
    // Cell UI (cards)
    // ===============================

    private static class FinItemCardCell extends ListCell<FinItem> {

        private final VBox root = new VBox(6);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Text meta = new Text();
        private final Text desc = new Text();
        private final ToolBar actions = new ToolBar();

        private final Button btnView = new Button("Voir");
        private final Button btnAction = new Button("Action");

        private final boolean startupMode;

        FinItemCardCell(boolean startupMode) {
            this.startupMode = startupMode;

            root.getStyleClass().add("card");
            title.getStyleClass().add("card-title");
            subtitle.getStyleClass().add("muted");
            meta.getStyleClass().add("card-meta");
            desc.getStyleClass().add("card-desc");

            desc.wrappingWidthProperty().bind(root.widthProperty().subtract(24));
            meta.wrappingWidthProperty().bind(root.widthProperty().subtract(24));

            btnView.getStyleClass().add("btn-refresh");
            btnAction.getStyleClass().add("btn-add");

            actions.getItems().addAll(btnView, new Separator(), btnAction);
            actions.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            root.getChildren().addAll(title, subtitle, meta, desc, actions);

            btnView.setOnAction(e -> {
                FinItem item = getItem();
                if (item != null) System.out.println("Voir: " + item.title + " (ID=" + item.refId + ")");
            });

            btnAction.setOnAction(e -> {
                FinItem item = getItem();
                if (item != null) {
                    if (startupMode) {
                        System.out.println("Action Startup: gérer projet ID=" + item.refId);
                    } else {
                        System.out.println("Action Investisseur: investir sur projet ID=" + item.refId);
                    }
                }
            });
        }

        @Override
        protected void updateItem(FinItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            title.setText(item.title);
            subtitle.setText(item.subtitle);
            meta.setText(item.meta);
            desc.setText(item.description);

            // bouton label contextuel
            btnAction.setText(startupMode ? "Gérer" : "Investir");

            setGraphic(root);
        }
    }
    // ====== handlers appelés par le "domain-card" du FXML ======

    @FXML private void openRequestDetails() {
        System.out.println("Détails demande (card FXML)");
    }

    @FXML private void editRequest() {
        System.out.println("Modifier demande (card FXML)");
    }

    @FXML private void cancelRequest() {
        System.out.println("Annuler demande (card FXML)");
    }

    @FXML private void openOfferDetails() {
        System.out.println("Détails offre (card FXML)");
    }

    @FXML private void editOffer() {
        System.out.println("Modifier offre (card FXML)");
    }

    @FXML private void disableOffer() {
        System.out.println("Désactiver offre (card FXML)");
    }

    // Option affichée dans la ComboBox (id + titre)
    private static class ProjetOption {
        final int id;
        final String label;

        ProjetOption(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override public String toString() { // ce qui s'affiche dans la ComboBox
            return label;
        }

        private List<ProjetOption> fetchProjetOptions() throws Exception {
            String sql = "SELECT id_projet, titre, statut, budget FROM projet ORDER BY id_projet DESC";
            List<ProjetOption> list = new ArrayList<>();

            try (Connection c = MyDatabase.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id_projet");
                    String titre = rs.getString("titre");
                    String statut = rs.getString("statut");
                    double budget = rs.getDouble("budget");

                    String niceTitre = (titre == null || titre.isBlank()) ? ("Projet #" + id) : titre.trim();
                    String niceStatut = (statut == null || statut.isBlank()) ? "—" : statut.toUpperCase(Locale.ROOT);

                    String label = "PROJ-" + id + " • " + niceTitre + "  (" + niceStatut + " • " + money.format(budget) + " DT)";
                    list.add(new ProjetOption(id, label));
                }
            }
            return list;
        }
    }
}