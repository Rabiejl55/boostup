package controller;

import entities.GFinancement.Projet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.FinancementService.ProjetService;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;

import javafx.fxml.Initializable;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;


public class FinancementController {

    private final ProjetService projetService = new ProjetService();

    // Form
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBudget;
    @FXML private ComboBox<String> cbStatutProjet;

    // Erreurs par champ
    @FXML private Label errTitre;
    @FXML private Label errBudget;
    @FXML private Label errStatut;

    // Boutons (disable si pas de sélection)
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

    private final ObservableList<Projet> projetsMaster = FXCollections.observableArrayList();
    private FilteredList<Projet> projetsFiltered;
    @FXML private void goFinancement() {}
    @FXML private void goCandidature() {}
    @FXML private void goAccompagnement() {}
    @FXML private void goEvenements() {}
    @FXML private void goDashboard() { /* switchScene("/fxml/dashboard.fxml"); */ }
    @FXML private void goInvestissements() { /* option: sélectionner tab investissements */ }
    @FXML private void goTransactions() { /* option: sélectionner tab transactions */ }
    @FXML private void logout() {}

    @FXML
    public void initialize() {

        // statuts
        cbStatutProjet.setItems(FXCollections.observableArrayList("EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatutProjet.setValue("EN_ATTENTE");

        // colonnes
        colProjetId.setVisible(false);
        colProjetTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjetBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        colProjetStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colProjetStatut.setCellFactory(col -> new TableCell<Projet, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");
                    return;
                }

                setText(statut);

                // Reset classes
                getStyleClass().removeAll("statut-attente", "statut-finance", "statut-refuse");

                // Add class based on statut
                switch (statut.toUpperCase()) {
                    case "EN_ATTENTE" -> getStyleClass().add("statut-attente");
                    case "FINANCE" -> getStyleClass().add("statut-finance");
                    case "REFUSE" -> getStyleClass().add("statut-refuse");
                    default -> { /* rien */ }
                }
            }
        });

