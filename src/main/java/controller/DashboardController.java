package controller;

import entities.GFinancement.Transaction_Financiere;
import entities.Api.NewsItem;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import services.api.ExchangeRateService;
import services.api.OpenRouterDashboardService;
import services.api.RssNewsService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class DashboardController {

    // Top
    @FXML private Label lblAdminEmail;
    @FXML private BorderPane root;

    // Stats
    @FXML private Label lblCountProjets;
    @FXML private Label lblCountInv;
    @FXML private Label lblCountTx;

    // Last Tx table
    @FXML private TableView<Transaction_Financiere> tableLastTx;
    @FXML private TableColumn<Transaction_Financiere, Integer> colLastTxId;
    @FXML private TableColumn<Transaction_Financiere, Double> colLastTxMontant;
    @FXML private TableColumn<Transaction_Financiere, String> colLastTxDate;
    @FXML private TableColumn<Transaction_Financiere, String> colLastTxMode;
    @FXML private TableColumn<Transaction_Financiere, String> colLastTxStatut;
    @FXML private Label lblLastTxCount;

    // Alerts
    @FXML private ListView<String> listBudgetAlerts;
    @FXML private Label lblAlertsHint;

    // AI
    @FXML private TextArea taAiSummary;
    @FXML private Label lblAiStatus;

    // Exchange Rate
    @FXML private Label lblEurUsdRate;
    @FXML private TextField tfEurAmount;
    @FXML private Label lblUsdResult;
    @FXML private Label lblRateStatus;

    // NEWS (RSS)
    @FXML private TextField tfNewsQuery;
    @FXML private ListView<NewsItem> listNews;
    @FXML private Label lblNewsStatus;

    private final ObservableList<Transaction_Financiere> lastTx = FXCollections.observableArrayList();

    private HostServices hostServices;
    public void setHostServices(HostServices hostServices) { this.hostServices = hostServices; }

    @FXML
    public void initialize() {
        setupLastTxTable();
        refreshStats();
        loadMockDashboardData(); // ✅ remplace par DB après

        refreshRate(null);        // charge taux au démarrage
        setupNewsOpenOnDoubleClick();
        refreshNews();            // charge news au démarrage
    }

    // ================= NEWS =================

    @FXML
    private void refreshNews() {
        String query = (tfNewsQuery != null && !tfNewsQuery.getText().isBlank())
                ? tfNewsQuery.getText().trim()
                : "financement tunisie";

        if (lblNewsStatus != null) lblNewsStatus.setText("Chargement news...");
        if (listNews != null) listNews.getItems().clear();

        Task<List<NewsItem>> task = new Task<>() {
            @Override
            protected List<NewsItem> call() throws Exception {
                return RssNewsService.search(query, 12);
            }
        };

        task.setOnSucceeded(e -> {
            List<NewsItem> items = task.getValue();
            if (listNews != null) listNews.getItems().setAll(items);
            if (lblNewsStatus != null) lblNewsStatus.setText("✅ " + items.size() + " news");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (lblNewsStatus != null) {
                lblNewsStatus.setText("❌ Erreur news: " + (ex == null ? "inconnue" : ex.getMessage()));
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void setupNewsOpenOnDoubleClick() {
        if (listNews == null) return;

        listNews.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                NewsItem selected = listNews.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                String url = selected.getLink();
                if (url == null || url.isBlank()) return;

                // ✅ Le plus fiable dans JavaFX
                if (hostServices != null) {
                    hostServices.showDocument(url);
                    return;
                }

                // fallback Desktop
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI.create(url));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ================= EXCHANGE RATE =================

    @FXML
    private void refreshRate(ActionEvent e) {
        if (lblRateStatus != null) lblRateStatus.setText("Chargement du taux...");
        if (lblEurUsdRate != null) lblEurUsdRate.setText("...");

        Task<Double> task = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return ExchangeRateService.eurToUsdRate();
            }
        };

        task.setOnSucceeded(ev -> {
            double rate = task.getValue();
            if (lblEurUsdRate != null) lblEurUsdRate.setText(String.format("%.4f", rate));
            if (lblRateStatus != null) lblRateStatus.setText("✅ Taux mis à jour.");
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            if (lblRateStatus != null) {
                lblRateStatus.setText("❌ Erreur taux: " + (ex == null ? "inconnue" : ex.getMessage()));
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void convertToUsd(ActionEvent e) {
        String s = (tfEurAmount != null) ? tfEurAmount.getText() : null;

        if (s == null || s.trim().isEmpty()) {
            if (lblUsdResult != null) lblUsdResult.setText("Entre un montant EUR.");
            return;
        }

        double eur;
        try {
            eur = Double.parseDouble(s.trim().replace(",", "."));
        } catch (Exception ex) {
            if (lblUsdResult != null) lblUsdResult.setText("Montant invalide.");
            return;
        }

        if (lblUsdResult != null) lblUsdResult.setText("Conversion...");
        if (lblRateStatus != null) lblRateStatus.setText("Calcul...");

        Task<Double> task = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return ExchangeRateService.convertEurToUsd(eur);
            }
        };

        task.setOnSucceeded(ev -> {
            double usd = task.getValue();
            if (lblUsdResult != null) lblUsdResult.setText(String.format("%.2f USD", usd));
            if (lblRateStatus != null) lblRateStatus.setText("✅ Conversion OK.");
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            if (lblRateStatus != null) {
                lblRateStatus.setText("❌ Erreur conversion: " + (ex == null ? "inconnue" : ex.getMessage()));
            }
            if (lblUsdResult != null) lblUsdResult.setText("");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ================= NAVIGATION =================

    @FXML
    private void goHomePage() {
        switchScene("/fxml/financement_front.fxml");
    }

    @FXML
    private void goDashboard() {
        refreshStats();
        loadMockDashboardData();
    }

    @FXML
    private void goProjets(ActionEvent e) {
        openFinancementAndSelect(0);
    }

    @FXML
    private void goInvestissements() {
        openFinancementAndSelect(1);
    }

    @FXML
    private void goTransactions() {
        openFinancementAndSelect(2);
    }

    @FXML
    private void goUsers() {
        System.out.println("TODO: users-management.fxml");
    }

    @FXML private void goMessages(ActionEvent e) {}
    @FXML private void goNotifications(ActionEvent e) {}
    @FXML private void goProfil(ActionEvent e) {}

    @FXML
    private void logout() {
        System.out.println("Logout");
    }

    // ================= TABLE SETUP =================

    private void setupLastTxTable() {
        if (tableLastTx == null || colLastTxId == null) return;

        colLastTxId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_transaction()));
        colLastTxMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantTransaction()));
        colLastTxDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_transaction()));
        colLastTxMode.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMode_paiement()));
        colLastTxStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut_transaction()));

        colLastTxStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                setText(null);
                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");

                if (empty || statut == null) return;

                setText(statut);

                switch (statut.toUpperCase()) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "VALIDEE", "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSEE", "REFUSE" -> getStyleClass().add("statut-refuse");
                    default -> { }
                }
            }
        });

        tableLastTx.setItems(lastTx);
        tableLastTx.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ================= DATA =================

    @FXML
    private void refreshStats() {
        if (lblCountProjets != null) lblCountProjets.setText("12");
        if (lblCountInv != null) lblCountInv.setText("7");
        if (lblCountTx != null) lblCountTx.setText("19");
    }

    private void loadMockDashboardData() {
        lastTx.setAll(List.of(
                new Transaction_Financiere(101, 450.0, "2026-02-12", "VIREMENT", "VALIDEE", 5),
                new Transaction_Financiere(102, 120.0, "2026-02-13", "CARTE", "EN_ATTENTE", 3),
                new Transaction_Financiere(103, 980.0, "2026-02-14", "CHEQUE", "REFUSEE", 2)
        ));

        if (lblLastTxCount != null) lblLastTxCount.setText(lastTx.size() + " affichées");

        if (listBudgetAlerts != null) {
            listBudgetAlerts.setItems(FXCollections.observableArrayList(
                    "Projet A — 92% du budget consommé",
                    "Projet B — dépassement imminent (97%)",
                    "Projet C — budget faible restant (8%)"
            ));
        }

        if (lblAlertsHint != null) {
            lblAlertsHint.setText("Astuce : connecte ces alertes à tes projets (budget vs investissement).");
        }
    }

    // ================= SCENE HELPERS =================

    private void openFinancementAndSelect(int tabIndex) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/financement.fxml"));
            Parent newRoot = loader.load();

            FinancementController controller = loader.getController();
            if (controller != null) controller.selectTab(tabIndex);

            Scene scene = root.getScene();
            if (scene == null) return;

            scene.setRoot(newRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private Stage getCurrentStage() {
        if (lblCountTx != null && lblCountTx.getScene() != null) {
            return (Stage) lblCountTx.getScene().getWindow();
        }
        if (!Stage.getWindows().isEmpty()) {
            return (Stage) Stage.getWindows().filtered(w -> w.isShowing()).get(0);
        }
        return null;
    }

    // ================= AI SUMMARY =================

    @FXML
    private void generateAiSummary() {
        if (lblAiStatus != null) lblAiStatus.setText("Génération du résumé IA...");
        if (taAiSummary != null) taAiSummary.clear();

        String projets = (lblCountProjets != null) ? lblCountProjets.getText() : "?";
        String inv = (lblCountInv != null) ? lblCountInv.getText() : "?";
        String tx = (lblCountTx != null) ? lblCountTx.getText() : "?";

        String lastTxText = lastTx.stream()
                .limit(6)
                .map(t -> "- #" + t.getId_transaction() + " | " + t.getMontantTransaction() + " | " +
                        t.getDate_transaction() + " | " + t.getMode_paiement() + " | " + t.getStatut_transaction())
                .reduce("", (a, b) -> a + b + "\n");

        String alerts = (listBudgetAlerts != null && listBudgetAlerts.getItems() != null)
                ? listBudgetAlerts.getItems().stream().limit(6).reduce("", (a, b) -> a + "- " + b + "\n")
                : "";

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return OpenRouterDashboardService.generateSummary(projets, inv, tx, lastTxText, alerts);
            }
        };

        task.setOnSucceeded(e -> {
            if (taAiSummary != null) taAiSummary.setText(task.getValue());
            if (lblAiStatus != null) lblAiStatus.setText("✅ Résumé généré.");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (lblAiStatus != null) {
                lblAiStatus.setText("❌ Erreur IA: " + (ex == null ? "inconnue" : ex.getMessage()));
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}