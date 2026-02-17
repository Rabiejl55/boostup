package controller;

import entities.GFinancement.Investissement;
import entities.GFinancement.Projet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.FinancementService.InvestissementService;
import services.FinancementService.ProjetService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class FinancementFrontController {

    // ===== SIDEBAR =====
    @FXML private HBox financementItem;
    @FXML private HBox accompagnementsItem;

    // ===== SECTIONS =====
    @FXML private VBox sectionFinancement;
    @FXML private VBox sectionAccompagnements;
    @FXML private VBox sectionObjectifs;
    @FXML private VBox sectionMesInvestissements;

    // ===== FINANCEMENT UI =====
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatut;

    @FXML private Label lblNbProjets;
    @FXML private Label lblNbInvestissements;
    @FXML private Label lblNbEnAttente;
    @FXML private Label lblNbAffiches;

    @FXML private FlowPane fpProjets;
    @FXML private Label lblProjetSelected;

    @FXML private TextField tfMontant;
    @FXML private ComboBox<String> cbModePaiement;
    @FXML private Label lblFeedback;

    @FXML private FlowPane fpInvestissements;

    private final ProjetService projetService = new ProjetService();
    private final InvestissementService investissementService = new InvestissementService();

    private final ObservableList<Projet> projetsAll = FXCollections.observableArrayList();

    // TODO: remplacer par l'id réel du user connecté
    private final int CURRENT_USER_ID = 1;

    private Projet selectedProjet = null;

    @FXML
    public void initialize() {
        // ---- Combo statut
        cbStatut.setItems(FXCollections.observableArrayList("Tous", "Ouvert", "Fermé", "En cours"));
        cbStatut.setValue("Tous");

        // ---- Paiement
        cbModePaiement.setItems(FXCollections.observableArrayList("Carte", "Virement", "Cash"));
        cbModePaiement.getSelectionModel().selectFirst();

        // ---- listeners filtres
        tfSearch.textProperty().addListener((o, a, b) -> renderProjetsCards());
        cbStatut.valueProperty().addListener((o, a, b) -> renderProjetsCards());

        // ---- afficher par défaut : financement
        showFinancement();

        // ---- load data
        refreshProjets(null);
        refreshMesInvestissements(null);
    }

    // ===================== NAVIGATION (SIDEBAR) =====================

    @FXML
    private void handleTableauDeBordClick(MouseEvent e) {
        System.out.println("Tableau de bord clicked");
        // TODO: navigation vers dashboard
    }

    @FXML
    private void handleFinancementClick(MouseEvent e) {
        showFinancement();
    }

    @FXML
    private void handleAccompagnementsClick(MouseEvent e) {
        showAccompagnements();
    }

    private void showFinancement() {
        if (sectionFinancement != null) sectionFinancement.setManaged(true);
        if (sectionFinancement != null) sectionFinancement.setVisible(true);

        if (sectionMesInvestissements != null) sectionMesInvestissements.setManaged(true);
        if (sectionMesInvestissements != null) sectionMesInvestissements.setVisible(true);

        if (sectionAccompagnements != null) sectionAccompagnements.setManaged(false);
        if (sectionAccompagnements != null) sectionAccompagnements.setVisible(false);

        if (sectionObjectifs != null) sectionObjectifs.setManaged(false);
        if (sectionObjectifs != null) sectionObjectifs.setVisible(false);
    }

    private void showAccompagnements() {
        if (sectionAccompagnements != null) sectionAccompagnements.setManaged(true);
        if (sectionAccompagnements != null) sectionAccompagnements.setVisible(true);

        if (sectionObjectifs != null) sectionObjectifs.setManaged(true);
        if (sectionObjectifs != null) sectionObjectifs.setVisible(true);

        if (sectionFinancement != null) sectionFinancement.setManaged(false);
        if (sectionFinancement != null) sectionFinancement.setVisible(false);

        if (sectionMesInvestissements != null) sectionMesInvestissements.setManaged(false);
        if (sectionMesInvestissements != null) sectionMesInvestissements.setVisible(false);
    }

    // ===================== DATA LOAD =====================

    @FXML
    private void refreshProjets(ActionEvent e) {
        try {
            List<Projet> list = projetService.recuperer(); // <== tu as demandé "recuperer seulement"
            projetsAll.setAll(list);
            lblNbProjets.setText(String.valueOf(projetsAll.size()));
            renderProjetsCards();
        } catch (Exception ex) {
            showError("Erreur chargement projets : " + ex.getMessage());
        }
    }

    @FXML
    private void refreshMesInvestissements(ActionEvent e) {
        try {
            fpInvestissements.getChildren().clear();

            // ⚠️ adapte si ton service a une méthode différente
            // Idéal: investissementService.recupererParUser(CURRENT_USER_ID)
            List<Investissement> list;
            try {
                list = investissementService.recuperer(); // si tu n'as que recuperer()
                // filtre côté front si nécessaire
                list = list.stream()
                        .filter(inv -> inv.getId_user() == CURRENT_USER_ID)
                        .collect(Collectors.toList());
            } catch (Exception ignore) {
                // si recuperer() n'existe pas chez toi, garde juste une liste vide
                list = List.of();
            }

            lblNbInvestissements.setText(String.valueOf(list.size()));
            long enAttente = list.stream().filter(inv -> "EN_ATTENTE".equalsIgnoreCase(inv.getStatut())).count();
            lblNbEnAttente.setText(String.valueOf(enAttente));

            for (Investissement inv : list) {
                fpInvestissements.getChildren().add(buildInvestissementCard(inv));
            }

        } catch (Exception ex) {
            showError("Erreur chargement investissements : " + ex.getMessage());
        }
    }

    // ===================== FILTER + RENDER =====================

    private void renderProjetsCards() {
        fpProjets.getChildren().clear();

        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();
        String statut = cbStatut.getValue();

        List<Projet> filtered = projetsAll.stream().filter(p -> {
            boolean okSearch = q.isEmpty() || (p.getTitre() != null && p.getTitre().toLowerCase().contains(q));
            boolean okStatut = (statut == null || "Tous".equals(statut)) ||
                    (p.getStatut() != null && p.getStatut().equalsIgnoreCase(statut));
            return okSearch && okStatut;
        }).toList();

        if (lblNbAffiches != null) lblNbAffiches.setText(filtered.size() + " affichés");

        for (Projet p : filtered) {
            fpProjets.getChildren().add(buildProjetCard(p));
        }

        // reset selection si elle n'est plus visible
        if (selectedProjet != null && !filtered.contains(selectedProjet)) {
            selectedProjet = null;
            lblProjetSelected.setText("Sélectionne un projet pour investir.");
        }
    }

    @FXML
    private void clearFilters(ActionEvent e) {
        tfSearch.clear();
        cbStatut.setValue("Tous");
        renderProjetsCards();
    }

    // ===================== INVEST ACTION =====================

    @FXML
    private void investir(ActionEvent e) {
        if (selectedProjet == null) {
            showError("Veuillez sélectionner un projet.");
            return;
        }

        String montantStr = tfMontant.getText() == null ? "" : tfMontant.getText().trim();
        if (montantStr.isEmpty()) {
            showError("Veuillez saisir un montant.");
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr);
        } catch (NumberFormatException ex) {
            showError("Montant invalide (ex: 500).");
            return;
        }

        if (montant <= 0) {
            showError("Le montant doit être > 0.");
            return;
        }

        if (montant > selectedProjet.getBudget()) {
            showError("Le montant dépasse le budget du projet.");
            return;
        }

        String mode = cbModePaiement.getValue();
        if (mode == null || mode.isBlank()) {
            showError("Veuillez choisir un mode de paiement.");
            return;
        }

        Investissement inv = new Investissement();
        inv.setMontantInvestissement(montant);
        inv.setStatut("EN_ATTENTE");
        inv.setDate_investissement(LocalDate.now().toString());
        inv.setId_projet(selectedProjet.getId_projet());
        inv.setId_user(CURRENT_USER_ID);

        try {
            investissementService.ajouter(inv);
            showSuccess("✅ Investissement enregistré ! Statut: EN_ATTENTE");
            tfMontant.clear();
            refreshMesInvestissements(null);
        } catch (Exception ex) {
            showError("Erreur lors de l'investissement : " + ex.getMessage());
        }
    }

    // ===================== UI HELPERS (CARDS) =====================

    private VBox buildProjetCard(Projet p) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("stats-mini-card"); // réutilise ton style existant
        card.setPadding(new Insets(12));
        card.setPrefWidth(280);

        Label titre = new Label(p.getTitre() == null ? "(Sans titre)" : p.getTitre());
        titre.getStyleClass().add("section-subtitle");

        Label id = new Label("ID: " + p.getId_projet());
        Label budget = new Label("Budget: " + p.getBudget());
        Label statut = new Label("Statut: " + (p.getStatut() == null ? "-" : p.getStatut()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button select = new Button("Sélectionner");
        select.getStyleClass().add("search-button");
        select.setOnAction(e -> {
            selectedProjet = p;
            lblProjetSelected.setText("Projet sélectionné : " + p.getTitre()
                    + " | Budget: " + p.getBudget()
                    + " | Statut: " + p.getStatut());
            lblFeedback.setText("");
            lblFeedback.getStyleClass().setAll();
        });

        card.getChildren().addAll(titre, id, budget, statut, spacer, select);

        // click sur la carte = sélectionner aussi
        card.setOnMouseClicked(e -> select.fire());

        return card;
    }

    private VBox buildInvestissementCard(Investissement inv) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stats-mini-card");
        card.setPadding(new Insets(12));
        card.setPrefWidth(320);

        Label head = new Label("Investissement");
        head.getStyleClass().add("section-subtitle");

        Label montant = new Label("Montant: " + inv.getMontantInvestissement());
        Label date = new Label("Date: " + inv.getDate_investissement());
        Label statut = new Label("Statut: " + inv.getStatut());
        Label projet = new Label("Projet ID: " + inv.getId_projet());

        card.getChildren().addAll(head, projet, montant, date, statut);
        return card;
    }

    private void showError(String msg) {
        lblFeedback.setText(msg);
        lblFeedback.getStyleClass().setAll("error");
    }

    private void showSuccess(String msg) {
        lblFeedback.setText(msg);
        lblFeedback.getStyleClass().setAll("success");
    }
}
