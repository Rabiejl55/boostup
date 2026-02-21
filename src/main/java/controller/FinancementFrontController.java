package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Locale;
import java.util.stream.Collectors;

public class FinancementFrontController {

    // ===== Top bar =====
    @FXML private TextField tfSearch;
    @FXML private Label lbUserName;
    @FXML private Label lbUserRole;

    // ===== Quick stats =====
    @FXML private Label lbActiveRequests;
    @FXML private Label lbAvailableOffers;
    @FXML private Label lbTotalAmount;

    // ===== Role switch =====
    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton rbStartup;
    @FXML private RadioButton rbInvestor;

    // ===== Panels to show/hide =====
    @FXML private VBox startupPanel;
    @FXML private VBox investorPanel;

    // ===== Actions =====
    @FXML private Button btnNewRequest;
    @FXML private Button btnNewOffer;

    // ===== Filters =====
    @FXML private ComboBox<String> cbSector;
    @FXML private ComboBox<String> cbStage;
    @FXML private ComboBox<String> cbRange;
    @FXML private ComboBox<String> cbType;

    // ===== Lists =====
    @FXML private ListView<FinItem> lvStartupItems;
    @FXML private ListView<FinItem> lvInvestorItems;

    // ===== Data =====
    private final ObservableList<FinItem> startupAll = FXCollections.observableArrayList();
    private final ObservableList<FinItem> investorAll = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ---- Fake user (remplace par session user) ----
        lbUserName.setText("Utilisateur");
        setRoleLabel("Startup");

        // ---- Stats (remplace par tes services) ----
        lbActiveRequests.setText("12");
        lbAvailableOffers.setText("7");
        lbTotalAmount.setText("240,000 DT");

        // ---- Filters ----
        cbSector.setItems(FXCollections.observableArrayList("FinTech", "HealthTech", "EdTech", "AgriTech", "SaaS", "E-commerce"));
        cbStage.setItems(FXCollections.observableArrayList("Idea", "Pre-seed", "Seed", "Series A"));
        cbRange.setItems(FXCollections.observableArrayList("0–10k", "10k–50k", "50k–100k", "100k+"));
        cbType.setItems(FXCollections.observableArrayList("Equity", "Loan", "Grant", "Convertible"));

        // ---- ListView look as cards ----
        lvStartupItems.setCellFactory(lv -> new FinItemCardCell());
        lvInvestorItems.setCellFactory(lv -> new FinItemCardCell());

        // ---- Seed data demo ----
        seedDemoData();

        // ---- Default mode ----
        rbStartup.setSelected(true);
        applyRoleUI(true);

