package controller;

import entities.GFinancement.Projet;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import services.FinancementService.ProjetService;

import java.io.IOException;

public class FinancementController {

    private final ProjetService projetService = new ProjetService();

    // ===== Tabs =====
    @FXML private TabPane tabPaneFinancement;
    @FXML private Tab tabProjets;
    @FXML private Tab tabInvestissements;
    @FXML private Tab tabTransactions;
    @FXML private BorderPane root;

    // ===== Lazy roots =====
    @FXML private BorderPane investissementsRoot;
    @FXML private BorderPane transactionsRoot;
    private boolean invLoaded = false;
    private boolean txLoaded = false;

    // ===== Form Projet (sert pour AJOUT uniquement) =====
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBudget;
    @FXML private ComboBox<String> cbStatutProjet;

    // Errors
    @FXML private Label errTitre;
    @FXML private Label errBudget;
    @FXML private Label errStatut;

    // Buttons
    @FXML private Button btnModifierProjet;   // sera caché/désactivé
    @FXML private Button btnSupprimerProjet;

    // Table
    @FXML private TableView<Projet> tableProjets;
    @FXML private TableColumn<Projet, Integer> colProjetId;
    @FXML private TableColumn<Projet, String> colProjetTitre;
    @FXML private TableColumn<Projet, Double> colProjetBudget;
    @FXML private TableColumn<Projet, String> colProjetStatut;

    // Search/filter/count
    @FXML private TextField tfSearchProjet;
    @FXML private ComboBox<String> cbFilterStatutProjet;
    @FXML private Label lblProjetCount;

    // Toast
    @FXML private Label lblProjetToast;

    private final ObservableList<Projet> projetsMaster = FXCollections.observableArrayList();
    private FilteredList<Projet> projetsFiltered;

    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 60;
    private static final int DESC_MAX = 300;

    private final PseudoClass errorClass = PseudoClass.getPseudoClass("error");

    @FXML
    public void initialize() {

        // Statuts
        cbStatutProjet.setItems(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatutProjet.setValue("EN_ATTENTE");

        // Colonnes (✅ ID correct)
        colProjetId.setCellValueFactory(new PropertyValueFactory<>("id_projet")); // IMPORTANT
        colProjetTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjetBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        colProjetStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Si tu veux cacher l'ID :
        colProjetId.setVisible(false);

        tableProjets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ✅ Activer édition inline
        tableProjets.setEditable(true);

        // ✅ Inline edit: TITRE
        colProjetTitre.setCellFactory(TextFieldTableCell.forTableColumn());
        colProjetTitre.setOnEditCommit(ev -> {
            Projet p = ev.getRowValue();
            String newVal = ev.getNewValue() != null ? ev.getNewValue().trim() : "";

            if (!isValidTitre(newVal)) {
                showToastError("Titre invalide (min " + TITRE_MIN + ", max " + TITRE_MAX + ")");
                tableProjets.refresh();
                return;
            }

            p.setTitre(newVal);
            saveProjetInline(p, "Titre modifié.");
        });

        // ✅ Inline edit: BUDGET
        colProjetBudget.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colProjetBudget.setOnEditCommit(ev -> {
            Projet p = ev.getRowValue();
            Double newVal = ev.getNewValue();

            if (newVal == null || newVal <= 0) {
                showToastError("Budget invalide (> 0).");
                tableProjets.refresh();
                return;
            }

            p.setBudget(newVal);
            saveProjetInline(p, "Budget modifié.");
        });

        // ✅ Inline edit: STATUT (ComboBox)
        colProjetStatut.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE")
        ));
        colProjetStatut.setOnEditCommit(ev -> {
            Projet p = ev.getRowValue();
            String newVal = ev.getNewValue();

            if (newVal == null || newVal.trim().isEmpty()) {
                tableProjets.refresh();
                return;
            }

            p.setStatut(newVal);
            saveProjetInline(p, "Statut modifié.");
        });

        // ✅ Style du badge statut (on garde ton design)
        colProjetStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                setText(null);
                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                if (empty || statut == null) return;

