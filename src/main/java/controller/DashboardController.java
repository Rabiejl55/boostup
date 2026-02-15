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

    private final ObservableList<Transaction_Financiere> lastTx = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupLastTxTable();
        refreshStats();
        loadMockDashboardData(); // ✅ remplace par DB après
    }

    private void setupLastTxTable() {
        // sécurité : si FXML pas à jour
        if (colLastTxId == null) return;

        colLastTxId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_transaction()));
        colLastTxMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantTransaction()));
        colLastTxDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_transaction()));
        colLastTxMode.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMode_paiement()));
        colLastTxStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut_transaction()));

        // joli badge statut (réutilise tes classes)
        colLastTxStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                if (empty || statut == null) { setText(null); return; }

                setText(statut);
                switch (statut.toUpperCase()) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "VALIDEE", "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSEE", "REFUSE" -> getStyleClass().add("statut-refuse");
                }
            }
        });

        tableLastTx.setItems(lastTx);
        tableLastTx.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void goDashboard() {
        refreshStats();
        loadMockDashboardData();
    }

    @FXML
    private void goFinancementModule() {
        switchScene("/fxml/financement.fxml");
    }

    @FXML
    private void goInvestissements() {
        // si tu as une page dédiée, mets son fxml ici (ou ouvre financement tab investissements)
        switchScene("/fxml/investissement.fxml");
    }

    @FXML
    private void goTransactions() {
        // idem
        switchScene("/fxml/transaction.fxml");
    }

    @FXML
    private void goUsers() {
        // switchScene("/fxml/users-management.fxml");
        System.out.println("TODO: users-management.fxml");
    }

    @FXML
    private void logout() {
        // switchScene("/fxml/login.fxml");
        System.out.println("Logout");
    }

    @FXML
    private void refreshStats() {
        // TODO: remplacer par DB
        lblCountProjets.setText("12");
        lblCountInv.setText("7");
        lblCountTx.setText("19");
    }

    private void loadMockDashboardData() {
        // Dernières transactions (mock)
        lastTx.setAll(List.of(
                new Transaction_Financiere(101, 450.0, "2026-02-12", "VIREMENT", "VALIDEE", 5),
                new Transaction_Financiere(102, 120.0, "2026-02-13", "CARTE", "EN_ATTENTE", 3),
                new Transaction_Financiere(103, 980.0, "2026-02-14", "CHEQUE", "REFUSEE", 2)
        ));
        if (lblLastTxCount != null) lblLastTxCount.setText(lastTx.size() + " affichées");

        // Alertes budget (mock)
        if (listBudgetAlerts != null) {
            listBudgetAlerts.setItems(FXCollections.observableArrayList(
                    "Projet A — 92% du budget consommé",
                    "Projet B — dépassement imminent (97%)",
                    "Projet C — budget faible restant (8%)"
            ));
        }
        if (lblAlertsHint != null) lblAlertsHint.setText("Astuce : connecte ces alertes à tes projets (budget vs investissement).");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) Stage.getWindows().filtered(w -> w.isShowing()).get(0);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
