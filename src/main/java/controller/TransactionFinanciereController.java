package controller;

import entities.GFinancement.Transaction_Financiere;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import services.FinancementService.TransactionFinanciereService;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionFinanciereController {

    // ===================== UI FORM =====================
    @FXML private TextField tfTxMontant;
    @FXML private DatePicker dpTxDate;
    @FXML private ComboBox<String> cbTxMode;
    @FXML private ComboBox<String> cbTxStatut;
    @FXML private ComboBox<InvItem> cbTxInvestissement;

    @FXML private Label lblTxToast;
    @FXML private Label errTxMontant, errTxDate, errTxMode, errTxStatut, errTxInvestissement;

    @FXML private Button btnModifierTx, btnSupprimerTx;

    // ===================== SEARCH / FILTER =====================
    @FXML private TextField tfSearchTx;
    @FXML private ComboBox<String> cbFilterTxStatut;
    @FXML private Label lblTxCount;

    // ===================== TABLE =====================
    @FXML private TableView<Transaction_Financiere> tableTransactions;
    @FXML private TableColumn<Transaction_Financiere, Integer> colTxId;
    @FXML private TableColumn<Transaction_Financiere, Double> colTxMontant;
    @FXML private TableColumn<Transaction_Financiere, String> colTxDate;
    @FXML private TableColumn<Transaction_Financiere, String> colTxMode;
    @FXML private TableColumn<Transaction_Financiere, String> colTxStatut;
    @FXML private TableColumn<Transaction_Financiere, String> colTxInvestissement;

    private final TransactionFinanciereService txService = new TransactionFinanciereService();

    private final ObservableList<Transaction_Financiere> master = FXCollections.observableArrayList();
    private final ObservableList<Transaction_Financiere> filtered = FXCollections.observableArrayList();

    // ✅ adapte si tes valeurs DB sont différentes
    private static final String[] TX_STATUTS = {"EN_ATTENTE", "VALIDEE", "REFUSEE"};
    private static final String[] TX_MODES   = {"CARTE", "VIREMENT", "ESPECES", "CHEQUE"};
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private void setError(Control field, Label errLabel, String msg) {
        field.pseudoClassStateChanged(ERROR, true);
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearError(Control field, Label errLabel) {
        field.pseudoClassStateChanged(ERROR, false);
        errLabel.setText("");
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    // ===================== INIT =====================
    @FXML
    public void initialize() {
        setupCombos();
        setupTable();
        setupSelection();
        setupSearchFilter();
        setupLiveValidation();

        loadInvestissements();
        refreshTransactions();
    }

    // ===================== SETUP =====================
    private void setupCombos() {
        cbTxMode.setItems(FXCollections.observableArrayList(TX_MODES));
        cbTxStatut.setItems(FXCollections.observableArrayList(TX_STATUTS));

        cbFilterTxStatut.setItems(FXCollections.observableArrayList());
        cbFilterTxStatut.getItems().add("TOUS");
        cbFilterTxStatut.getItems().addAll(TX_STATUTS);
        cbFilterTxStatut.getSelectionModel().selectFirst();

        cbTxInvestissement.setConverter(new StringConverter<>() {
            @Override public String toString(InvItem inv) { return inv == null ? "" : inv.label; }
            @Override public InvItem fromString(String s) { return null; }
        });
    }

    private void setupTable() {
        // ID (tu l’as mis prefWidth=0 dans FXML, c’est OK)
        colTxId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_transaction()));

        colTxMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantTransaction()));
        colTxDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_transaction()));
        colTxMode.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMode_paiement()));
        colTxStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut_transaction()));

        colTxInvestissement.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(getInvLabel(c.getValue().getId_investissement()))
        );

        // Statut coloré (réutilise tes classes CSS)
        colTxStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                if (empty || statut == null) {
                    setText(null);
                    return;
                }
                setText(statut);

                switch (statut) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "VALIDEE"    -> getStyleClass().add("statut-finance");
                    case "REFUSEE"    -> getStyleClass().add("statut-refuse");
                }
            }
        });

        tableTransactions.setItems(filtered);
    }

    private void setupSelection() {
        btnModifierTx.setDisable(true);
        btnSupprimerTx.setDisable(true);

        tableTransactions.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean has = sel != null;
            btnModifierTx.setDisable(!has);
            btnSupprimerTx.setDisable(!has);

            if (!has) return;

            tfTxMontant.setText(String.valueOf(sel.getMontantTransaction()));
            cbTxMode.getSelectionModel().select(sel.getMode_paiement());
            cbTxStatut.getSelectionModel().select(sel.getStatut_transaction());

            try {
                dpTxDate.setValue(LocalDate.parse(sel.getDate_transaction()));
            } catch (Exception e) {
                dpTxDate.setValue(null);
            }

            cbTxInvestissement.getSelectionModel().select(
                    cbTxInvestissement.getItems().stream()
                            .filter(i -> i.id == sel.getId_investissement())
                            .findFirst().orElse(null)
            );
        });
    }

    private void setupSearchFilter() {
        tfSearchTx.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterTxStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupLiveValidation() {
        tfTxMontant.textProperty().addListener((obs, o, n) -> validateMontantLive());
        dpTxDate.valueProperty().addListener((obs, o, n) -> validateDateLive());
        cbTxMode.valueProperty().addListener((obs, o, n) -> validateModeLive());
        cbTxStatut.valueProperty().addListener((obs, o, n) -> validateStatutLive());
        cbTxInvestissement.valueProperty().addListener((obs, o, n) -> validateInvestissementLive());
    }

    // ===================== ACTIONS (NOMS = FXML) =====================

    @FXML
    private void ajouterTransaction() {
        clearErrors();
        if (!validateForm()) return;

        try {
            Transaction_Financiere tx = buildFromForm(0);
            txService.ajouter(tx);
            showToast("✅ Transaction ajoutée.", "toastSuccess");
            refreshTransactions();
            clearTransactionForm();
        } catch (SQLException e) {
            showToast("❌ Erreur ajout: " + e.getMessage(), "toastError");
        }
    }

    @FXML
    private void modifierTransaction() {
        Transaction_Financiere selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        clearErrors();
        if (!validateForm()) return;

        try {
            Transaction_Financiere tx = buildFromForm(selected.getId_transaction());
            txService.update(tx);
            showToast("✅ Transaction modifiée.", "toastSuccess");
            refreshTransactions();
        } catch (SQLException e) {
            showToast("❌ Erreur modification: " + e.getMessage(), "toastError");
        }
    }

    @FXML
    private void supprimerTransaction() {
        Transaction_Financiere selected = tableTransactions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette transaction ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    txService.supprimer(selected.getId_transaction());
                    showToast("🗑️ Transaction supprimée.", "toastInfo");
                    refreshTransactions();
                    clearTransactionForm();
                } catch (SQLException e) {
                    showToast("❌ Erreur suppression: " + e.getMessage(), "toastError");
                }
            }
        });
    }

    @FXML
    private void refreshTransactions() {
        try {
            master.setAll(txService.read());
            applyFilters();
            updateCount();
        } catch (SQLException e) {
            showToast("❌ Erreur chargement: " + e.getMessage(), "toastError");
        }
    }

    @FXML
    private void clearTransactionForm() {
        tfTxMontant.clear();
        dpTxDate.setValue(null);
        cbTxMode.getSelectionModel().clearSelection();
        cbTxStatut.getSelectionModel().clearSelection();
        cbTxInvestissement.getSelectionModel().clearSelection();
        tableTransactions.getSelectionModel().clearSelection();
        clearErrors();
    }

    @FXML
    private void clearSearchTx() {
        tfSearchTx.clear();
        cbFilterTxStatut.getSelectionModel().selectFirst(); // TOUS
    }

    // ===================== FILTER =====================

    private void applyFilters() {
        String q = tfSearchTx.getText() == null ? "" : tfSearchTx.getText().trim().toLowerCase(Locale.ROOT);
        String statutFilter = cbFilterTxStatut.getValue();

        filtered.setAll(master.stream().filter(tx -> {
            boolean okStatut = (statutFilter == null || "TOUS".equals(statutFilter)) ||
                    Objects.equals(tx.getStatut_transaction(), statutFilter);

            boolean okQuery = q.isBlank()
                    || String.valueOf(tx.getMontantTransaction()).toLowerCase(Locale.ROOT).contains(q)
                    || (tx.getMode_paiement() != null && tx.getMode_paiement().toLowerCase(Locale.ROOT).contains(q))
                    || (tx.getDate_transaction() != null && tx.getDate_transaction().toLowerCase(Locale.ROOT).contains(q))
                    || (tx.getStatut_transaction() != null && tx.getStatut_transaction().toLowerCase(Locale.ROOT).contains(q))
                    || getInvLabel(tx.getId_investissement()).toLowerCase(Locale.ROOT).contains(q);

            return okStatut && okQuery;
        }).collect(Collectors.toList()));
        updateCount();
    }

    private void updateCount() {
        lblTxCount.setText("Résultats: " + filtered.size() + " / Total: " + master.size());
    }

    // ===================== VALIDATION =====================

    private boolean validateForm() {
        boolean ok = true;
        if (!validateMontantLive()) ok = false;
        if (!validateDateLive()) ok = false;
        if (!validateModeLive()) ok = false;
        if (!validateStatutLive()) ok = false;
        if (!validateInvestissementLive()) ok = false;
        return ok;
    }

    private boolean validateMontantLive() {
        String m = tfTxMontant.getText() == null ? "" : tfTxMontant.getText().trim();
        try {
            double montant = Double.parseDouble(m.replace(",", "."));
            if (montant <= 0) throw new NumberFormatException();
            hideError(errTxMontant);
            return true;
        } catch (Exception e) {
            showError(errTxMontant, "Montant invalide (doit être > 0).");
            return false;
        }
    }

    private boolean validateDateLive() {
        if (dpTxDate.getValue() == null) {
            showError(errTxDate, "Date obligatoire.");
            return false;
        }
        hideError(errTxDate);
        return true;
    }

    private boolean validateModeLive() {
        if (cbTxMode.getValue() == null || cbTxMode.getValue().isBlank()) {
            showError(errTxMode, "Mode obligatoire.");
            return false;
        }
        hideError(errTxMode);
        return true;
    }

    private boolean validateStatutLive() {
        if (cbTxStatut.getValue() == null || cbTxStatut.getValue().isBlank()) {
            showError(errTxStatut, "Statut obligatoire.");
            return false;
        }
        hideError(errTxStatut);
        return true;
    }

    private boolean validateInvestissementLive() {
        if (cbTxInvestissement.getValue() == null) {
            showError(errTxInvestissement, "Investissement obligatoire.");
            return false;
        }
        hideError(errTxInvestissement);
        return true;
    }

    private Transaction_Financiere buildFromForm(int id) {
        double montant = Double.parseDouble(tfTxMontant.getText().trim().replace(",", "."));
        String dateStr = dpTxDate.getValue().toString(); // yyyy-MM-dd
        String mode = cbTxMode.getValue();
        String statut = cbTxStatut.getValue();
        int idInv = cbTxInvestissement.getValue().id;

        return new Transaction_Financiere(id, montant, dateStr, mode, statut, idInv);
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void clearErrors() {
        Label[] errs = {errTxMontant, errTxDate, errTxMode, errTxStatut, errTxInvestissement};
        for (Label l : errs) hideError(l);
    }

    // ===================== TOAST =====================

    private void showToast(String msg, String cssClass) {
        lblTxToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblTxToast.getStyleClass().add(cssClass);

        lblTxToast.setText(msg);
        lblTxToast.setVisible(true);
        lblTxToast.setManaged(true);
    }

    // ===================== INVESTISSEMENTS COMBO (DB) =====================

    private void loadInvestissements() {
        String sql = "SELECT id_investissement, montantInvestissement FROM investissement ORDER BY id_investissement DESC";

        ObservableList<InvItem> invs = FXCollections.observableArrayList();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_investissement");
                double montant = rs.getDouble("montantInvestissement");
                invs.add(new InvItem(id, "Invest #" + id + " - " + montant));
            }
        } catch (Exception e) {
            // fallback si table pas prête
        }

        cbTxInvestissement.setItems(invs);
    }

    private String getInvLabel(int idInv) {
        InvItem i = cbTxInvestissement.getItems().stream().filter(x -> x.id == idInv).findFirst().orElse(null);
        return i == null ? ("Invest #" + idInv) : i.label;
    }

    // ===================== SMALL DTO =====================
    public static class InvItem {
        public final int id;
        public final String label;
        public InvItem(int id, String label) { this.id = id; this.label = label; }
    }
}
