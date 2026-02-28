package controller;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
    private Integer selectedProjetId = null;

    // debounce refresh
    private final PauseTransition debounce = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        cbStatutProjet.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "FINANCE", "REFUSE"));
        cbStatutProjet.getSelectionModel().selectFirst();

        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusMonths(6));

        axisTopProjetsX.setLabel("Projets");
        axisTopProjetsY.setLabel("Montant");
        axisLineX.setLabel("Mois");
        axisLineY.setLabel("Montant");

        setupAlertesListView();

        // auto refresh (sans boutons)
        dpFrom.valueProperty().addListener((o, a, b) -> scheduleRefresh());
        dpTo.valueProperty().addListener((o, a, b) -> scheduleRefresh());
        cbStatutProjet.valueProperty().addListener((o, a, b) -> scheduleRefresh());

        // 1er chargement
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        debounce.setOnFinished(e -> refreshAsync());
        debounce.playFromStart();
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

    // ===================== MAIN ASYNC REFRESH =====================

    private void refreshAsync() {
        final LocalDate from = (dpFrom.getValue() != null) ? dpFrom.getValue() : LocalDate.now().minusMonths(6);
        final LocalDate to   = (dpTo.getValue() != null) ? dpTo.getValue() : LocalDate.now();
        final String statut  = (cbStatutProjet.getValue() == null) ? "TOUS" : cbStatutProjet.getValue();

        if (from.isAfter(to)) {
            showToast("❌ Dates invalides (Du > Au).", "toastError", 2500);
            return;
        }

        showToast("⏳ Mise à jour...", "toastInfo", 0);

        Task<Bundle> task = new Task<>() {
            @Override
            protected Bundle call() throws Exception {
                Bundle b = new Bundle();

                // KPI
                b.kpi = loadKpis(from, to, statut);

                // Top projets (bar)
                b.top = loadTopProjets(from, to, statut);

                // pie
                b.pie = loadPie(from, to);

                // line
                b.line = loadLine(from, to);

                // alertes (selon projet sélectionné ou fallback)
                int pid = selectedProjetId != null ? selectedProjetId : b.top.firstProjetId();
                b.alertes = alertService.getAlertesProjet(pid);
                b.alertesProjetId = pid;

                return b;
            }
        };

        task.setOnSucceeded(ev -> {
            Bundle b = task.getValue();

            // KPI
            lblKpiTotalLeve.setText(df.format(b.kpi.totalLeve));
            lblKpiNbInvest.setText(String.valueOf(b.kpi.nbInvest));
            lblKpiNbProjets.setText(String.valueOf(b.kpi.nbProjets));
            lblKpiTauxFinancement.setText(df.format(b.kpi.taux) + " %");

            // Bar
            barTopProjets.getData().clear();
            labelToProjetId.clear();

            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName("Top Projets");

            for (TopRow r : b.top.rows) {
                labelToProjetId.put(r.label, r.idProjet);
                s.getData().add(new XYChart.Data<>(r.label, r.total));
            }
            barTopProjets.getData().add(s);

            // click bars (✅ pas d’erreur final)
            for (XYChart.Data<String, Number> d : s.getData()) {
                d.nodeProperty().addListener((obs, old, node) -> {
                    if (node == null) return;

                    final String xLabel = d.getXValue();              // ✅ effectively final
                    final Integer pid = labelToProjetId.get(xLabel);  // ✅ effectively final
                    node.setStyle("-fx-cursor: hand;");

                    node.setOnMouseClicked(e2 -> {
                        if (pid != null) {
                            selectedProjetId = pid;
                            showToast("📌 Projet sélectionné: " + xLabel + " (ID=" + pid + ")", "toastInfo", 2000);
                            refreshAlertesOnly(pid);
                        }
                    });
                });
            }

            // Pie
            pieModesPaiement.getData().setAll(b.pie);

            // Line
            lineEvolution.getData().clear();
            XYChart.Series<String, Number> ls = new XYChart.Series<>();
            ls.setName("Transactions validées (montant)");
            for (var e : b.line.entrySet()) {
                ls.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue()));
            }
            lineEvolution.getData().add(ls);

            // Alertes
            if (b.alertes == null || b.alertes.isEmpty()) {
                lvAlertes.getItems().setAll("✅ Aucune alerte pour ce projet (ID=" + b.alertesProjetId + ").");
            } else {
                lvAlertes.getItems().setAll(b.alertes.stream().map(Object::toString).toList());
            }

            showToast("✅ Mise à jour terminée.", "toastSuccess", 2000);
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            showToast("❌ Erreur : " + (ex == null ? "inconnue" : ex.getMessage()), "toastError", 3500);
            if (lvAlertes != null) lvAlertes.getItems().setAll("❌ Erreur stats.");
            if (ex != null) ex.printStackTrace();
        });

        Thread th = new Thread(task, "stats-refresh");
        th.setDaemon(true);
        th.start();
    }

    private void refreshAlertesOnly(int idProjet) {
        Task<List<Object>> t = new Task<>() {
            @Override
            protected List<Object> call() throws Exception {
                // on retourne List<Object> pour éviter contraintes de type si ton service renvoie autre chose
                return new ArrayList<>(alertService.getAlertesProjet(idProjet));
            }
        };

        t.setOnSucceeded(e -> {
            List<Object> list = t.getValue();
            if (list == null || list.isEmpty()) {
                lvAlertes.getItems().setAll("✅ Aucune alerte pour ce projet (ID=" + idProjet + ").");
            } else {
                lvAlertes.getItems().setAll(list.stream().map(Object::toString).toList());
            }
        });

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            lvAlertes.getItems().setAll("❌ Erreur alertes : " + (ex == null ? "inconnue" : ex.getMessage()));
        });

        Thread th = new Thread(t, "alertes-refresh");
        th.setDaemon(true);
        th.start();
    }

    // ===================== DB LOADERS =====================

    private Kpi loadKpis(LocalDate from, LocalDate to, String statutProjet) throws Exception {
        String sqlTotal = """
                SELECT COALESCE(SUM(i.montantInvestissement),0) AS total
                FROM investissement i
                JOIN projet p ON p.id_projet = i.id_projet
                WHERE UPPER(i.statut) = 'FINANCE'
                  AND (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;

        String sqlNbInv = """
                SELECT COUNT(*) AS nb
                FROM investissement i
                JOIN projet p ON p.id_projet = i.id_projet
                WHERE (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;

        String sqlNbProjets = "SELECT COUNT(*) AS nb FROM projet p";
        String sqlSumBudget = "SELECT COALESCE(SUM(p.budget),0) AS sb FROM projet p";

        boolean filter = statutProjet != null && !"TOUS".equalsIgnoreCase(statutProjet);
        if (filter) {
            sqlTotal += " AND p.statut = ? ";
            sqlNbInv += " AND p.statut = ? ";
            sqlNbProjets += " WHERE p.statut = ? ";
            sqlSumBudget += " WHERE p.statut = ? ";
        }

        Kpi k = new Kpi();

        try (Connection c = MyDatabase.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sqlTotal)) {
                ps.setDate(1, java.sql.Date.valueOf(from));
                ps.setDate(2, java.sql.Date.valueOf(to));
                if (filter) ps.setString(3, statutProjet);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); k.totalLeve = rs.getDouble("total"); }
            }

            try (PreparedStatement ps = c.prepareStatement(sqlNbInv)) {
                ps.setDate(1, java.sql.Date.valueOf(from));
                ps.setDate(2, java.sql.Date.valueOf(to));
                if (filter) ps.setString(3, statutProjet);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); k.nbInvest = rs.getInt("nb"); }
            }

            try (PreparedStatement ps = c.prepareStatement(sqlNbProjets)) {
                if (filter) ps.setString(1, statutProjet);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); k.nbProjets = rs.getInt("nb"); }
            }

            double sumBudget;
            try (PreparedStatement ps = c.prepareStatement(sqlSumBudget)) {
                if (filter) ps.setString(1, statutProjet);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); sumBudget = rs.getDouble("sb"); }
            }

            k.taux = (sumBudget <= 0) ? 0 : (k.totalLeve * 100.0 / sumBudget);
        }

        return k;
    }

    private Top loadTopProjets(LocalDate from, LocalDate to, String statutProjet) throws Exception {
        boolean filter = statutProjet != null && !"TOUS".equalsIgnoreCase(statutProjet);

        String sql = """
                SELECT p.id_projet, p.titre,
                       COALESCE(SUM(CASE WHEN UPPER(i.statut)='FINANCE' THEN i.montantInvestissement ELSE 0 END),0) AS total
                FROM projet p
                LEFT JOIN investissement i ON i.id_projet = p.id_projet
                WHERE (i.date_investissement IS NULL OR i.date_investissement = '' OR (DATE(i.date_investissement) BETWEEN ? AND ?))
                """;
        if (filter) sql += " AND p.statut = ? ";
        sql += """
                GROUP BY p.id_projet, p.titre
                ORDER BY total DESC
                LIMIT 5
                """;

        Top top = new Top();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            if (filter) ps.setString(3, statutProjet);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_projet");
                    String titre = rs.getString("titre");
                    double total = rs.getDouble("total");

                    String label = (titre == null || titre.isBlank()) ? ("Projet #" + id) : titre;
                    top.rows.add(new TopRow(id, label, total));
                }
            }
        }
        return top;
    }

    private List<PieChart.Data> loadPie(LocalDate from, LocalDate to) throws Exception {
        String sql = """
                SELECT mode_paiement, COUNT(*) AS nb
                FROM transaction_financiere
                WHERE statut_transaction = 'VALIDEE'
                  AND (date_transaction IS NULL OR date_transaction = '' OR (DATE(date_transaction) BETWEEN ? AND ?))
                GROUP BY mode_paiement
                ORDER BY nb DESC
                """;

        List<PieChart.Data> list = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mode = rs.getString("mode_paiement");
                    int nb = rs.getInt("nb");
                    if (mode == null || mode.isBlank()) mode = "INCONNU";
                    list.add(new PieChart.Data(mode, nb));
                }
            }
        }
        return list;
    }

    private Map<YearMonth, Double> loadLine(LocalDate from, LocalDate to) throws Exception {
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
        return map;
    }

    // ===================== TOAST =====================

    private void showToast(String msg, String cssClass, int autoHideMs) {
        if (lblStatsToast == null) return;

        lblStatsToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblStatsToast.getStyleClass().add(cssClass);

        lblStatsToast.setText(msg);
        lblStatsToast.setVisible(true);
        lblStatsToast.setManaged(true);

        if (autoHideMs > 0) {
            PauseTransition p = new PauseTransition(Duration.millis(autoHideMs));
            p.setOnFinished(e -> {
                lblStatsToast.setText("");
                lblStatsToast.setVisible(false);
                lblStatsToast.setManaged(false);
            });
            p.play();
        }
    }

    // ===================== DTOs =====================

    private static class Kpi {
        double totalLeve;
        int nbProjets;
        int nbInvest;
        double taux;
    }

    private static class TopRow {
        int idProjet;
        String label;
        double total;
        TopRow(int idProjet, String label, double total) {
            this.idProjet = idProjet;
            this.label = label;
            this.total = total;
        }
    }

    private static class Top {
        List<TopRow> rows = new ArrayList<>();
        int firstProjetId() {
            return rows.isEmpty() ? 0 : rows.get(0).idProjet;
        }
    }

    private static class Bundle {
        Kpi kpi;
        Top top;
        List<PieChart.Data> pie;
        Map<YearMonth, Double> line;
        List<?> alertes;
        int alertesProjetId;
    }
}