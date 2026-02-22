package controller;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.util.Duration;
import services.FinancementService.FinancementAlertService;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class StatsFinancementController {

    // ====== FXML
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> cbStatutProjet;

    @FXML private Button btnRefreshStats;
    @FXML private Label lblStatsToast;

    // KPI
    @FXML private Label lblKpiTotalLeve;
    @FXML private Label lblKpiNbProjets;
    @FXML private Label lblKpiNbInvest;
    @FXML private Label lblKpiTauxFinancement;

    // Charts
    @FXML private BarChart<String, Number> barTopProjets;
    @FXML private CategoryAxis axisTopProjetsX;
    @FXML private NumberAxis axisTopProjetsY;

    @FXML private PieChart pieModesPaiement;

    @FXML private LineChart<String, Number> lineEvolution;
    @FXML private CategoryAxis axisLineX;
    @FXML private NumberAxis axisLineY;

    // Alertes
    @FXML private ListView<String> lvAlertes;

    // ====== Services
    private final FinancementAlertService alertService = new FinancementAlertService();

    // ====== State
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final Map<String, Integer> labelToProjetId = new HashMap<>();
    private Integer selectedProjetId = null; // choisi via click sur bar chart

    @FXML
    public void initialize() {

        // Statuts projets
        cbStatutProjet.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatutProjet.getSelectionModel().selectFirst();

        // Dates par défaut: 6 derniers mois
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusMonths(6));

        // Axes
        axisTopProjetsX.setLabel("Projets");
        axisTopProjetsY.setLabel("Montant");

        axisLineX.setLabel("Mois");
        axisLineY.setLabel("Montant");

        // Styles ListView alertes + premier refresh
        setupAlertesListView();
        refresh();
    }

    // ===================== ALERTES LISTVIEW STYLE =====================

    private void setupAlertesListView() {
        if (lvAlertes == null) return;

        lvAlertes.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                String upper = item.toUpperCase(Locale.ROOT);
                setStyle("");

                if (upper.startsWith("[DANGER]")) {
                    setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-background-color: #fee2e2;");
                } else if (upper.startsWith("[WARN]") || upper.startsWith("[WARNING]")) {
                    setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-background-color: #ffedd5;");
                } else if (upper.startsWith("[INFO]")) {
                    setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-background-color: #dbeafe;");
                } else {
                    setStyle("-fx-text-fill: #111827;");
                }
            }
        });
    }

    // ===================== ACTIONS =====================

    @FXML
    private void refresh() {
        try {
            LocalDate from = (dpFrom.getValue() != null) ? dpFrom.getValue() : LocalDate.now().minusMonths(6);
            LocalDate to   = (dpTo.getValue() != null) ? dpTo.getValue() : LocalDate.now();

            if (from.isAfter(to)) {
                showToast("❌ Dates invalides (Du > Au).", "toastError");
                return;
            }

            String statut = cbStatutProjet.getValue();
            if (statut == null) statut = "TOUS";

            loadKpis(from, to, statut);
            loadBarTopProjets(from, to, statut);
            loadPieModesPaiement(from, to);
            loadLineEvolution(from, to);

            // Après refresh des charts, rafraîchir alertes
            refreshAlertes();

            showToast("✅ Stats mises à jour.", "toastSuccess");

        } catch (Exception e) {
            showToast("❌ Erreur stats : " + e.getMessage(), "toastError");
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshAlertes() {
        try {
            int idProjet = getSelectedProjetId();
            var alertes = alertService.getAlertesProjet(idProjet);

            if (alertes == null || alertes.isEmpty()) {
                lvAlertes.getItems().setAll("✅ Aucune alerte pour ce projet (ID=" + idProjet + ").");
                return;
            }

            lvAlertes.getItems().setAll(alertes.stream().map(Object::toString).toList());

        } catch (Exception e) {
            lvAlertes.getItems().setAll("❌ Erreur alertes : " + e.getMessage());
        }
    }

    // ===================== SELECTED PROJECT (Bar click / fallback) =====================

    private int getSelectedProjetId() {
        if (selectedProjetId != null) return selectedProjetId;

        if (!labelToProjetId.isEmpty()) {
            if (!barTopProjets.getData().isEmpty()
                    && !barTopProjets.getData().get(0).getData().isEmpty()) {

                String label = barTopProjets.getData().get(0).getData().get(0).getXValue();
                Integer id = labelToProjetId.get(label);
                if (id != null) return id;
            }
            return labelToProjetId.values().iterator().next();
        }

        throw new IllegalStateException("Aucun projet disponible (Top projets vide).");
    }

    // ===================== KPI =====================

    private void loadKpis(LocalDate from, LocalDate to, String statutProjet) throws Exception {

        String sqlTotal = """
                SELECT COALESCE(SUM(i.montantInvestissement),0) AS total
                FROM investissement i
                JOIN projet p ON p.id_projet = i.id_projet
                WHERE i.statut = 'FINANCE'
                  AND (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;

        String sqlNbInv = """
                SELECT COUNT(*) AS nb
                FROM investissement i
                JOIN projet p ON p.id_projet = i.id_projet
                WHERE (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;

        String sqlNbProjets = """
                SELECT COUNT(*) AS nb
                FROM projet p
                """;

        String sqlSumBudget = """
                SELECT COALESCE(SUM(p.budget),0) AS sb
                FROM projet p
                """;

        boolean filterStatut = statutProjet != null && !"TOUS".equalsIgnoreCase(statutProjet);

        if (filterStatut) {
            sqlTotal += " AND p.statut = ? ";
            sqlNbInv += " AND p.statut = ? ";
            sqlNbProjets += " WHERE p.statut = ? ";
            sqlSumBudget += " WHERE p.statut = ? ";
        }

        try (Connection c = MyDatabase.getInstance().getConnection()) {

            double totalLeve;
            try (PreparedStatement ps = c.prepareStatement(sqlTotal)) {
                ps.setDate(1, java.sql.Date.valueOf(from));
                ps.setDate(2, java.sql.Date.valueOf(to));
                if (filterStatut) ps.setString(3, statutProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalLeve = rs.getDouble("total");
                }
            }

            int nbInv;
            try (PreparedStatement ps = c.prepareStatement(sqlNbInv)) {
                ps.setDate(1, java.sql.Date.valueOf(from));
                ps.setDate(2, java.sql.Date.valueOf(to));
                if (filterStatut) ps.setString(3, statutProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    nbInv = rs.getInt("nb");
                }
            }

            int nbProjets;
            try (PreparedStatement ps = c.prepareStatement(sqlNbProjets)) {
                if (filterStatut) ps.setString(1, statutProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    nbProjets = rs.getInt("nb");
                }
            }

            double sumBudget;
            try (PreparedStatement ps = c.prepareStatement(sqlSumBudget)) {
                if (filterStatut) ps.setString(1, statutProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    sumBudget = rs.getDouble("sb");
                }
            }

            double taux = (sumBudget <= 0) ? 0 : (totalLeve * 100.0 / sumBudget);

            lblKpiTotalLeve.setText(df.format(totalLeve));
            lblKpiNbInvest.setText(String.valueOf(nbInv));
            lblKpiNbProjets.setText(String.valueOf(nbProjets));
            lblKpiTauxFinancement.setText(df.format(taux) + " %");
        }
    }

    // ===================== BAR TOP PROJETS =====================

    private void loadBarTopProjets(LocalDate from, LocalDate to, String statutProjet) throws Exception {
        barTopProjets.getData().clear();
        labelToProjetId.clear();
        selectedProjetId = null;

        boolean filterStatut = statutProjet != null && !"TOUS".equalsIgnoreCase(statutProjet);

        String sql = """
                SELECT p.id_projet, p.titre,
                       COALESCE(SUM(CASE WHEN i.statut='FINANCE' THEN i.montantInvestissement ELSE 0 END),0) AS total
                FROM projet p
                LEFT JOIN investissement i ON i.id_projet = p.id_projet
                WHERE (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;

        if (filterStatut) sql += " AND p.statut = ? ";

        sql += """
                GROUP BY p.id_projet, p.titre
                ORDER BY total DESC
                LIMIT 5
                """;

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Top Projets");

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            if (filterStatut) ps.setString(3, statutProjet);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_projet");
                    String titre = rs.getString("titre");
                    double total = rs.getDouble("total");

                    String label = (titre == null || titre.isBlank()) ? ("Projet #" + id) : titre;
                    labelToProjetId.put(label, id);

                    XYChart.Data<String, Number> d = new XYChart.Data<>(label, total);
                    s.getData().add(d);
                }
            }
        }

        barTopProjets.getData().add(s);

        // Click => sélectionner projet pour alertes
        for (XYChart.Data<String, Number> d : s.getData()) {
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setOnMouseClicked(ev -> {
                        Integer id = labelToProjetId.get(d.getXValue());
                        if (id != null) {
                            selectedProjetId = id;
                            showToast("📌 Projet sélectionné: " + d.getXValue() + " (ID=" + id + ")", "toastInfo");
                            refreshAlertes();
                        }
                    });
                    node.setStyle("-fx-cursor: hand;");
                }
            });
        }
    }

    // ===================== PIE MODES PAIEMENT =====================

    private void loadPieModesPaiement(LocalDate from, LocalDate to) throws Exception {
        pieModesPaiement.getData().clear();

        String sql = """
                SELECT mode_paiement, COUNT(*) AS nb
                FROM transaction_financiere
                WHERE statut_transaction = 'VALIDEE'
                  AND (date_transaction IS NULL OR date_transaction = '' OR (DATE(date_transaction) BETWEEN ? AND ?))
                GROUP BY mode_paiement
                ORDER BY nb DESC
                """;

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mode = rs.getString("mode_paiement");
                    int nb = rs.getInt("nb");
                    if (mode == null || mode.isBlank()) mode = "INCONNU";
                    pieModesPaiement.getData().add(new PieChart.Data(mode, nb));
                }
            }
        }
    }

    // ===================== LINE EVOLUTION (par mois) =====================

    private void loadLineEvolution(LocalDate from, LocalDate to) throws Exception {
        lineEvolution.getData().clear();

        Map<YearMonth, Double> map = new LinkedHashMap<>();
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);

        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            map.put(cur, 0.0);
            cur = cur.plusMonths(1);
        }

        String sql = """
                SELECT DATE_FORMAT(date_transaction, '%Y-%m') AS ym,
                       COALESCE(SUM(montantTransaction),0) AS total
                FROM transaction_financiere
                WHERE statut_transaction='VALIDEE'
                  AND (date_transaction IS NULL OR date_transaction = '' OR (DATE(date_transaction) BETWEEN ? AND ?))
                GROUP BY ym
                ORDER BY ym ASC
                """;

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ym = rs.getString("ym");
                    double total = rs.getDouble("total");
                    if (ym != null && ym.matches("\\d{4}-\\d{2}")) {
                        YearMonth y = YearMonth.parse(ym);
                        if (map.containsKey(y)) map.put(y, total);
                    }
                }
            }
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Transactions validées (montant)");

        for (var e : map.entrySet()) {
            s.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue()));
        }

        lineEvolution.getData().add(s);
    }

    // ===================== TOAST =====================

    private void showToast(String msg, String cssClass) {
        if (lblStatsToast == null) return;

        lblStatsToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblStatsToast.getStyleClass().add(cssClass);

        lblStatsToast.setText(msg);
        lblStatsToast.setVisible(true);
        lblStatsToast.setManaged(true);

        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> {
            lblStatsToast.setText("");
            lblStatsToast.setVisible(false);
            lblStatsToast.setManaged(false);
        });
        p.play();
    }
}