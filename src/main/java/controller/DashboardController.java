package controller;

import entities.GFinancement.Transaction_Financiere;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    // Top
    @FXML private Label lblAdminEmail;

    // Stats
    @FXML private Label lblCountProjets;
    @FXML private Label lblCountInv;
    @FXML private Label lblCountTx;

    // Last Tx table (unique)
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

    private final ObservableList<Transaction_Financiere> lastTx = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupLastTxTable();
        refreshStats();
        loadMockDashboardData(); // ✅ remplace par DB après
    }

    // ================= NAVIGATION =================

    @FXML
    private void goHomePage() {
        switchScene("/fxml/financement_front.fxml");
    }

    @FXML
    private void goDashboard() {
        // On est déjà sur Dashboard => refresh
        refreshStats();
        loadMockDashboardData();
    }

    @FXML
    private void goFinancementModule() {
        openFinancementAndSelect(0); // 0 = Projets
    }

    @FXML
    private void goInvestissements() {
        openFinancementAndSelect(1); // 1 = Investissements
    }

    @FXML
    private void goTransactions() {
        openFinancementAndSelect(2); // 2 = Transactions
    }

    @FXML
    private void goUsers() {
        System.out.println("TODO: users-management.fxml");
        // switchScene("/fxml/users-management.fxml");
    }

    @FXML
    private void logout() {
        System.out.println("Logout");
        // switchScene("/fxml/login.fxml");
    }

    // ================= TABLE SETUP =================

    private void setupLastTxTable() {
        if (tableLastTx == null || colLastTxId == null) return;

        colLastTxId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_transaction()));
        colLastTxMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantTransaction()));
        colLastTxDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_transaction()));
        colLastTxMode.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMode_paiement()));
        colLastTxStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut_transaction()));

        // Badge statut (classes CSS)
        colLastTxStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                // reset
                setText(null);
                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");

                if (empty || statut == null) return;

                setText(statut);

                switch (statut.toUpperCase()) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "VALIDEE", "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSEE", "REFUSE" -> getStyleClass().add("statut-refuse");
                    default -> { /* rien */ }
                }
            }
        });

        tableLastTx.setItems(lastTx);
        tableLastTx.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ================= DATA =================

    @FXML
    private void refreshStats() {
        // TODO: remplacer par DB
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

        if (lblLastTxCount != null) {
            lblLastTxCount.setText(lastTx.size() + " affichées");
        }

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
            Parent root = loader.load();

            // IMPORTANT: FinancementController doit avoir selectTab(int)
            FinancementController controller = loader.getController();
            if (controller != null) controller.selectTab(tabIndex);

            Stage stage = getCurrentStage();
            if (stage == null) return;

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = getCurrentStage();
            if (stage == null) return;

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getCurrentStage() {
        // le plus fiable: prendre une node connue du FXML
        if (lblCountTx != null && lblCountTx.getScene() != null) {
            return (Stage) lblCountTx.getScene().getWindow();
        }
        // fallback (moins propre mais marche)
        if (!Stage.getWindows().isEmpty()) {
            return (Stage) Stage.getWindows().filtered(w -> w.isShowing()).get(0);
        }
        return null;
    }
}
