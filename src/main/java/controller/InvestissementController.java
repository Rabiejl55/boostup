package controller;

import entities.GFinancement.Investissement;
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
import services.FinancementService.InvestissementService;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class InvestissementController {

    private static final int STATIC_USER_ID = 8;

    private static final PseudoClass PSEUDO_ERROR = PseudoClass.getPseudoClass("error");
    private static final String[] STATUTS = {"EN_ATTENTE", "FINANCE", "REFUSE", "REMBOURSE"};

    // ====== UI Form (sert pour AJOUT uniquement)
    @FXML private TextField tfInvMontant;
    @FXML private DatePicker dpInvDate;
    @FXML private ComboBox<String> cbInvStatut;
    @FXML private ComboBox<ProjetItem> cbInvProjet;
    @FXML private ComboBox<UserItem> cbInvUser; // caché

    @FXML private Label lblInvToast;
    @FXML private Label errInvMontant, errInvDate, errInvStatut, errInvProjet, errInvUser;

    @FXML private Button btnModifierInv, btnSupprimerInv;

    // ====== Search / filter
    @FXML private TextField tfSearchInv;
    @FXML private ComboBox<String> cbFilterInvStatut;
    @FXML private Label lblInvCount;

    // ====== Table
    @FXML private TableView<Investissement> tableInvestissements;
    @FXML private TableColumn<Investissement, Integer> colInvId;
    @FXML private TableColumn<Investissement, Double> colInvMontant;
    @FXML private TableColumn<Investissement, String> colInvStatut;
    @FXML private TableColumn<Investissement, String> colInvDate;
    @FXML private TableColumn<Investissement, String> colInvProjet;
    @FXML private TableColumn<Investissement, String> colInvUser;

    private final InvestissementService invService = new InvestissementService();

    private final ObservableList<Investissement> master = FXCollections.observableArrayList();
    private final ObservableList<Investissement> filtered = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCombos();
        setupTable();
        setupInlineEditing();     // ✅ NEW
        setupSelection();         // ✅ modifié
        setupSearchFilter();

        setupInputGuards();
        setupLiveValidation();

        disableUserSelection();

        refreshInvestissements();
        clearErrors();

        if (btnModifierInv != null) {
            btnModifierInv.setDisable(true);
            btnModifierInv.setVisible(false);
            btnModifierInv.setManaged(false);
        }
    }

    // ===================== SETUP =====================

    private void setupCombos() {
        cbInvStatut.setItems(FXCollections.observableArrayList(STATUTS));

        cbFilterInvStatut.setItems(FXCollections.observableArrayList());
        cbFilterInvStatut.getItems().add("TOUS");
        cbFilterInvStatut.getItems().addAll(STATUTS);
        cbFilterInvStatut.getSelectionModel().selectFirst();

        cbInvProjet.setConverter(new StringConverter<>() {
            @Override public String toString(ProjetItem p) { return p == null ? "" : p.titre; }
            @Override public ProjetItem fromString(String s) { return null; }
        });

        cbInvUser.setConverter(new StringConverter<>() {
            @Override public String toString(UserItem u) { return u == null ? "" : u.label; }
            @Override public UserItem fromString(String s) { return null; }
        });

        loadProjets();
        loadUsers();
    }

    private void disableUserSelection() {
        if (cbInvUser != null) {
            cbInvUser.setDisable(true);
            cbInvUser.setVisible(false);
            cbInvUser.setManaged(false);
        }
        if (errInvUser != null) {
            errInvUser.setVisible(false);
            errInvUser.setManaged(false);
        }
    }

    private void setupTable() {
        colInvId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId_investissement()));
        colInvMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMontantInvestissement()));
        colInvStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut()));
        colInvDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate_investissement()));
        colInvProjet.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getProjetLabel(c.getValue().getId_projet())));
        colInvUser.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getUserLabel(c.getValue().getId_user())));

        // Badge style statut (display)
        colInvStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                if (empty || statut == null) { setText(null); return; }

                setText(statut);
                switch (statut) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSE" -> getStyleClass().add("statut-refuse");
                }
            }
        });

        tableInvestissements.setItems(filtered);
        tableInvestissements.setEditable(true); // ✅ IMPORTANT
    }

    // ===================== INLINE EDITING (✅ NEW) =====================

    private void setupInlineEditing() {

        // 1) Montant (double-clic)
        colInvMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colInvMontant.setOnEditCommit(ev -> {
            Investissement inv = ev.getRowValue();
            Double nv = ev.getNewValue();

            if (nv == null || nv <= 0) {
                showToast("❌ Montant invalide (>0).", "toastError");
                tableInvestissements.refresh();
                return;
            }

            inv.setMontantInvestissement(nv);
            saveInline(inv, "✅ Montant modifié.");
        });

        // 2) Statut (combo)
        colInvStatut.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(STATUTS)
        ));
        colInvStatut.setOnEditCommit(ev -> {
            Investissement inv = ev.getRowValue();
            String nv = ev.getNewValue();

            if (nv == null || nv.isBlank()) {
                tableInvestissements.refresh();
                return;
            }

            inv.setStatut(nv);
            saveInline(inv, "✅ Statut modifié.");
        });

        // 3) Date (édition texte)
        colInvDate.setCellFactory(TextFieldTableCell.forTableColumn());
        colInvDate.setOnEditCommit(ev -> {
            Investissement inv = ev.getRowValue();
            String nv = ev.getNewValue() == null ? "" : ev.getNewValue().trim();

            // format attendu: yyyy-MM-dd
            try {
                LocalDate d = LocalDate.parse(nv);
                if (d.isAfter(LocalDate.now())) {
                    showToast("❌ Date future interdite.", "toastError");
                    tableInvestissements.refresh();
                    return;
                }
                inv.setDate_investissement(d.toString());
                saveInline(inv, "✅ Date modifiée.");
            } catch (Exception ex) {
                showToast("❌ Date invalide (yyyy-MM-dd).", "toastError");
                tableInvestissements.refresh();
            }
        });

        // 4) Projet (édition texte -> choisir parmi la liste)
        // Ici on laisse colInvProjet affichée en String, mais on autorise la saisie d'un titre existant.
        // (si tu préfères un vrai ComboBox dans la cellule, je te donne la version juste après)
        colInvProjet.setCellFactory(TextFieldTableCell.forTableColumn());
        colInvProjet.setOnEditCommit(ev -> {
            Investissement inv = ev.getRowValue();
            String nvTitre = ev.getNewValue() == null ? "" : ev.getNewValue().trim().toLowerCase(Locale.ROOT);

            ProjetItem match = cbInvProjet.getItems().stream()
                    .filter(p -> p.titre != null && p.titre.toLowerCase(Locale.ROOT).equals(nvTitre))
                    .findFirst().orElse(null);

            if (match == null) {
                showToast("❌ Projet introuvable (saisir le titre exact).", "toastError");
                tableInvestissements.refresh();
                return;
            }

            inv.setId_projet(match.id);
            saveInline(inv, "✅ Projet modifié.");
        });

        // User colonne: pas éditable (user auto)
        colInvUser.setEditable(false);
    }

    private void saveInline(Investissement inv, String okMsg) {
        try {
            invService.update(inv);
            showToast(okMsg, "toastSuccess");
            refreshInvestissements();
        } catch (SQLException e) {
            showToast("❌ Erreur update: " + e.getMessage(), "toastError");
            tableInvestissements.refresh();
        }
    }

    // ===================== SELECTION (✅ modifié: plus de remplissage form) =====================

    private void setupSelection() {
        btnSupprimerInv.setDisable(true);

        tableInvestissements.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            btnSupprimerInv.setDisable(sel == null);
        });
    }

    private void setupSearchFilter() {
        tfSearchInv.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterInvStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupInputGuards() {
        tfInvMontant.textProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            String cleaned = val.replaceAll("[^0-9,\\.]", "");
            if (!cleaned.equals(val)) tfInvMontant.setText(cleaned);
        });
    }

    // ===================== LIVE VALIDATION (form add) =====================

    private void setupLiveValidation() {
        tfInvMontant.textProperty().addListener((obs, o, n) -> validateMontantLive());
        dpInvDate.valueProperty().addListener((obs, o, n) -> validateDateLive());
        cbInvStatut.valueProperty().addListener((obs, o, n) -> validateStatutLive());
        cbInvProjet.valueProperty().addListener((obs, o, n) -> validateProjetLive());
    }

    private void validateAllLive() {
        validateMontantLive();
        validateDateLive();
        validateStatutLive();
        validateProjetLive();
    }

    private boolean validateMontantLive() {
        String m = tfInvMontant.getText() == null ? "" : tfInvMontant.getText().trim();
        boolean ok = false;

        try {
            double montant = Double.parseDouble(m.replace(",", "."));
            ok = montant > 0;
        } catch (Exception ignored) {}

        if (!ok) showError(errInvMontant, "Montant invalide (doit être > 0).");
        else hideError(errInvMontant);

        tfInvMontant.pseudoClassStateChanged(PSEUDO_ERROR, !ok);
        return ok;
    }

    private boolean validateDateLive() {
        LocalDate d = dpInvDate.getValue();
        boolean ok = (d != null);
        if (ok && d.isAfter(LocalDate.now())) ok = false;

        if (!ok) showError(errInvDate, "Date obligatoire (et non future).");
        else hideError(errInvDate);

        dpInvDate.pseudoClassStateChanged(PSEUDO_ERROR, !ok);
        return ok;
    }

    private boolean validateStatutLive() {
        boolean ok = cbInvStatut.getValue() != null && !cbInvStatut.getValue().isBlank();
        if (!ok) showError(errInvStatut, "Statut obligatoire.");
        else hideError(errInvStatut);

        cbInvStatut.pseudoClassStateChanged(PSEUDO_ERROR, !ok);
        return ok;
    }

    private boolean validateProjetLive() {
        boolean ok = cbInvProjet.getValue() != null;
        if (!ok) showError(errInvProjet, "Projet obligatoire.");
        else hideError(errInvProjet);

        cbInvProjet.pseudoClassStateChanged(PSEUDO_ERROR, !ok);
        return ok;
    }

    // ===================== ACTIONS =====================

    @FXML
    private void ajouterInvestissement() {
        clearErrors();
        validateAllLive();
        if (!validateForm()) return;

        try {
            Investissement inv = buildFromForm(0);

            // ✅ On force EN_ATTENTE pour que le workflow soit cohérent
            inv.setStatut("EN_ATTENTE");

            // ✅ Ajoute investissement + crée transaction EN_ATTENTE automatiquement
            // Mode par défaut : "CARTE"
            invService.ajouterAvecTransaction(inv, "CARTE");

            showToast("✅ Investissement ajouté + transaction créée.", "toastSuccess");
            refreshInvestissements();
            clearInvestissementForm();

        } catch (SQLException e) {
            showToast("❌ Erreur ajout: " + e.getMessage(), "toastError");
        }
    }

    // ✅ plus utilisé
    @FXML
    private void modifierInvestissement() {
        showToast("ℹ️ Modification via la table (double-clic).", "toastInfo");
    }

    @FXML
    private void supprimerInvestissement() {
        Investissement selected = tableInvestissements.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cet investissement ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    invService.supprimer(selected.getId_investissement());
                    showToast("🗑️ Investissement supprimé.", "toastInfo");
                    refreshInvestissements();
                    clearInvestissementForm();
                } catch (SQLException e) {
                    showToast("❌ Erreur suppression: " + e.getMessage(), "toastError");
                }
            }
        });
    }

    @FXML
    private void refreshInvestissements() {
        try {
            master.setAll(invService.read());
            applyFilters();
            updateCount();
        } catch (SQLException e) {
            showToast("❌ Erreur chargement: " + e.getMessage(), "toastError");
        }
    }

    @FXML
    private void clearInvestissementForm() {
        tfInvMontant.clear();
        dpInvDate.setValue(null);
        cbInvStatut.getSelectionModel().clearSelection();
        cbInvProjet.getSelectionModel().clearSelection();
        tableInvestissements.getSelectionModel().clearSelection();

        tfInvMontant.pseudoClassStateChanged(PSEUDO_ERROR, false);
        dpInvDate.pseudoClassStateChanged(PSEUDO_ERROR, false);
        cbInvStatut.pseudoClassStateChanged(PSEUDO_ERROR, false);
        cbInvProjet.pseudoClassStateChanged(PSEUDO_ERROR, false);

        clearErrors();
    }

    @FXML
    private void clearSearchInv() {
        tfSearchInv.clear();
        cbFilterInvStatut.getSelectionModel().selectFirst();
    }

    // ===================== VALIDATION submit (form add) =====================

    private boolean validateForm() {
        boolean ok = true;

        String m = tfInvMontant.getText() == null ? "" : tfInvMontant.getText().trim();
        try {
            double montant = Double.parseDouble(m.replace(",", "."));
            if (montant <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError(errInvMontant, "Montant invalide (doit être > 0).");
            tfInvMontant.pseudoClassStateChanged(PSEUDO_ERROR, true);
            ok = false;
        }

        if (dpInvDate.getValue() == null || dpInvDate.getValue().isAfter(LocalDate.now())) {
            showError(errInvDate, "Date obligatoire (et non future).");
            dpInvDate.pseudoClassStateChanged(PSEUDO_ERROR, true);
            ok = false;
        }

        if (cbInvStatut.getValue() == null || cbInvStatut.getValue().isBlank()) {
            showError(errInvStatut, "Statut obligatoire.");
            cbInvStatut.pseudoClassStateChanged(PSEUDO_ERROR, true);
            ok = false;
        }

        if (cbInvProjet.getValue() == null) {
            showError(errInvProjet, "Projet obligatoire.");
            cbInvProjet.pseudoClassStateChanged(PSEUDO_ERROR, true);
            ok = false;
        }

        return ok;
    }

    private Investissement buildFromForm(int id) {
        double montant = Double.parseDouble(tfInvMontant.getText().trim().replace(",", "."));
        String statut = cbInvStatut.getValue();
        String dateStr = dpInvDate.getValue().toString();
        int idProjet = cbInvProjet.getValue().id;
        int idUser = STATIC_USER_ID;

        return new Investissement(id, montant, statut, dateStr, idProjet, idUser);
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
        Label[] errs = {errInvMontant, errInvDate, errInvStatut, errInvProjet, errInvUser};
        for (Label l : errs) {
            if (l == null) continue;
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    // ===================== FILTERING =====================

    private void applyFilters() {
        String q = tfSearchInv.getText() == null ? "" : tfSearchInv.getText().trim().toLowerCase(Locale.ROOT);
        String statutFilter = cbFilterInvStatut.getValue();

        filtered.setAll(master.stream().filter(inv -> {
            boolean okStatut = (statutFilter == null || "TOUS".equals(statutFilter)) ||
                    Objects.equals(inv.getStatut(), statutFilter);

            String projetLabel = getProjetLabel(inv.getId_projet()).toLowerCase(Locale.ROOT);
            String userLabel = getUserLabel(inv.getId_user()).toLowerCase(Locale.ROOT);

            boolean okQuery = q.isBlank()
                    || String.valueOf(inv.getMontantInvestissement()).toLowerCase(Locale.ROOT).contains(q)
                    || (inv.getStatut() != null && inv.getStatut().toLowerCase(Locale.ROOT).contains(q))
                    || (inv.getDate_investissement() != null && inv.getDate_investissement().toLowerCase(Locale.ROOT).contains(q))
                    || projetLabel.contains(q)
                    || userLabel.contains(q);

            return okStatut && okQuery;
        }).collect(Collectors.toList()));

        updateCount();
    }

    private void updateCount() {
        lblInvCount.setText("Résultats: " + filtered.size() + " / Total: " + master.size());
    }

    // ===================== TOAST (avec auto hide) =====================

    private void showToast(String msg, String cssClass) {
        lblInvToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblInvToast.getStyleClass().add(cssClass);

        lblInvToast.setText(msg);
        lblInvToast.setVisible(true);
        lblInvToast.setManaged(true);

        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> {
            lblInvToast.setText("");
            lblInvToast.setVisible(false);
            lblInvToast.setManaged(false);
        });
        p.play();
    }

    // ===================== DATA LOADING =====================

    private void loadProjets() {
        String sql = "SELECT id_projet, titre FROM projet ORDER BY id_projet DESC";
        ObservableList<ProjetItem> projets = FXCollections.observableArrayList();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                projets.add(new ProjetItem(rs.getInt("id_projet"), rs.getString("titre")));
            }
        } catch (Exception ignored) {}

        cbInvProjet.setItems(projets);
    }

    private void loadUsers() {
        String sql = "SELECT id_user, email FROM user ORDER BY id_user DESC";
        ObservableList<UserItem> users = FXCollections.observableArrayList();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_user");
                String email = rs.getString("email");
                users.add(new UserItem(id, email));
            }
        } catch (Exception ignored) {}

        cbInvUser.setItems(users);
    }

    private String getProjetLabel(int idProjet) {
        ProjetItem p = cbInvProjet.getItems().stream().filter(x -> x.id == idProjet).findFirst().orElse(null);
        return p == null ? ("Projet #" + idProjet) : p.titre;
    }

    private String getUserLabel(int idUser) {
        UserItem u = cbInvUser.getItems().stream().filter(x -> x.id == idUser).findFirst().orElse(null);
        return u == null ? ("User #" + idUser) : u.label;
    }

    // ===================== DTOs =====================

    public static class ProjetItem {
        public final int id;
        public final String titre;
        public ProjetItem(int id, String titre) { this.id = id; this.titre = titre; }
    }
    public static class UserItem {
        public final int id;
        public final String label;
        public UserItem(int id, String label) { this.id = id; this.label = label; }
    }


}