        // ---- Listeners (role switch & search) ----
        roleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isStartup = newT == rbStartup;
            applyRoleUI(isStartup);
        });

        tfSearch.textProperty().addListener((obs, oldV, newV) -> applySearchAndFilters());
    }

    // ===============================
    // UI behavior
    // ===============================

    private void applyRoleUI(boolean isStartupMode) {
        // show/hide panels
        startupPanel.setManaged(isStartupMode);
        startupPanel.setVisible(isStartupMode);

        investorPanel.setManaged(!isStartupMode);
        investorPanel.setVisible(!isStartupMode);

        // enable the right action button
        btnNewRequest.setManaged(isStartupMode);
        btnNewRequest.setVisible(isStartupMode);

        btnNewOffer.setManaged(!isStartupMode);
        btnNewOffer.setVisible(!isStartupMode);

        setRoleLabel(isStartupMode ? "Startup" : "Investisseur");
        applySearchAndFilters();
    }

    private void setRoleLabel(String role) {
        lbUserRole.setText(role);
    }

    private void applySearchAndFilters() {
        String q = safeLower(tfSearch.getText());

        String sector = cbSector.getValue();
        String stage = cbStage.getValue();
        String range = cbRange.getValue();
        String type  = cbType.getValue();

        // Startup
        ObservableList<FinItem> startupFiltered = startupAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        lvStartupItems.setItems(startupFiltered);

        // Investor
        ObservableList<FinItem> investorFiltered = investorAll.stream()
                .filter(it -> matches(it, q, sector, stage, range, type))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        lvInvestorItems.setItems(investorFiltered);
    }

    private boolean matches(FinItem it, String q, String sector, String stage, String range, String type) {
        boolean ok = true;

        if (q != null && !q.isBlank()) {
            String hay = (it.title + " " + it.subtitle + " " + it.meta).toLowerCase(Locale.ROOT);
            ok = ok && hay.contains(q);
        }
        if (sector != null && !sector.isBlank()) ok = ok && it.meta.contains(sector);
        if (stage != null && !stage.isBlank()) ok = ok && it.meta.contains(stage);
        if (range != null && !range.isBlank()) ok = ok && it.meta.contains(range);
        if (type != null && !type.isBlank()) ok = ok && it.meta.contains(type);

        return ok;
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    // ===============================
    // Demo data (remplace par DB)
    // ===============================

    private void seedDemoData() {
        startupAll.clear();
        investorAll.clear();

        // Startup items (demandes)
        startupAll.addAll(
                new FinItem("FIN-203 • Demande", "50,000 DT", "HealthTech • Seed • Equity", "Plateforme de suivi patient pour cliniques privées."),
                new FinItem("FIN-217 • Demande", "120,000 DT", "SaaS • Pre-seed • Convertible", "Outil B2B d’automatisation facturation + relances."),
                new FinItem("FIN-221 • Demande", "20,000 DT", "EdTech • Idea • Grant", "MVP mobile pour micro-learning en langues.")
        );

        // Investor items (offres)
        investorAll.addAll(
                new FinItem("INV-88 • Offre", "Ticket 20k–100k", "FinTech/SaaS • Seed • Equity", "Mentoring inclus, suivi 6–12 mois."),
                new FinItem("INV-91 • Offre", "Ticket 10k–50k", "AgriTech • Pre-seed • Loan", "Priorité régions, traction minimale requise."),
                new FinItem("INV-104 • Offre", "Ticket 50k–200k", "E-commerce • Series A • Equity", "Recherche équipe solide + croissance.")
        );

        lvStartupItems.setItems(startupAll);
        lvInvestorItems.setItems(investorAll);
    }

    // ===============================
    // Sidebar navigation (à brancher)
    // ===============================

    @FXML private void goDashboard() { toast("Navigation : Dashboard"); }
    @FXML private void goFinancement() { toast("Vous êtes déjà sur Financement"); }
    @FXML private void goAccompagnement() { toast("Navigation: Accompagnement"); }
    @FXML private void goEvenements() { toast("Navigation: Événements"); }
    @FXML private void goCandidature() { toast("Navigation: Candidature"); }

    @FXML private void logout() { toast("Déconnexion..."); }

    // ===============================
    // Actions Financement
    // ===============================

    @FXML private void newRequest() { toast("Créer une nouvelle demande (Startup)"); }
    @FXML private void newOffer() { toast("Créer une nouvelle offre (Investisseur)"); }

    @FXML private void applyFilters() { applySearchAndFilters(); toast("Filtres appliqués"); }
    @FXML private void resetFilters() {
        cbSector.setValue(null);
        cbStage.setValue(null);
        cbRange.setValue(null);
        cbType.setValue(null);
        tfSearch.clear();
        applySearchAndFilters();
        toast("Filtres réinitialisés");
    }

    // Cards buttons from FXML (tu les branches à ta logique)
    @FXML private void openRequestDetails() { toast("Détails demande"); }
    @FXML private void editRequest() { toast("Modifier demande"); }
    @FXML private void cancelRequest() { toast("Annuler demande"); }

    @FXML private void contactInvestor() { toast("Contacter investisseur"); }
    @FXML private void openInvestorProfile() { toast("Profil investisseur"); }

    @FXML private void openOfferDetails() { toast("Détails offre"); }
    @FXML private void editOffer() { toast("Modifier offre"); }
    @FXML private void disableOffer() { toast("Désactiver offre"); }

    @FXML private void openStartupDeck() { toast("Voir dossier startup"); }
    @FXML private void scheduleCall() { toast("Planifier un call"); }

    // ===============================
    // Mini helper
    // ===============================
    private void toast(String msg) {
        // simple feedback (tu peux remplacer par Notifications/Toast)
        System.out.println("[FinancementFront] " + msg);
    }

    // ===============================
    // Inner model + cell
    // ===============================

    public static class FinItem {
        public final String title;
        public final String subtitle;
        public final String meta;
        public final String description;

        public FinItem(String title, String subtitle, String meta, String description) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.description = description;
        }
    }

    private static class FinItemCardCell extends ListCell<FinItem> {
        private final VBox root = new VBox(6);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Text meta = new Text();
        private final Text desc = new Text();
        private final HBox actions = new HBox(8);

        private final Button btnView = new Button("Voir");
        private final Button btnAction = new Button("Action");

        FinItemCardCell() {
            root.getStyleClass().add("card");
            title.getStyleClass().add("card-title");
            subtitle.getStyleClass().add("muted");
            meta.getStyleClass().add("card-meta");
            desc.getStyleClass().add("card-desc");

            desc.wrappingWidthProperty().bind(root.widthProperty().subtract(24));
            meta.wrappingWidthProperty().bind(root.widthProperty().subtract(24));

            btnView.getStyleClass().add("secondary-btn");
            btnAction.getStyleClass().add("primary-btn");
            actions.getChildren().addAll(btnView, btnAction);

            root.getChildren().addAll(title, subtitle, meta, desc, actions);

            btnView.setOnAction(e -> {
                FinItem item = getItem();
                if (item != null) System.out.println("Voir: " + item.title);
            });

            btnAction.setOnAction(e -> {
                FinItem item = getItem();
                if (item != null) System.out.println("Action sur: " + item.title);
            });
        }

        @Override
        protected void updateItem(FinItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            title.setText(item.title);
            subtitle.setText(item.subtitle);
            meta.setText(item.meta);
            desc.setText(item.description);

            setGraphic(root);
        }
    }
}
