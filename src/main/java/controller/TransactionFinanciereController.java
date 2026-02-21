package controller;

import entities.GFinancement.Transaction_Financiere;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import services.FinancementService.TransactionFinanciereService;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionFinanciereController {

    // ===================== UI FORM (sert pour AJOUT)
    @FXML private TextField tfTxMontant;
    @FXML private DatePicker dpTxDate;
    @FXML private ComboBox<String> cbTxMode;
    @FXML private ComboBox<String> cbTxStatut;
    @FXML private ComboBox<InvItem> cbTxInvestissement;

    @FXML private Label lblTxToast;
    @FXML private Label errTxMontant, errTxDate, errTxMode, errTxStatut, errTxInvestissement;

    @FXML private Button btnModifierTx, btnSupprimerTx;

    // ===================== SEARCH / FILTER
    @FXML private TextField tfSearchTx;
    @FXML private ComboBox<String> cbFilterTxStatut;
    @FXML private Label lblTxCount;

    // ===================== TABLE
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

    // ===================== INIT
    @FXML
    public void initialize() {
        setupCombos();
        setupTable();
        setupInlineEditing();   // ✅ NEW
        setupSelection();       // ✅ modifié
        setupSearchFilter();
        setupLiveValidation();  // pour AJOUT

        loadInvestissements();
        refreshTransactions();

        // ✅ cacher bouton modifier (modif = table)
        if (btnModifierTx != null) {
            btnModifierTx.setDisable(true);
            btnModifierTx.setVisible(false);
            btnModifierTx.setManaged(false);
        }
    }

    // ===================== SETUP
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
        colTxId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_transaction()));

        colTxMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantTransaction()));
        colTxDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_transaction()));
        colTxMode.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMode_paiement()));
        colTxStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut_transaction()));

        colTxInvestissement.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(getInvLabel(c.getValue().getId_investissement()))
        );

        // Statut coloré (display)
        colTxStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                if (empty || statut == null) { setText(null); return; }

                setText(statut);
                switch (statut) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "VALIDEE"    -> getStyleClass().add("statut-finance");
                    case "REFUSEE"    -> getStyleClass().add("statut-refuse");
                }
            }
        });

        tableTransactions.setItems(filtered);
        tableTransactions.setEditable(true); // ✅ IMPORTANT
    }

    // ===================== INLINE EDITING (✅ NEW)
    private void setupInlineEditing() {

        // 1) Montant
        colTxMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colTxMontant.setOnEditCommit(ev -> {
            Transaction_Financiere tx = ev.getRowValue();
            Double nv = ev.getNewValue();

            if (nv == null || nv <= 0) {
                showToast("❌ Montant invalide (>0).", "toastError");
                tableTransactions.refresh();
                return;
            }

            tx.setMontantTransaction(nv);
            saveInline(tx, "✅ Montant modifié.");
        });

        // 2) Date (édition texte yyyy-MM-dd)
        colTxDate.setCellFactory(TextFieldTableCell.forTableColumn());
        colTxDate.setOnEditCommit(ev -> {
            Transaction_Financiere tx = ev.getRowValue();
            String nv = ev.getNewValue() == null ? "" : ev.getNewValue().trim();

            try {
                LocalDate d = LocalDate.parse(nv);
                if (d.isAfter(LocalDate.now())) {
                    showToast("❌ Date future interdite.", "toastError");
                    tableTransactions.refresh();
                    return;
                }

                tx.setDate_transaction(d.toString());
                saveInline(tx, "✅ Date modifiée.");
            } catch (Exception ex) {
                showToast("❌ Date invalide (yyyy-MM-dd).", "toastError");
                tableTransactions.refresh();
            }
        });

        // 3) Mode paiement (combo)
        colTxMode.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(TX_MODES)
        ));
        colTxMode.setOnEditCommit(ev -> {
            Transaction_Financiere tx = ev.getRowValue();
            String nv = ev.getNewValue();

            if (nv == null || nv.isBlank()) {
                tableTransactions.refresh();
                return;
            }

            tx.setMode_paiement(nv);
            saveInline(tx, "✅ Mode modifié.");
        });

        // 4) Statut (combo)
        colTxStatut.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(TX_STATUTS)
        ));
        colTxStatut.setOnEditCommit(ev -> {
            Transaction_Financiere tx = ev.getRowValue();
            String nv = ev.getNewValue();

            if (nv == null || nv.isBlank()) {
                tableTransactions.refresh();
                return;
            }

            tx.setStatut_transaction(nv);
            saveInline(tx, "✅ Statut modifié.");
        });

        // 5) Investissement : on laisse affiché en String (saisie label exact)
        // Tu peux écrire : "Invest #12 - 500.0" (exact)
        colTxInvestissement.setCellFactory(TextFieldTableCell.forTableColumn());
        colTxInvestissement.setOnEditCommit(ev -> {
            Transaction_Financiere tx = ev.getRowValue();
            String nv = ev.getNewValue() == null ? "" : ev.getNewValue().trim().toLowerCase(Locale.ROOT);

            InvItem match = cbTxInvestissement.getItems().stream()
                    .filter(i -> i.label != null && i.label.toLowerCase(Locale.ROOT).equals(nv))
                    .findFirst().orElse(null);

            if (match == null) {
                showToast("❌ Investissement introuvable (saisir le label exact).", "toastError");
                tableTransactions.refresh();
                return;
            }

            tx.setId_investissement(match.id);
            saveInline(tx, "✅ Investissement modifié.");
        });
    }

    private void saveInline(Transaction_Financiere tx, String okMsg) {
        try {
            txService.update(tx);
            showToast(okMsg, "toastSuccess");
            refreshTransactions();
        } catch (SQLException e) {
            showToast("❌ Erreur update: " + e.getMessage(), "toastError");
            tableTransactions.refresh();
        }
    }

    // ===================== SELECTION (✅ modifié)
    private void setupSelection() {
        btnSupprimerTx.setDisable(true);
        tableTransactions.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            btnSupprimerTx.setDisable(sel == null);
        });
    }

    private void setupSearchFilter() {
        tfSearchTx.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterTxStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ===================== VALIDATION (form add)
    private void setupLiveValidation() {
        tfTxMontant.textProperty().addListener((obs, o, n) -> validateMontantLive());
        dpTxDate.valueProperty().addListener((obs, o, n) -> validateDateLive());
        cbTxMode.valueProperty().addListener((obs, o, n) -> validateModeLive());
        cbTxStatut.valueProperty().addListener((obs, o, n) -> validateStatutLive());
        cbTxInvestissement.valueProperty().addListener((obs, o, n) -> validateInvestissementLive());
    }

    // ===================== ACTIONS
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

    // ✅ plus utilisé (modif = table)
    @FXML
    private void modifierTransaction() {
        showToast("ℹ️ Modification via la table (double-clic).", "toastInfo");
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
        cbFilterTxStatut.getSelectionModel().selectFirst();
    }

    // ===================== FILTER
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

    // ===================== VALIDATION (form add)
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
            clearError(tfTxMontant, errTxMontant);
            return true;
        } catch (Exception e) {
            setError(tfTxMontant, errTxMontant, "Montant invalide (doit être > 0).");
            return false;
        }
    }

    private boolean validateDateLive() {
        if (dpTxDate.getValue() == null || dpTxDate.getValue().isAfter(LocalDate.now())) {
            setError(dpTxDate, errTxDate, "Date obligatoire (et non future).");
            return false;
        }
        clearError(dpTxDate, errTxDate);
        return true;
    }

    private boolean validateModeLive() {
        if (cbTxMode.getValue() == null || cbTxMode.getValue().isBlank()) {
            setError(cbTxMode, errTxMode, "Mode obligatoire.");
            return false;
        }
        clearError(cbTxMode, errTxMode);
        return true;
    }

    private boolean validateStatutLive() {
        if (cbTxStatut.getValue() == null || cbTxStatut.getValue().isBlank()) {
            setError(cbTxStatut, errTxStatut, "Statut obligatoire.");
            return false;
        }
        clearError(cbTxStatut, errTxStatut);
        return true;
    }

    private boolean validateInvestissementLive() {
        if (cbTxInvestissement.getValue() == null) {
            setError(cbTxInvestissement, errTxInvestissement, "Investissement obligatoire.");
            return false;
        }
        clearError(cbTxInvestissement, errTxInvestissement);
        return true;
    }

    private Transaction_Financiere buildFromForm(int id) {
        double montant = Double.parseDouble(tfTxMontant.getText().trim().replace(",", "."));
        String dateStr = dpTxDate.getValue().toString();
        String mode = cbTxMode.getValue();
        String statut = cbTxStatut.getValue();
        int idInv = cbTxInvestissement.getValue().id;
        return new Transaction_Financiere(id, montant, dateStr, mode, statut, idInv);
    }

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

    private void showOnlyLabel(Label l, String msg) {
        l.setText(msg);
        l.setVisible(true);
        l.setManaged(true);
    }

    private void clearErrors() {
        Label[] errs = {errTxMontant, errTxDate, errTxMode, errTxStatut, errTxInvestissement};
        for (Label l : errs) {
            if (l == null) continue;
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
        }

        tfTxMontant.pseudoClassStateChanged(ERROR, false);
        dpTxDate.pseudoClassStateChanged(ERROR, false);
        cbTxMode.pseudoClassStateChanged(ERROR, false);
        cbTxStatut.pseudoClassStateChanged(ERROR, false);
        cbTxInvestissement.pseudoClassStateChanged(ERROR, false);
    }

    // ===================== TOAST
    private void showToast(String msg, String cssClass) {
        lblTxToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblTxToast.getStyleClass().add(cssClass);

        lblTxToast.setText(msg);
        lblTxToast.setVisible(true);
        lblTxToast.setManaged(true);

        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> {
            lblTxToast.setText("");
            lblTxToast.setVisible(false);
            lblTxToast.setManaged(false);
        });
        p.play();
    }

    // ===================== INVESTISSEMENTS COMBO (DB)
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
        } catch (Exception ignored) {}

        cbTxInvestissement.setItems(invs);
    }

    private String getInvLabel(int idInv) {
        InvItem i = cbTxInvestissement.getItems().stream().filter(x -> x.id == idInv).findFirst().orElse(null);
        return i == null ? ("Invest #" + idInv) : i.label;
    }

    // ===================== SMALL DTO
    public static class InvItem {
        public final int id;
        public final String label;
        public InvItem(int id, String label) { this.id = id; this.label = label; }
    }
}
