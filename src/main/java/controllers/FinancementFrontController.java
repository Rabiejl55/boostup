package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;
import utils.MyDatabase;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;


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

    // ===== Panels =====
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

    // ===== Investor Portfolio (NEW) =====
    @FXML private Label lbPortfolioHint;
    @FXML private Label lbBudgetAnnuel;
    @FXML private Label lbTotalInvesti;
    @FXML private Label lbBudgetRestant;
    @FXML private Label lbNbDeals;
    @FXML private Label lbDiversification;

    // ===== Investor Strategy (NEW) =====
    @FXML private TextField tfPrefSectors;
    @FXML private ComboBox<String> cbPrefStage;
    @FXML private TextField tfTicketMin;
    @FXML private TextField tfTicketMax;
    @FXML private Slider slRiskMax;
    @FXML private Label lbRiskValue;
    @FXML private Label lbStrategySaved;
    @FXML private BorderPane root;
    // ===== Data =====
    private final ObservableList<FinItem> startupAll = FXCollections.observableArrayList();
    private final ObservableList<FinItem> investorAll = FXCollections.observableArrayList();

    private static final DecimalFormat money = new DecimalFormat("#,##0.00");

    // Simu user connecté
    private static final int CURRENT_USER_ID = 8;

    // Strategy state
    private InvestorStrategy strategy = InvestorStrategy.defaults();

    @FXML
    public void initialize() {

        // ---- User header ----
        if (lbUserName != null) lbUserName.setText("Utilisateur");
        setRoleLabel("Startup");

        // ---- Filters ----
        if (cbSector != null) cbSector.setItems(FXCollections.observableArrayList("Tous", "FinTech", "HealthTech", "EdTech", "AgriTech", "SaaS", "E-commerce"));
        if (cbStage != null) cbStage.setItems(FXCollections.observableArrayList("Tous", "Idea", "Pre-seed", "Seed", "Series A"));
        if (cbRange != null) cbRange.setItems(FXCollections.observableArrayList("Tous", "0–10k", "10k–50k", "50k–100k", "100k+"));
        if (cbType != null)  cbType.setItems(FXCollections.observableArrayList("Tous", "Equity", "Loan", "Grant", "Convertible"));

        if (cbSector != null) cbSector.getSelectionModel().selectFirst();
        if (cbStage != null) cbStage.getSelectionModel().selectFirst();
        if (cbRange != null) cbRange.getSelectionModel().selectFirst();
        if (cbType != null)  cbType.getSelectionModel().selectFirst();

        // ---- Investor stage pref combo ----
        if (cbPrefStage != null) {
            cbPrefStage.setItems(FXCollections.observableArrayList("TOUS", "Idea", "Pre-seed", "Seed", "Series A"));
            cbPrefStage.getSelectionModel().select("TOUS");
        }

        // ---- ListView cards ----
        if (lvStartupItems != null) lvStartupItems.setCellFactory(lv -> new FinItemCardCell(true, this));
        if (lvInvestorItems != null) lvInvestorItems.setCellFactory(lv -> new FinItemCardCell(false, this));

        // ---- Default mode ----
        if (rbStartup != null) rbStartup.setSelected(true);
        applyRoleUI(true);

        // ---- Listeners ----
        if (roleGroup != null) {
            roleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                boolean isStartup = (newT == rbStartup);
                applyRoleUI(isStartup);
            });
        }

        if (tfSearch != null) tfSearch.textProperty().addListener((obs, oldV, newV) -> applySearchAndFilters());

        if (cbSector != null) cbSector.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        if (cbStage != null) cbStage.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        if (cbRange != null) cbRange.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());
        if (cbType != null)  cbType.valueProperty().addListener((obs, o, n) -> applySearchAndFilters());

        // ---- Strategy listeners (auto recommendations) ----
        setupStrategyAutoApply();

        // ---- Load DB data ----
        reloadAllFromDbAsync();

        // ---- Load investor strategy + portfolio KPIs ----
        loadStrategyAsync();
        refreshPortfolioAsync();
    }

    // ===============================
    // Navigation vers projet_details.fxml
    // ===============================

    private void openProjetDetails(int projectId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projet_details.fxml"));
            Parent root = loader.load();

            ProjetDetailsController ctrl = loader.getController();

            Scene scene = lvInvestorItems.getScene();
            Stage stage = (Stage) scene.getWindow();

            Scene oldScene = stage.getScene();

            ctrl.init(projectId, CURRENT_USER_ID, () -> stage.setScene(oldScene));

            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page détails", e.getMessage());
        }
    }

    private Scene getAnyScene() {
        if (startupPanel != null && startupPanel.getScene() != null) return startupPanel.getScene();
        if (investorPanel != null && investorPanel.getScene() != null) return investorPanel.getScene();
        if (tfSearch != null && tfSearch.getScene() != null) return tfSearch.getScene();
        return null;
    }

    // ===============================
    // UI behavior
    // ===============================

    private void applyRoleUI(boolean isStartupMode) {
        if (startupPanel != null) {
            startupPanel.setManaged(isStartupMode);
            startupPanel.setVisible(isStartupMode);
        }

        if (investorPanel != null) {
            investorPanel.setManaged(!isStartupMode);
            investorPanel.setVisible(!isStartupMode);
        }

        if (btnNewRequest != null) {
            btnNewRequest.setManaged(isStartupMode);
            btnNewRequest.setVisible(isStartupMode);
        }

        if (btnNewOffer != null) {
            btnNewOffer.setManaged(!isStartupMode);
            btnNewOffer.setVisible(!isStartupMode);
        }

        setRoleLabel(isStartupMode ? "Startup" : "Investisseur");
        applySearchAndFilters();
    }

    private void setRoleLabel(String role) {
        if (lbUserRole != null) lbUserRole.setText(role);
    }

    // ===============================
    // Strategy (Portfolio)
    // ===============================

    private void setupStrategyAutoApply() {
        if (slRiskMax != null && lbRiskValue != null) {
            lbRiskValue.setText((int) slRiskMax.getValue() + "/100");
            slRiskMax.valueProperty().addListener((o, a, b) -> {
                lbRiskValue.setText(b.intValue() + "/100");
                readStrategyFromUI();
                rerankInvestorList();
            });
        }

        if (tfPrefSectors != null) tfPrefSectors.textProperty().addListener((o,a,b) -> { readStrategyFromUI(); rerankInvestorList(); });
        if (cbPrefStage != null) cbPrefStage.valueProperty().addListener((o,a,b) -> { readStrategyFromUI(); rerankInvestorList(); });
        if (tfTicketMin != null) tfTicketMin.textProperty().addListener((o,a,b) -> { readStrategyFromUI(); rerankInvestorList(); });
        if (tfTicketMax != null) tfTicketMax.textProperty().addListener((o,a,b) -> { readStrategyFromUI(); rerankInvestorList(); });
    }

    private void readStrategyFromUI() {
        InvestorStrategy s = new InvestorStrategy();

        s.budgetAnnuel = strategy.budgetAnnuel;
        s.prefSectorsCsv = safe(tfPrefSectors != null ? tfPrefSectors.getText() : "");
        s.prefStage = safe(cbPrefStage != null ? cbPrefStage.getValue() : "TOUS");
        s.ticketMin = parseDoubleSafe(tfTicketMin != null ? tfTicketMin.getText() : "", 0);
        s.ticketMax = parseDoubleSafe(tfTicketMax != null ? tfTicketMax.getText() : "", 999999999);
        s.riskMax = (slRiskMax != null) ? (int) slRiskMax.getValue() : 60;

        if (s.ticketMin < 0) s.ticketMin = 0;
        if (s.ticketMax < s.ticketMin) s.ticketMax = s.ticketMin;

        this.strategy = s;
    }

    private void applyStrategyToUI() {
        if (tfPrefSectors != null) tfPrefSectors.setText(strategy.prefSectorsCsv);
        if (cbPrefStage != null) cbPrefStage.getSelectionModel().select(strategy.prefStage == null ? "TOUS" : strategy.prefStage);
        if (tfTicketMin != null) tfTicketMin.setText(trimNumber(strategy.ticketMin));
        if (tfTicketMax != null) tfTicketMax.setText(trimNumber(strategy.ticketMax));

        if (slRiskMax != null) slRiskMax.setValue(strategy.riskMax);
        if (lbRiskValue != null) lbRiskValue.setText(strategy.riskMax + "/100");

        if (lbBudgetAnnuel != null) lbBudgetAnnuel.setText(money.format(strategy.budgetAnnuel) + " DT");
    }

    @FXML
    private void saveStrategy() {
        readStrategyFromUI();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ensureStrategyTable();
                upsertStrategy(CURRENT_USER_ID, strategy);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (lbStrategySaved != null) lbStrategySaved.setText("✅ Stratégie enregistrée");
            refreshPortfolioAsync();
            rerankInvestorList();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (lbStrategySaved != null) lbStrategySaved.setText("❌ " + (ex == null ? "Erreur" : ex.getMessage()));
        });

        Thread th = new Thread(task, "save-strategy");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void resetStrategy() {
        this.strategy = InvestorStrategy.defaults();
        applyStrategyToUI();
        rerankInvestorList();
        if (lbStrategySaved != null) lbStrategySaved.setText("↩ Stratégie réinitialisée");
    }

    private void loadStrategyAsync() {
        Task<InvestorStrategy> task = new Task<>() {
            @Override protected InvestorStrategy call() throws Exception {
                ensureStrategyTable();
                InvestorStrategy s = fetchStrategy(CURRENT_USER_ID);
                return (s == null) ? InvestorStrategy.defaults() : s;
            }
        };

        task.setOnSucceeded(e -> {
            strategy = task.getValue();
            applyStrategyToUI();
            rerankInvestorList();
        });

        task.setOnFailed(e -> {
            strategy = InvestorStrategy.defaults();
            applyStrategyToUI();
            rerankInvestorList();
        });

        Thread th = new Thread(task, "load-strategy");
        th.setDaemon(true);
        th.start();
    }

    private void refreshPortfolioAsync() {
        Task<PortfolioStats> task = new Task<>() {
            @Override protected PortfolioStats call() throws Exception {
                ensureStrategyTable();
                InvestorStrategy s = fetchStrategy(CURRENT_USER_ID);
                if (s != null) strategy = s;

                double totalInvesti = fetchTotalInvestedByUser(CURRENT_USER_ID);
                int nbDeals = fetchDealsCountByUser(CURRENT_USER_ID);
                int diversification = fetchDistinctProjectsInvestedByUser(CURRENT_USER_ID);

                PortfolioStats ps = new PortfolioStats();
                ps.budgetAnnuel = strategy.budgetAnnuel;
                ps.totalInvesti = totalInvesti;
                ps.budgetRestant = Math.max(0, ps.budgetAnnuel - ps.totalInvesti);
                ps.nbDeals = nbDeals;
                ps.diversification = diversification;
                return ps;
            }
        };

        task.setOnSucceeded(e -> {
            PortfolioStats p = task.getValue();
            if (lbBudgetAnnuel != null) lbBudgetAnnuel.setText(money.format(p.budgetAnnuel) + " DT");
            if (lbTotalInvesti != null) lbTotalInvesti.setText(money.format(p.totalInvesti) + " DT");
            if (lbBudgetRestant != null) lbBudgetRestant.setText(money.format(p.budgetRestant) + " DT");
            if (lbNbDeals != null) lbNbDeals.setText(String.valueOf(p.nbDeals));
            if (lbDiversification != null) lbDiversification.setText(p.diversification + " projets");

            if (lbPortfolioHint != null) {
                lbPortfolioHint.setText(p.budgetRestant <= 0 ? "⚠ Budget annuel épuisé" : "✅ Budget disponible");
            }

            applyStrategyToUI();
        });

        task.setOnFailed(e -> {
            if (lbPortfolioHint != null) lbPortfolioHint.setText("❌ Erreur portefeuille");
        });

        Thread th = new Thread(task, "portfolio");
        th.setDaemon(true);
        th.start();
    }

    // ===============================
    // Load DB
    // ===============================

    private void reloadAllFromDbAsync() {
        Task<Pair<List<FinItem>, List<FinItem>>> task = new Task<>() {
            @Override protected Pair<List<FinItem>, List<FinItem>> call() throws Exception {
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

                    String title = "PROJ-" + p.idProjet + " • Projet";
                    String subtitle = "Budget : " + money.format(budget) + " DT  •  Levé : " + money.format(totalLeve) + " DT (" + money.format(prog) + "%)";
                    String meta = (p.statut == null ? "EN_ATTENTE" : p.statut.toUpperCase(Locale.ROOT)) + " • Reste : " + money.format(reste) + " DT";
                    String desc = safe(p.description);

                    startup.add(new FinItem(FinKind.PROJET, p.idProjet, title, subtitle, meta, desc, budget, totalLeve, 0, ""));

                    String ititle = "OPP-" + p.idProjet + " • Opportunité";
                    String isub = "Besoin restant : " + money.format(reste) + " DT";
                    String imeta = "Statut projet : " + (p.statut == null ? "EN_ATTENTE" : p.statut.toUpperCase(Locale.ROOT)) + " • Nb invest : " + a.nbInvest;
                    String idesc = safe(p.titre) + " — " + cut(safe(p.description), 140);

                    ScoreResult sr = scoreProjectForInvestor(p, a, strategy);
                    investor.add(new FinItem(FinKind.OPPORTUNITE, p.idProjet, ititle, isub, imeta, idesc, budget, totalLeve, sr.score, sr.explain));
                }

                startup.sort(Comparator.comparingInt(x -> x.refId));
                investor.sort(Comparator.comparingInt((FinItem x) -> x.score).reversed());

                return new Pair<>(startup, investor);
            }
        };

        task.setOnSucceeded(e -> {
            Pair<List<FinItem>, List<FinItem>> res = task.getValue();
            startupAll.setAll(res.getKey());
            investorAll.setAll(res.getValue());

            if (lvStartupItems != null) lvStartupItems.setItems(startupAll);
            if (lvInvestorItems != null) lvInvestorItems.setItems(investorAll);

            refreshStatsAsync();
            applySearchAndFilters();
            refreshPortfolioAsync();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            startupAll.clear();
            investorAll.clear();
            if (lvStartupItems != null) {
                lvStartupItems.getItems().setAll(new FinItem(FinKind.INFO, -1, "Erreur DB", "", "", ex.getMessage(), 0, 0, 0, ""));
            }
        });

        Thread th = new Thread(task, "db-front-load");
        th.setDaemon(true);
        th.start();
    }

    private void refreshStatsAsync() {
        Task<FrontStats> task = new Task<>() {
            @Override protected FrontStats call() throws Exception {
                return fetchFrontStats();
            }
        };

        task.setOnSucceeded(e -> {
            FrontStats s = task.getValue();
            if (lbActiveRequests != null) lbActiveRequests.setText(String.valueOf(s.nbProjetsAttente));
            if (lbAvailableOffers != null) lbAvailableOffers.setText(String.valueOf(s.nbInvestFinance));
            if (lbTotalAmount != null) lbTotalAmount.setText(money.format(s.totalFinance) + " DT");
        });

        task.setOnFailed(e -> {
            if (lbActiveRequests != null) lbActiveRequests.setText("—");
            if (lbAvailableOffers != null) lbAvailableOffers.setText("—");
            if (lbTotalAmount != null) lbTotalAmount.setText("—");
        });

        Thread th = new Thread(task, "db-front-stats");
        th.setDaemon(true);
        th.start();
    }

    // ===============================
    // Search + filters
    // ===============================

    private void applySearchAndFilters() {
        String q = safeLower(tfSearch != null ? tfSearch.getText() : "");

        String sector = valOrTous(cbSector);
        String stage = valOrTous(cbStage);
        String range = valOrTous(cbRange);
        String type  = valOrTous(cbType);

        ObservableList<FinItem> startupFiltered = startupAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        if (lvStartupItems != null) lvStartupItems.setItems(startupFiltered);

        ObservableList<FinItem> investorFiltered = investorAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        if (lvInvestorItems != null) lvInvestorItems.setItems(investorFiltered);
    }

    private boolean matches(FinItem it, String q, String sector, String stage, String range, String type) {
        boolean ok = true;

        if (q != null && !q.isBlank()) {
            String hay = (it.title + " " + it.subtitle + " " + it.meta + " " + it.description).toLowerCase(Locale.ROOT);
            ok = hay.contains(q);
        }

        if (!"Tous".equalsIgnoreCase(sector)) ok = ok && (it.description.contains(sector) || it.meta.contains(sector));
        if (!"Tous".equalsIgnoreCase(stage))  ok = ok && (it.description.contains(stage) || it.meta.contains(stage));
        if (!"Tous".equalsIgnoreCase(type))   ok = ok && (it.description.contains(type) || it.meta.contains(type));

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
        if (cb == null) return "Tous";
        String v = cb.getValue();
        return (v == null || v.isBlank()) ? "Tous" : v;
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    // ===============================
    // Rerank opportunities when strategy changes
    // ===============================

    private void rerankInvestorList() {
        if (investorAll.isEmpty()) return;

        List<FinItem> rescored = investorAll.stream().map(it -> {
            if (it.kind != FinKind.OPPORTUNITE) return it;

            double reste = Math.max(0, it.budget - it.totalLeve);
            ScoreResult sr = scoreOpportunityTextBased(it, reste, strategy);

            return new FinItem(it.kind, it.refId, it.title, it.subtitle, it.meta, it.description,
                    it.budget, it.totalLeve, sr.score, sr.explain);
        }).collect(Collectors.toList());

        rescored.sort(Comparator.comparingInt((FinItem x) -> x.score).reversed());
        investorAll.setAll(rescored);

        applySearchAndFilters();
    }

    // ===============================
    // Sidebar navigation
    // ===============================

    @FXML
    private void goDashboard() {
        switchScene("/fxml/dashboard.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newRoot = loader.load();

            Scene scene = root.getScene();
            if (scene != null) scene.setRoot(newRoot);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML private void goFinancement() { System.out.println("Vous êtes déjà sur Financement"); }
    @FXML private void goAccompagnement() { System.out.println("Navigation: Accompagnement"); }
    @FXML private void goEvenements() { System.out.println("Navigation: Événements"); }
    @FXML private void goCandidature() { System.out.println("Navigation: Candidature"); }
    @FXML private void logout() { System.out.println("Déconnexion..."); }

    // ===============================
    // Actions Financement
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

    @FXML
    private void resetFilters() {
        if (cbSector != null) cbSector.getSelectionModel().selectFirst();
        if (cbStage != null) cbStage.getSelectionModel().selectFirst();
        if (cbRange != null) cbRange.getSelectionModel().selectFirst();
        if (cbType != null) cbType.getSelectionModel().selectFirst();
        if (tfSearch != null) tfSearch.clear();
        applySearchAndFilters();
    }

    // ===============================
    // DB queries
    // ===============================

    private Map<Integer, ProjetRow> fetchProjets() throws Exception {
        String sql = "SELECT id_projet, titre, description, budget, statut FROM projet";
        Map<Integer, ProjetRow> map = new LinkedHashMap<>();

        // ⚠️ IMPORTANT: on NE ferme PAS la Connection ici (sinon elle devient "closed" partout)
        Connection c = MyDatabase.getInstance().getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ProjetRow p = new ProjetRow();
                p.idProjet = rs.getInt("id_projet");
                p.titre = rs.getString("titre");
                p.description = rs.getString("description");
                p.budget = rs.getDouble("budget");   // OK
                p.statut = rs.getString("statut");
                map.put(p.idProjet, p);
            }
        }
        return map;
    }

    private Map<Integer, AggInvest> fetchAggInvestissements() throws Exception {
        String sql = """
            SELECT id_projet,
                   COALESCE(SUM(CASE WHEN UPPER(statut)='FINANCE' THEN montantInvestissement ELSE 0 END),0) AS total_finance,
                   COUNT(*) AS nb_invest
            FROM investissement
            GROUP BY id_projet
            """;

        Map<Integer, AggInvest> map = new HashMap<>();

        // ⚠️ IMPORTANT: on NE ferme PAS la Connection ici
        Connection c = MyDatabase.getInstance().getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql);
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

        String sql1 = "SELECT COUNT(*) nb FROM projet WHERE UPPER(statut)='EN_ATTENTE'";
        String sql2 = "SELECT COUNT(*) nb FROM investissement WHERE UPPER(statut)='FINANCE'";
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
    // Portfolio DB
    // ===============================

    private double fetchTotalInvestedByUser(int userId) throws Exception {
        String sql = """
                SELECT COALESCE(SUM(montantInvestissement),0) total
                FROM investissement
                WHERE id_user=? AND UPPER(statut)='FINANCE'
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total");
            }
        }
    }

    private int fetchDealsCountByUser(int userId) throws Exception {
        String sql = """
                SELECT COUNT(*) nb
                FROM investissement
                WHERE id_user=? AND UPPER(statut)='FINANCE'
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    private int fetchDistinctProjectsInvestedByUser(int userId) throws Exception {
        String sql = """
                SELECT COUNT(DISTINCT id_projet) nb
                FROM investissement
                WHERE id_user=? AND UPPER(statut)='FINANCE'
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    // ===============================
    // Strategy table
    // ===============================

    private void ensureStrategyTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS investor_strategy (
                    id_user INT PRIMARY KEY,
                    budget_annuel DOUBLE DEFAULT 100000,
                    pref_sectors VARCHAR(255) DEFAULT '',
                    pref_stage VARCHAR(50) DEFAULT 'TOUS',
                    ticket_min DOUBLE DEFAULT 0,
                    ticket_max DOUBLE DEFAULT 100000,
                    risk_max INT DEFAULT 60,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {}
    }

    private InvestorStrategy fetchStrategy(int userId) {
        String sql = """
                SELECT budget_annuel, pref_sectors, pref_stage, ticket_min, ticket_max, risk_max
                FROM investor_strategy
                WHERE id_user=?
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                InvestorStrategy s = new InvestorStrategy();
                s.budgetAnnuel = rs.getDouble("budget_annuel");
                s.prefSectorsCsv = rs.getString("pref_sectors");
                s.prefStage = rs.getString("pref_stage");
                s.ticketMin = rs.getDouble("ticket_min");
                s.ticketMax = rs.getDouble("ticket_max");
                s.riskMax = rs.getInt("risk_max");
                return s;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void upsertStrategy(int userId, InvestorStrategy s) throws Exception {
        String sql = """
                INSERT INTO investor_strategy (id_user, budget_annuel, pref_sectors, pref_stage, ticket_min, ticket_max, risk_max)
                VALUES (?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    budget_annuel=VALUES(budget_annuel),
                    pref_sectors=VALUES(pref_sectors),
                    pref_stage=VALUES(pref_stage),
                    ticket_min=VALUES(ticket_min),
                    ticket_max=VALUES(ticket_max),
                    risk_max=VALUES(risk_max),
                    updated_at=CURRENT_TIMESTAMP
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDouble(2, s.budgetAnnuel);
            ps.setString(3, s.prefSectorsCsv);
            ps.setString(4, s.prefStage);
            ps.setDouble(5, s.ticketMin);
            ps.setDouble(6, s.ticketMax);
            ps.setInt(7, s.riskMax);
            ps.executeUpdate();
        }
    }

    // ===============================
    // Scoring
    // ===============================

    private ScoreResult scoreProjectForInvestor(ProjetRow p, AggInvest agg, InvestorStrategy s) {
        int score = 0;
        List<String> parts = new ArrayList<>();

        String text = (safe(p.titre) + " " + safe(p.description)).toLowerCase(Locale.ROOT);

        int sectorPts = 0;
        List<String> sectors = parseCsv(s.prefSectorsCsv);
        if (!sectors.isEmpty()) {
            boolean any = sectors.stream().anyMatch(sec -> text.contains(sec.toLowerCase(Locale.ROOT).trim()));
            sectorPts = any ? 30 : 0;
            if (sectorPts > 0) parts.add("+30 secteur");
        }

        int stagePts = 0;
        if (s.prefStage != null && !"TOUS".equalsIgnoreCase(s.prefStage)) {
            stagePts = text.contains(s.prefStage.toLowerCase(Locale.ROOT)) ? 20 : 0;
            if (stagePts > 0) parts.add("+20 stade");
        }

        double totalLeve = agg.totalFinance;
        double reste = Math.max(0, p.budget - totalLeve);

        int ticketPts = 0;
        if (reste > 0) {
            boolean inRange = reste >= s.ticketMin && reste <= s.ticketMax;
            if (inRange) ticketPts = 25;
            else {
                double dist = 0;
                if (reste < s.ticketMin) dist = s.ticketMin - reste;
                if (reste > s.ticketMax) dist = reste - s.ticketMax;
                double rel = (s.ticketMax > 0) ? Math.min(1.0, dist / Math.max(1.0, s.ticketMax)) : 1.0;
                ticketPts = (int) Math.max(0, 25 * (1.0 - rel));
            }
            parts.add("+" + ticketPts + " ticket");
        }

        int locPts = (text.contains("tunisie") || text.contains("tn")) ? 10 : 0;
        if (locPts > 0) parts.add("+10 localisation");

        int riskScore = estimateRisk(p, reste);
        int riskPts;
        if (riskScore <= s.riskMax) riskPts = 15;
        else {
            int over = riskScore - s.riskMax;
            riskPts = Math.max(0, 15 - over / 5);
        }
        parts.add("+" + riskPts + " risque");

        score = sectorPts + stagePts + ticketPts + locPts + riskPts;
        score = Math.max(0, Math.min(100, score));

        return new ScoreResult(score, String.join(", ", parts));
    }

    private ScoreResult scoreOpportunityTextBased(FinItem it, double reste, InvestorStrategy s) {
        int score = 0;
        List<String> parts = new ArrayList<>();

        String text = (safe(it.title) + " " + safe(it.description) + " " + safe(it.meta)).toLowerCase(Locale.ROOT);

        List<String> sectors = parseCsv(s.prefSectorsCsv);
        if (!sectors.isEmpty() && sectors.stream().anyMatch(sec -> text.contains(sec.toLowerCase(Locale.ROOT).trim()))) {
            score += 30;
            parts.add("+30 secteur");
        }

        if (s.prefStage != null && !"TOUS".equalsIgnoreCase(s.prefStage) && text.contains(s.prefStage.toLowerCase(Locale.ROOT))) {
            score += 20;
            parts.add("+20 stade");
        }

        int ticketPts = (reste > 0 && reste >= s.ticketMin && reste <= s.ticketMax) ? 25 : (reste > 0 ? 10 : 0);
        score += ticketPts;
        parts.add("+" + ticketPts + " ticket");

        if (text.contains("tunisie") || text.contains("tn")) {
            score += 10;
            parts.add("+10 localisation");
        }

        int riskScore = (reste >= 100000) ? 80 : (reste >= 50000 ? 60 : 35);
        int riskPts = (riskScore <= s.riskMax) ? 15 : 8;
        score += riskPts;
        parts.add("+" + riskPts + " risque");

        score = Math.max(0, Math.min(100, score));
        return new ScoreResult(score, String.join(", ", parts));
    }

    private int estimateRisk(ProjetRow p, double reste) {
        int r = 40;

        String statut = safe(p.statut).toUpperCase(Locale.ROOT);
        if ("FINANCE".equals(statut)) r -= 10;
        if ("REFUSE".equals(statut)) r += 20;

        String desc = safe(p.description).trim();
        if (desc.isBlank() || desc.length() < 40) r += 15;

        if (reste >= 100000) r += 20;
        else if (reste >= 50000) r += 10;

        return Math.max(0, Math.min(100, r));
    }

    // ===============================
    // Dialogs (forms)
    // ===============================

    private Optional<ProjetForm> showNewProjetDialog() {
        Dialog<ProjetForm> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle demande");
        dialog.setHeaderText("Créer un projet à financer");

        ButtonType okType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18;");

        Label hint = new Label("Remplis les informations ci-dessous. Les champs * sont obligatoires.");
        hint.setStyle("-fx-text-fill:#6c757d; -fx-font-size:12;");

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

        tfBudget.setTextFormatter(new TextFormatter<String>(change -> {
            String n = change.getControlNewText();
            if (n.isEmpty()) return change;
            return n.matches("\\d{0,12}([\\.,]\\d{0,2})?") ? change : null;
        }));

        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatut.getSelectionModel().selectFirst();
        cbStatut.getStyleClass().add("modern-input");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label lTitre = new Label("Titre *");       lTitre.getStyleClass().add("form-label");
        Label lDesc  = new Label("Description");   lDesc.getStyleClass().add("form-label");
        Label lBud   = new Label("Budget (DT) *"); lBud.getStyleClass().add("form-label");
        Label lStat  = new Label("Statut");        lStat.getStyleClass().add("form-label");

        grid.addRow(0, lTitre, tfTitre);
        grid.addRow(1, lDesc,  taDesc);
        grid.addRow(2, lBud,   tfBudget);
        grid.addRow(3, lStat,  cbStatut);

        tfTitre.setPrefWidth(420);
        tfBudget.setPrefWidth(200);
        cbStatut.setPrefWidth(200);

        Label lbErr = new Label();
        lbErr.getStyleClass().add("fieldError");
        lbErr.setVisible(false);
        lbErr.setManaged(false);

        card.getChildren().addAll(hint, grid, lbErr);
        dialog.getDialogPane().setContent(card);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.getStyleClass().add("btn-add");

        Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-refresh");

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

        Platform.runLater(validate);
        return dialog.showAndWait();
    }

    private Optional<InvestForm> showNewInvestDialog() {
        Dialog<InvestForm> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle offre (Investissement)");
        dialog.setHeaderText("Investir dans un projet existant");

        ButtonType ok = new ButtonType("Investir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        ComboBox<ProjetOption> cbProjet = new ComboBox<>();
        cbProjet.setPrefWidth(420);

        TextField tfMontant = new TextField();
        tfMontant.setPromptText("Ex: 25000");
        DatePicker dpDate = new DatePicker(LocalDate.now());

        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatut.getSelectionModel().selectFirst();

        cbProjet.getStyleClass().add("modern-input");
        tfMontant.getStyleClass().add("modern-input");
        dpDate.getStyleClass().add("modern-input");
        cbStatut.getStyleClass().add("modern-input");

        Label hint = new Label("");
        hint.getStyleClass().add("fieldError");
        hint.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label lProjet = new Label("Projet");          lProjet.getStyleClass().add("form-label");
        Label lMontant = new Label("Montant (DT)");   lMontant.getStyleClass().add("form-label");
        Label lDate = new Label("Date");              lDate.getStyleClass().add("form-label");
        Label lStatut = new Label("Statut");          lStatut.getStyleClass().add("form-label");

        grid.add(lProjet, 0, 0);   grid.add(cbProjet, 1, 0);
        grid.add(lMontant, 0, 1);  grid.add(tfMontant, 1, 1);
        grid.add(lDate, 0, 2);     grid.add(dpDate, 1, 2);
        grid.add(lStatut, 0, 3);   grid.add(cbStatut, 1, 3);
        grid.add(hint, 1, 4);

        dialog.getDialogPane().setContent(grid);

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

    // ===============================
    // Helpers
    // ===============================

    private static void showAlert(String title, String header, String content) {
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

    private boolean isDouble(String s) {
        try { Double.parseDouble(s.replace(",", ".")); return true; } catch (Exception e) { return false; }
    }

    private double parseDoubleSafe(String s, double def) {
        try {
            if (s == null) return def;
            String t = s.trim();
            if (t.isEmpty()) return def;
            return Double.parseDouble(t.replace(",", "."));
        } catch (Exception e) {
            return def;
        }
    }

    private String trimNumber(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format(Locale.US, "%.2f", v);
    }

    private List<String> parseCsv(String csv) {
        if (csv == null) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .limit(10)
                .collect(Collectors.toList());
    }

    // ===============================
    // Models
    // ===============================

    enum FinKind { PROJET, OPPORTUNITE, INFO }

    public static class FinItem {
        public final FinKind kind;
        public final int refId;
        public final String title;
        public final String subtitle;
        public final String meta;
        public final String description;
        public final double budget;
        public final double totalLeve;

        public final int score;
        public final String scoreExplain;

        public FinItem(FinKind kind, int refId, String title, String subtitle, String meta, String description,
                       double budget, double totalLeve, int score, String scoreExplain) {
            this.kind = kind;
            this.refId = refId;
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.description = description;
            this.budget = budget;
            this.totalLeve = totalLeve;
            this.score = score;
            this.scoreExplain = scoreExplain == null ? "" : scoreExplain;
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

    private static class ProjetOption {
        final int id;
        final String label;
        ProjetOption(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static class InvestorStrategy {
        double budgetAnnuel;
        String prefSectorsCsv;
        String prefStage;
        double ticketMin;
        double ticketMax;
        int riskMax;

        static InvestorStrategy defaults() {
            InvestorStrategy s = new InvestorStrategy();
            s.budgetAnnuel = 100_000;
            s.prefSectorsCsv = "FinTech, SaaS";
            s.prefStage = "TOUS";
            s.ticketMin = 5_000;
            s.ticketMax = 80_000;
            s.riskMax = 60;
            return s;
        }
    }

    private static class PortfolioStats {
        double budgetAnnuel;
        double totalInvesti;
        double budgetRestant;
        int nbDeals;
        int diversification;
    }

    private static class ScoreResult {
        final int score;
        final String explain;
        ScoreResult(int score, String explain) {
            this.score = score;
            this.explain = explain;
        }
    }

    // ===============================
    // Cell UI (cards)
    // ===============================

    private class FinItemCardCell extends ListCell<FinItem> {

        private final VBox root = new VBox(6);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Text meta = new Text();
        private final Text desc = new Text();

        private final Label scoreBadge = new Label();
        private final ProgressBar scoreBar = new ProgressBar(0);

        private final ToolBar actions = new ToolBar();
        private final Button btnView = new Button("Voir");
        private final Button btnWhy = new Button("Voir pourquoi");
        private final Button btnAction = new Button("Action");

        private final boolean startupMode;
        private final FinancementFrontController parent;

        FinItemCardCell(boolean startupMode, FinancementFrontController parent) {
            this.startupMode = startupMode;
            this.parent = parent;

            root.getStyleClass().add("card");
            title.getStyleClass().add("card-title");
            subtitle.getStyleClass().add("muted");
            meta.getStyleClass().add("card-meta");
            desc.getStyleClass().add("card-desc");

            scoreBadge.getStyleClass().add("card-badge");
            scoreBar.setPrefWidth(220);

            desc.wrappingWidthProperty().bind(root.widthProperty().subtract(24));
            meta.wrappingWidthProperty().bind(root.widthProperty().subtract(24));

            btnView.getStyleClass().add("btn-refresh");
            btnAction.getStyleClass().add("btn-add");
            btnWhy.getStyleClass().add("secondary-action");

            actions.getItems().addAll(btnView, new Separator(), btnWhy, new Separator(), btnAction);
            actions.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            root.getChildren().addAll(title, subtitle, meta, desc, scoreBadge, scoreBar, actions);

            btnView.setOnAction(e -> {
                FinItem item = getItem();
                if (item == null) return;

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projet_details.fxml"));
                    Parent root = loader.load();

                    // ✅ CSS: forcer l'ajout au niveau Scene (sinon Modena par défaut)
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(Objects.requireNonNull(
                            getClass().getResource("/css/theme.css")
                    ).toExternalForm());

                    Stage stage = new Stage();
                    stage.setTitle("Détails projet");
                    stage.setScene(scene);

                    ProjetDetailsController ctrl = loader.getController();
                    ctrl.init(item.refId, 8, stage::close); // userId=8 (ton CURRENT_USER_ID)
                    stage.show();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Erreur");
                    a.setHeaderText("Impossible d'ouvrir la page détails");
                    a.setContentText(ex.getMessage());
                    a.showAndWait();
                }
            });

            btnWhy.setOnAction(e -> {
                FinItem item = getItem();
                if (item == null) return;
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Pourquoi ce score ?");
                a.setHeaderText("Score " + item.score + "/100");
                a.setContentText(item.scoreExplain.isBlank() ? "Aucune explication." : item.scoreExplain);
                a.showAndWait();
            });

            btnAction.setOnAction(e -> {
                FinItem item = getItem();
                if (item != null) {
                    if (startupMode) System.out.println("Action Startup: gérer projet ID=" + item.refId);
                    else System.out.println("Action Investisseur: investir sur projet ID=" + item.refId);
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

            boolean showScore = (!startupMode && item.kind == FinKind.OPPORTUNITE);

            scoreBadge.setManaged(showScore);
            scoreBadge.setVisible(showScore);
            scoreBar.setManaged(showScore);
            scoreBar.setVisible(showScore);
            btnWhy.setManaged(showScore);
            btnWhy.setVisible(showScore);

            if (showScore) {
                scoreBadge.setText("Score " + item.score + "/100");
                scoreBar.setProgress(Math.max(0, Math.min(1, item.score / 100.0)));
            }

            btnAction.setText(startupMode ? "Gérer" : "Investir");
            setGraphic(root);
        }
    }



    private void openProjectDetails(int projectId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projet_details.fxml"));
            Parent root = loader.load();

            ProjetDetailsController ctrl = loader.getController();

            // stage actuel + ancienne scene pour retour
            Stage stage = (Stage) ((lvInvestorItems != null && lvInvestorItems.getScene() != null)
                    ? lvInvestorItems.getScene().getWindow()
                    : startupPanel.getScene().getWindow());

            Scene oldScene = stage.getScene();

            // ✅ init corrigé (3 params)
            ctrl.init(projectId, CURRENT_USER_ID, () -> stage.setScene(oldScene));

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Ouverture détails impossible", ex.getMessage());
        }
    }
    // ====== handlers appelés par le "domain-card" du FXML ======
    @FXML private void openRequestDetails() { System.out.println("Détails demande (card FXML)"); }
    @FXML private void editRequest() { System.out.println("Modifier demande (card FXML)"); }
    @FXML private void cancelRequest() { System.out.println("Annuler demande (card FXML)"); }
    @FXML private void openOfferDetails() { System.out.println("Détails offre (card FXML)"); }
    @FXML private void editOffer() { System.out.println("Modifier offre (card FXML)"); }
    @FXML private void disableOffer() { System.out.println("Désactiver offre (card FXML)"); }
}