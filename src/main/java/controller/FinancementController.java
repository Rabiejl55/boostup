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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.FinancementService.ProjetService;

import java.io.IOException;

public class FinancementController {

    private final ProjetService projetService = new ProjetService();

    // ===== Tabs =====
    @FXML private TabPane tabPaneFinancement;
    @FXML private Tab tabProjets;
    @FXML private Tab tabInvestissements;
    @FXML private Tab tabTransactions;

    // ===== Lazy roots =====
    @FXML private BorderPane investissementsRoot;
    @FXML private BorderPane transactionsRoot;
    private boolean invLoaded = false;
    private boolean txLoaded = false;

    // ===== Form Projet =====
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBudget;
    @FXML private ComboBox<String> cbStatutProjet;

    // Errors
    @FXML private Label errTitre;
    @FXML private Label errBudget;
    @FXML private Label errStatut;

    // Buttons
    @FXML private Button btnModifierProjet;
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

        // Colonnes
        colProjetId.setVisible(false);
        colProjetTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjetBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        colProjetStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Badge statut
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
        });

        tableProjets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        // Budget caractères
        tfBudget.textProperty().addListener((obs, old, nv) -> {
            if (nv == null) return;
            if (!nv.matches("[0-9]*([\\.,][0-9]*)?")) tfBudget.setText(old);
        });

        setEditButtonsDisabled(true);

        tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            clearFieldErrors();
            if (p == null) {
                setEditButtonsDisabled(true);
                return;
            }
            setEditButtonsDisabled(false);
            tfTitre.setText(p.getTitre());
            taDescription.setText(p.getDescription());
            tfBudget.setText(String.valueOf(p.getBudget()));
            cbStatutProjet.setValue(p.getStatut());
        });

        initLiveValidation();
        refreshProjets();
        tableProjets.sort();

        // ✅ Lazy load onglets (évite crash DB au chargement de financement.fxml)
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
            Parent root = loader.load();
            Stage stage = (Stage) tabPaneFinancement.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
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

    @FXML private void modifierProjet() {
        clearFieldErrors();
        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) { showInlineError(errTitre, "Sélectionne un projet."); return; }
        if (!validateProjetForm()) return;

        try {
            selected.setTitre(tfTitre.getText().trim());
            selected.setDescription(taDescription.getText().trim());
            selected.setBudget(parseBudget(tfBudget.getText().trim()));
            selected.setStatut(cbStatutProjet.getValue());

            projetService.update(selected);
            refreshProjets();
            clearProjetForm();
            showToastSuccess("Projet modifié avec succès.");
        } catch (Exception e) {
            showErrorDialog("Erreur modification projet", e);
        }
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
        tableProjets.getSelectionModel().clearSelection();
        clearFieldErrors();
        setEditButtonsDisabled(true);
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

    // ===== Validation =====
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

    private void setEditButtonsDisabled(boolean disabled) {
        btnModifierProjet.setDisable(disabled);
        btnSupprimerProjet.setDisable(disabled);
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