                setText(statut);
                switch (statut.toUpperCase()) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSE" -> getStyleClass().add("statut-refuse");
                }
            }

            @Override
            public void startEdit() {
                // Autorise l'édition par double-clic même si on a un badge cell
                super.startEdit();
            }
        });

        // Filtre statut
        cbFilterStatutProjet.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "FINANCE", "REFUSE"));
        cbFilterStatutProjet.setValue("TOUS");

        // Filter / Sort
        projetsFiltered = new FilteredList<>(projetsMaster, p -> true);
        SortedList<Projet> projetsSorted = new SortedList<>(projetsFiltered);
        projetsSorted.comparatorProperty().bind(tableProjets.comparatorProperty());
        tableProjets.setItems(projetsSorted);

        colProjetStatut.setComparator((s1, s2) -> Integer.compare(statutRank(s1), statutRank(s2)));
        colProjetStatut.setSortType(TableColumn.SortType.ASCENDING);
        colProjetBudget.setSortType(TableColumn.SortType.DESCENDING);
        tableProjets.getSortOrder().setAll(colProjetStatut, colProjetBudget);

        tfSearchProjet.textProperty().addListener((obs, o, n) -> applyProjetFilters());
        cbFilterStatutProjet.valueProperty().addListener((obs, o, n) -> applyProjetFilters());

        // Budget caractères dans form (ajout)
        tfBudget.textProperty().addListener((obs, old, nv) -> {
            if (nv == null) return;
            if (!nv.matches("[0-9]*([\\.,][0-9]*)?")) tfBudget.setText(old);
        });

        // ✅ Le formulaire ne sert plus à modifier => on désactive le bouton modifier
        if (btnModifierProjet != null) {
            btnModifierProjet.setDisable(true);
            btnModifierProjet.setVisible(false);
            btnModifierProjet.setManaged(false);
        }

        // ✅ Sélection : on ne remplit plus le formulaire (optionnel)
        tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            clearFieldErrors();
            // si tu veux garder “Supprimer” activé seulement si sélection:
            btnSupprimerProjet.setDisable(p == null);
        });
        btnSupprimerProjet.setDisable(true);

        initLiveValidation();
        refreshProjets();
        tableProjets.sort();

        // ✅ Lazy load onglets
        tabPaneFinancement.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabInvestissements && !invLoaded) {
                loadInto(investissementsRoot, "/fxml/investissement.fxml");
                invLoaded = true;
            } else if (newTab == tabTransactions && !txLoaded) {
                loadInto(transactionsRoot, "/fxml/transaction.fxml");
                txLoaded = true;
            }
        });
    }

    // ===== Inline save helper =====
    private void saveProjetInline(Projet p, String successMsg) {
        try {
            projetService.update(p);
            showToastSuccess(successMsg);
            refreshProjets(); // pour garder filtrage/tri cohérent
        } catch (Exception e) {
            showToastError("Erreur update : " + e.getMessage());
            tableProjets.refresh();
        }
    }

    private boolean isValidTitre(String t) {
        if (t == null) return false;
        t = t.trim();
        return !t.isEmpty() && t.length() >= TITRE_MIN && t.length() <= TITRE_MAX;
    }

    // Utilisé par DashboardController
    public void selectTab(int index) {
        if (tabPaneFinancement != null) tabPaneFinancement.getSelectionModel().select(index);
    }

    private void loadInto(BorderPane target, String fxmlPath) {
        if (target == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            target.setCenter(content);
        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Impossible de charger : " + fxmlPath + "\n" + e.getMessage());
            err.setStyle("-fx-text-fill: red; -fx-padding: 12;");
            target.setCenter(err);
        }
    }

    // ===== Sidebar actions =====
    @FXML private void goDashboard(ActionEvent e) { switchScene("/fxml/dashboard.fxml"); }
    @FXML private void goFinancement(ActionEvent e) { tabPaneFinancement.getSelectionModel().select(tabProjets); }
    @FXML private void goInvestissements(ActionEvent e) { tabPaneFinancement.getSelectionModel().select(tabInvestissements); }
    @FXML private void goTransactions(ActionEvent e) { tabPaneFinancement.getSelectionModel().select(tabTransactions); }
    @FXML private void logout(ActionEvent e) { System.out.println("Logout"); }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newRoot = loader.load();

            // ✅ On garde la même scene (donc même CSS)
            Scene scene = root.getScene();
            scene.setRoot(newRoot);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    // ===== CRUD =====
    @FXML private void refreshProjets() {
        try {
            projetsMaster.setAll(projetService.read());
            applyProjetFilters();
            tableProjets.sort();
        } catch (Exception e) {
            showErrorDialog("Erreur DB", e);
        }
    }

    @FXML private void ajouterProjet() {
        clearFieldErrors();
        if (!validateProjetForm()) return;

        try {
            Projet p = new Projet(
                    0,
                    tfTitre.getText().trim(),
                    taDescription.getText().trim(),
                    parseBudget(tfBudget.getText().trim()),
                    cbStatutProjet.getValue()
            );
            projetService.ajouter(p);
            refreshProjets();
            clearProjetForm();
            showToastSuccess("Projet ajouté avec succès.");
        } catch (Exception e) {
            showToastError("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    // ✅ plus utilisé (garde-le si tu veux éviter erreurs FXML)
    @FXML private void modifierProjet() {
        showToastError("La modification se fait directement dans le tableau (double-clic).");
    }

    @FXML private void supprimerProjet() {
        clearFieldErrors();
        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) { showInlineError(errTitre, "Sélectionne un projet."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le projet ?");
        confirm.setContentText("Projet: " + selected.getTitre() + " (ID=" + selected.getId_projet() + ")");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            projetService.supprimer(selected.getId_projet());
            refreshProjets();
            clearProjetForm();
            showToastSuccess("Projet supprimé.");
        } catch (Exception e) {
            showErrorDialog("Erreur suppression projet", e);
        }
    }

    @FXML private void clearProjetForm() {
        tfTitre.clear();
        taDescription.clear();
        tfBudget.clear();
        cbStatutProjet.setValue("EN_ATTENTE");
        clearFieldErrors();
    }

    // ===== Filter + count =====
    private void applyProjetFilters() {
        String q = tfSearchProjet.getText() == null ? "" : tfSearchProjet.getText().trim().toLowerCase();
        String statutChoisi = cbFilterStatutProjet.getValue();

        projetsFiltered.setPredicate(p -> {
            if (p == null) return false;

            boolean matchTexte = q.isEmpty()
                    || (p.getTitre() != null && p.getTitre().toLowerCase().contains(q))
                    || String.valueOf(p.getBudget()).contains(q);

            boolean matchStatut = statutChoisi == null
                    || "TOUS".equals(statutChoisi)
                    || (p.getStatut() != null && p.getStatut().equalsIgnoreCase(statutChoisi));

            return matchTexte && matchStatut;
        });

        int total = projetsMaster.size();
        int results = projetsFiltered.size();
        lblProjetCount.setText("Résultats: " + results + " / Total: " + total);
    }

    @FXML private void clearSearchProjet() {
        tfSearchProjet.clear();
        cbFilterStatutProjet.setValue("TOUS");
        applyProjetFilters();
    }

    // ===== Validation (pour AJOUT) =====
    private boolean validateProjetForm() {
        boolean ok = true;

        String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        String budget = tfBudget.getText() == null ? "" : tfBudget.getText().trim();
        String statut = cbStatutProjet.getValue();

        if (titre.isEmpty()) { setFieldError(tfTitre, errTitre, true, "Titre obligatoire."); ok = false; }
        if (budget.isEmpty()) { setFieldError(tfBudget, errBudget, true, "Budget obligatoire."); ok = false; }
        else { validateBudgetLive(); if (errBudget.isVisible()) ok = false; }

        if (statut == null || statut.trim().isEmpty()) { setComboError(cbStatutProjet, errStatut, true, "Statut obligatoire."); ok = false; }

        return ok;
    }

    private void initLiveValidation() {
        tfTitre.textProperty().addListener((obs, o, n) -> validateTitreLive());
        tfBudget.textProperty().addListener((obs, o, n) -> validateBudgetLive());
        cbStatutProjet.valueProperty().addListener((obs, o, n) -> validateStatutLive());

        taDescription.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > DESC_MAX) taDescription.setText(n.substring(0, DESC_MAX));
        });
    }

    private void validateBudgetLive() {
        String s = tfBudget.getText() == null ? "" : tfBudget.getText().trim();
        if (s.isEmpty()) { setFieldError(tfBudget, errBudget, false, ""); return; }

        if (!s.matches("\\d+(?:[\\.,]\\d{0,2})?")) {
            setFieldError(tfBudget, errBudget, true, "Format invalide. Ex: 1200.50");
            return;
        }

        try {
            double v = Double.parseDouble(s.replace(",", "."));
            if (v <= 0) { setFieldError(tfBudget, errBudget, true, "Le budget doit être > 0."); return; }
        } catch (Exception e) {
            setFieldError(tfBudget, errBudget, true, "Budget invalide.");
            return;
        }

        setFieldError(tfBudget, errBudget, false, "");
    }

    private void validateTitreLive() {
        String t = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        if (t.isEmpty()) { setFieldError(tfTitre, errTitre, false, ""); return; }
        if (t.length() < TITRE_MIN) { setFieldError(tfTitre, errTitre, true, "Titre trop court (min " + TITRE_MIN + ")."); return; }
        if (t.length() > TITRE_MAX) { setFieldError(tfTitre, errTitre, true, "Titre trop long (max " + TITRE_MAX + ")."); return; }
        setFieldError(tfTitre, errTitre, false, "");
    }

    private void validateStatutLive() {
        String st = cbStatutProjet.getValue();
        if (st == null || st.trim().isEmpty()) setComboError(cbStatutProjet, errStatut, true, "Statut obligatoire.");
        else setComboError(cbStatutProjet, errStatut, false, "");
    }

    private void setFieldError(TextInputControl field, Label errLabel, boolean isError, String msg) {
        field.pseudoClassStateChanged(errorClass, isError);
        if (isError) {
            errLabel.setText(msg);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        } else {
            errLabel.setText("");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
    }

    private void setComboError(ComboBox<?> combo, Label errLabel, boolean isError, String msg) {
        combo.pseudoClassStateChanged(errorClass, isError);
        if (isError) {
            errLabel.setText(msg);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
        } else {
            errLabel.setText("");
            errLabel.setVisible(false);
            errLabel.setManaged(false);
        }
    }

    private void clearFieldErrors() { hideErr(errTitre); hideErr(errBudget); hideErr(errStatut); }

    private void hideErr(Label l) {
        l.setText("");
        l.setVisible(false);
        l.setManaged(false);
    }

    private void showInlineError(Label errLabel, String msg) {
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private double parseBudget(String s) { return Double.parseDouble(s.replace(",", ".")); }

    private int statutRank(String statut) {
        if (statut == null) return 99;
        return switch (statut.toUpperCase()) {
            case "EN_ATTENTE" -> 0;
            case "FINANCE" -> 1;
            case "REFUSE" -> 2;
            default -> 50;
        };
    }

    private void showErrorDialog(String title, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    private void showToastSuccess(String msg) { showToast("✅ " + msg, "toastSuccess"); }
    private void showToastError(String msg) { showToast("❌ " + msg, "toastError"); }

    private void showToast(String msg, String cssClass) {
        lblProjetToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblProjetToast.getStyleClass().add(cssClass);

        lblProjetToast.setText(msg);
        lblProjetToast.setVisible(true);
        lblProjetToast.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            lblProjetToast.setText("");
            lblProjetToast.setVisible(false);
            lblProjetToast.setManaged(false);
        });
        pause.play();
    }

}