        tableProjets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // filtre statut
        cbFilterStatutProjet.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "FINANCE", "REFUSE"));
        cbFilterStatutProjet.setValue("TOUS");

        // filter/sort
        projetsFiltered = new FilteredList<>(projetsMaster, p -> true);
        SortedList<Projet> projetsSorted = new SortedList<>(projetsFiltered);
        projetsSorted.comparatorProperty().bind(tableProjets.comparatorProperty());
        tableProjets.setItems(projetsSorted);

        // Tri pro par défaut : Statut (EN_ATTENTE -> FINANCE -> REFUSE) puis Budget (desc)
        colProjetStatut.setComparator((s1, s2) -> Integer.compare(statutRank(s1), statutRank(s2)));

        tableProjets.getSortOrder().clear();

        colProjetStatut.setSortType(TableColumn.SortType.ASCENDING);
        colProjetBudget.setSortType(TableColumn.SortType.DESCENDING);

        tableProjets.getSortOrder().add(colProjetStatut);
        tableProjets.getSortOrder().add(colProjetBudget);

        tableProjets.sort();



        tfSearchProjet.textProperty().addListener((obs, o, n) -> applyProjetFilters());
        cbFilterStatutProjet.valueProperty().addListener((obs, o, n) -> applyProjetFilters());

        // budget: chiffres + . ou ,
        tfBudget.textProperty().addListener((obs, old, nv) -> {
            if (nv == null) return;
            if (!nv.matches("[0-9]*([\\.,][0-9]*)?")) tfBudget.setText(old);
        });

        // disable boutons au départ
        setEditButtonsDisabled(true);

        // sélection -> remplir + activer boutons
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
    }

    // ---------------- CRUD ----------------
    @FXML
    private void refreshProjets() {
        try {
            projetsMaster.setAll(projetService.read());
            applyProjetFilters();
            tableProjets.sort();

        } catch (Exception e) {
            showErrorDialog("Erreur DB", e);
        }
    }

    @FXML
    private void ajouterProjet() {
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

    @FXML
    private void modifierProjet() {
        clearFieldErrors();

        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInlineError(errTitre, "Sélectionne un projet dans la table.");
            return;
        }

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

    @FXML
    private void supprimerProjet() {
        clearFieldErrors();

        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInlineError(errTitre, "Sélectionne un projet dans la table.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le projet ?");
        confirm.setContentText("Projet: " + selected.getTitre() + " (ID=" + selected.getId_projet() + ")");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            projetService.supprimer(selected.getId_projet());
            refreshProjets();
            clearProjetForm();
            showToastSuccess("Projet supprimé avec succès.");

        } catch (Exception e) {
            showErrorDialog("Erreur suppression projet", e);
        }
    }

    @FXML
    private void clearProjetForm() {
        tfTitre.clear();
        taDescription.clear();
        tfBudget.clear();
        cbStatutProjet.setValue("EN_ATTENTE");
        tableProjets.getSelectionModel().clearSelection();
        clearFieldErrors();
        setEditButtonsDisabled(true);
    }

    // ---------------- Filter + Count ----------------
    private void applyProjetFilters() {
        String q = (tfSearchProjet.getText() == null) ? "" : tfSearchProjet.getText().trim().toLowerCase();
        String statutChoisi = cbFilterStatutProjet.getValue();

        projetsFiltered.setPredicate(p -> {
            if (p == null) return false;

            boolean matchTexte = q.isEmpty()
                    || (p.getTitre() != null && p.getTitre().toLowerCase().contains(q))
                    || String.valueOf(p.getBudget()).contains(q);

            boolean matchStatut = (statutChoisi == null)
                    || statutChoisi.equals("TOUS")
                    || (p.getStatut() != null && p.getStatut().equalsIgnoreCase(statutChoisi));

            return matchTexte && matchStatut;
        });

        updateProjetCount();
    }

    private void updateProjetCount() {
        int total = projetsMaster.size();
        int results = projetsFiltered.size();
        lblProjetCount.setText("Résultats: " + results + " / Total: " + total);
    }

    @FXML
    private void clearSearchProjet() {
        tfSearchProjet.clear();
        cbFilterStatutProjet.setValue("TOUS");
        applyProjetFilters();
    }

    // ---------------- Validation (par champ) ----------------
    private boolean validateProjetForm() {
        boolean ok = true;

        String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        String budget = tfBudget.getText() == null ? "" : tfBudget.getText().trim();
        String statut = cbStatutProjet.getValue();

        if (titre.isEmpty()) {
            setFieldError(tfTitre, errTitre, true, "Titre obligatoire.");
            ok = false;
        }

        if (budget.isEmpty()) {
            setFieldError(tfBudget, errBudget, true, "Budget obligatoire.");
            ok = false;
        } else {
            validateBudgetLive();
            // si budget invalide, errBudget sera visible
            if (errBudget.isVisible()) ok = false;
        }

        if (statut == null || statut.trim().isEmpty()) {
            setComboError(cbStatutProjet, errStatut, true, "Statut obligatoire.");
            ok = false;
        }

        return ok;
    }

    private void initLiveValidation() {

        // Validation live : Titre
        tfTitre.textProperty().addListener((obs, o, n) -> validateTitreLive());

        // Validation live : Budget
        tfBudget.textProperty().addListener((obs, o, n) -> validateBudgetLive());

        // Validation live : Statut
        cbStatutProjet.valueProperty().addListener((obs, o, n) -> validateStatutLive());

        // Validation live : Description (longueur max)
        taDescription.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > DESC_MAX) {
                taDescription.setText(n.substring(0, DESC_MAX));
            }
        });
    }

    private double parseBudget(String s) {
        return Double.parseDouble(s.replace(",", "."));
    }

    private void showInlineError(Label errLabel, String msg) {
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearFieldErrors() {
        hideErr(errTitre);
        hideErr(errBudget);
        hideErr(errStatut);
    }

    private void hideErr(Label l) {
        l.setText("");
        l.setVisible(false);
        l.setManaged(false);
    }

    private void setEditButtonsDisabled(boolean disabled) {
        if (btnModifierProjet != null) btnModifierProjet.setDisable(disabled);
        if (btnSupprimerProjet != null) btnSupprimerProjet.setDisable(disabled);
    }

    // ---------------- Dialog erreurs ----------------
    private void showErrorDialog(String title, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    @FXML private Label lblProjetToast;
    private void showToastSuccess(String msg) {
        showToast("✅ " + msg, "toastSuccess");
    }

    private void showToastError(String msg) {
        showToast("❌ " + msg, "toastError");
    }

    private void showToastInfo(String msg) {
        showToast("ℹ " + msg, "toastInfo");
    }

    private void showToast(String msg, String cssClass) {
        if (lblProjetToast == null) return;

        // Reset classes
        lblProjetToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblProjetToast.getStyleClass().add(cssClass);

        lblProjetToast.setText(msg);
        lblProjetToast.setVisible(true);
        lblProjetToast.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> hideToast());
        pause.play();
    }

    private void hideToast() {
        if (lblProjetToast == null) return;
        lblProjetToast.setText("");
        lblProjetToast.setVisible(false);
        lblProjetToast.setManaged(false);
    }

    private int statutRank(String statut) {
        if (statut == null) return 99;
        return switch (statut.toUpperCase()) {
            case "EN_ATTENTE" -> 0;
            case "FINANCE" -> 1;
            case "REFUSE" -> 2;
            default -> 50;
        };
    }

    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 60;
    private static final int DESC_MAX = 300;

    private final PseudoClass errorClass = PseudoClass.getPseudoClass("error");

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

    private void validateBudgetLive() {
        String s = tfBudget.getText() == null ? "" : tfBudget.getText().trim();

        // Vide: on ne force pas tout de suite, mais on peut afficher si tu veux
        if (s.isEmpty()) {
            setFieldError(tfBudget, errBudget, false, "");
            return;
        }

        // Autoriser chiffres + séparateur . ou , + max 2 décimales
        boolean okFormat = s.matches("\\d+(?:[\\.,]\\d{0,2})?");
        if (!okFormat) {
            setFieldError(tfBudget, errBudget, true, "Format budget invalide. Exemple: 1200.50");
            return;
        }

        // Vérif > 0 si possible
        try {
            double v = Double.parseDouble(s.replace(",", "."));
            if (v <= 0) {
                setFieldError(tfBudget, errBudget, true, "Le budget doit être > 0.");
                return;
            }
        } catch (Exception e) {
            setFieldError(tfBudget, errBudget, true, "Budget invalide.");
            return;
        }

        setFieldError(tfBudget, errBudget, false, "");
    }

    private void validateTitreLive() {
        String t = tfTitre.getText() == null ? "" : tfTitre.getText().trim();

        if (t.isEmpty()) {
            setFieldError(tfTitre, errTitre, false, ""); // pas agressif si vide
            return;
        }
        if (t.length() < TITRE_MIN) {
            setFieldError(tfTitre, errTitre, true, "Titre trop court (min " + TITRE_MIN + ").");
            return;
        }
        if (t.length() > TITRE_MAX) {
            setFieldError(tfTitre, errTitre, true, "Titre trop long (max " + TITRE_MAX + ").");
            return;
        }
        setFieldError(tfTitre, errTitre, false, "");
    }

    private void validateStatutLive() {
        String st = cbStatutProjet.getValue();
        if (st == null || st.trim().isEmpty()) {
            setComboError(cbStatutProjet, errStatut, true, "Statut obligatoire.");
        } else {
            setComboError(cbStatutProjet, errStatut, false, "");
        }
    }

    @FXML
    private void goFinancement(ActionEvent event) {
        // TODO: navigation vers page financement (plus tard)
        System.out.println("Financement");
    }

    @FXML
    private void goCandidature(ActionEvent event) {
        System.out.println("Candidature");
    }
    @FXML
    private void goAccompagnement(ActionEvent event) {
        // TODO
        System.out.println("Accompagnement");
    }

    @FXML
    private void goEvenements(ActionEvent event) {
        // TODO
        System.out.println("Evenements");
    }

    @FXML
    private void logout(ActionEvent event) {
        // TODO
        System.out.println("Logout");
    }


}
