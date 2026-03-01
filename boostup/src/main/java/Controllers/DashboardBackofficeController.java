package Controllers;

import entities.GCandidature.Candidature;
import entities.GCandidature.Evaluation;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.EvaluationService;
import utils.AlertUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardBackofficeController implements Initializable {

    // ── KPI Labels ────────────────────────────────────────────────
    @FXML private Label kpiTotalCandidatures;
    @FXML private Label kpiTotalSub;
    @FXML private Label kpiAcceptees;
    @FXML private Label kpiAccepteesPct;
    @FXML private Label kpiRefusees;
    @FXML private Label kpiRefuseesPct;
    @FXML private Label kpiEnAttente;
    @FXML private Label kpiAttentePct;
    @FXML private Label kpiScoreMoyen;

    // ── Chart containers ──────────────────────────────────────────
    @FXML private StackPane chartBarContainer;
    @FXML private StackPane chartPieContainer;
    @FXML private StackPane chartHistoContainer;

    // ── Services ──────────────────────────────────────────────────
    private final CandidatureService candidatureService = new CandidatureService();
    private final EvaluationService  evaluationService  = new EvaluationService();

    // ── Données courantes (pour export) ──────────────────────────
    private JFreeChart chartBar;
    private JFreeChart chartPie;
    private JFreeChart chartHisto;

    // ── Palette BOOSTUP ──────────────────────────────────────────
    private static final Color C_INDIGO   = new Color(0x4f, 0x62, 0xe8);
    private static final Color C_GREEN    = new Color(0x10, 0xb9, 0x81);
    private static final Color C_RED      = new Color(0xef, 0x44, 0x44);
    private static final Color C_ORANGE   = new Color(0xf5, 0x9e, 0x0b);
    private static final Color C_VIOLET   = new Color(0x7c, 0x3a, 0xed);
    private static final Color C_BG       = new Color(0xf7, 0xf8, 0xfc);
    private static final Color C_BORDER   = new Color(0xdc, 0xe0, 0xf0);
    private static final Color C_TEXT     = new Color(0x1e, 0x27, 0x46);
    private static final Color C_MUTED    = new Color(0x6b, 0x74, 0x94);

    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Chargement async pour ne pas bloquer l'UI
        Task<Void> task = new Task<>() {
            List<Candidature> candidatures;
            List<Evaluation>  evaluations;

            @Override
            protected Void call() throws Exception {
                candidatures = candidatureService.getAllCandidatures(true);
                evaluations  = evaluationService.getAllEvaluations(true);
                return null;
            }

            @Override
            protected void succeeded() {
                updateKPIs(candidatures, evaluations);
                buildChartBar(candidatures);
                buildChartPie(candidatures);
                buildChartHisto(evaluations);
            }

            @Override
            protected void failed() {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de charger les données", getException().getMessage());
            }
        };
        new Thread(task, "dashboard-loader").start();
    }

    // ══════════════════════════════════════════════════════════════
    //  KPI
    // ══════════════════════════════════════════════════════════════

    private void updateKPIs(List<Candidature> list, List<Evaluation> evals) {
        int total    = list.size();
        long validees = list.stream().filter(c -> "VALIDEE".equalsIgnoreCase(c.getStatut())).count();
        long refusees = list.stream().filter(c -> "REFUSEE".equalsIgnoreCase(c.getStatut())).count();
        long attente  = list.stream().filter(c -> "EN_ATTENTE".equalsIgnoreCase(c.getStatut())).count();

        OptionalDouble moyScore = evals.stream()
                .filter(e -> e.getNoteGlobale() != null)
                .mapToDouble(Evaluation::getNoteGlobale)
                .average();

        Platform.runLater(() -> {
            kpiTotalCandidatures.setText(String.valueOf(total));
            kpiTotalSub.setText("total soumises");

            kpiAcceptees.setText(String.valueOf(validees));
            kpiAccepteesPct.setText(total > 0
                    ? String.format("%.0f%% du total", validees * 100.0 / total) : "—");

            kpiRefusees.setText(String.valueOf(refusees));
            kpiRefuseesPct.setText(total > 0
                    ? String.format("%.0f%% du total", refusees * 100.0 / total) : "—");

            kpiEnAttente.setText(String.valueOf(attente));
            kpiAttentePct.setText(total > 0
                    ? String.format("%.0f%% du total", attente * 100.0 / total) : "—");

            kpiScoreMoyen.setText(moyScore.isPresent()
                    ? String.format("%.1f", moyScore.getAsDouble()) : "—");
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHIQUE 1 : Barres — Candidatures par mois
    // ══════════════════════════════════════════════════════════════

    private void buildChartBar(List<Candidature> list) {
        // Compter par mois (6 derniers mois)
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) counts.put(now.minusMonths(i), 0L);

        for (Candidature c : list) {
            if (c.getDateDepot() != null) {
                YearMonth ym = YearMonth.from(c.getDateDepot().toLocalDate());
                if (counts.containsKey(ym))
                    counts.merge(ym, 1L, Long::sum);
            }
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        counts.forEach((ym, count) -> {
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                    + " " + ym.getYear();
            dataset.addValue(count, "Candidatures", label);
        });

        chartBar = ChartFactory.createBarChart(
                null, null, "Nombre", dataset,
                PlotOrientation.VERTICAL, false, true, false);

        styleBarChart(chartBar);
        embedChart(chartBar, chartBarContainer);
    }

    private void styleBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(C_BG);
        chart.setBorderVisible(false);
        chart.setPadding(new RectangleInsets(10, 10, 10, 10));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(C_BG);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(C_BORDER);
        plot.setRangeGridlineStroke(new BasicStroke(1f));
        plot.getRangeAxis().setLabelPaint(C_MUTED);
        plot.getRangeAxis().setTickLabelPaint(C_MUTED);
        plot.getRangeAxis().setAxisLinePaint(C_BORDER);
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.getDomainAxis().setTickLabelPaint(C_MUTED);
        plot.getDomainAxis().setAxisLinePaint(C_BORDER);
        plot.getDomainAxis().setCategoryMargin(0.3);
        ((CategoryAxis) plot.getDomainAxis()).setMaximumCategoryLabelLines(2);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, C_INDIGO);
        renderer.setMaximumBarWidth(0.12);
        renderer.setItemMargin(0.05);
        // Valeurs au-dessus des barres
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(C_TEXT);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHIQUE 2 : Camembert — Taux d'acceptation
    // ══════════════════════════════════════════════════════════════

    private void buildChartPie(List<Candidature> list) {
        long validees = list.stream().filter(c -> "VALIDEE".equalsIgnoreCase(c.getStatut())).count();
        long refusees = list.stream().filter(c -> "REFUSEE".equalsIgnoreCase(c.getStatut())).count();
        long attente  = list.stream().filter(c -> "EN_ATTENTE".equalsIgnoreCase(c.getStatut())).count();

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        if (validees > 0) dataset.setValue("✅ Validées ("  + validees + ")", validees);
        if (refusees > 0) dataset.setValue("❌ Refusées ("  + refusees + ")", refusees);
        if (attente  > 0) dataset.setValue("⏳ En attente (" + attente  + ")", attente);
        if (list.isEmpty()) dataset.setValue("Aucune donnée", 1);

        chartPie = ChartFactory.createPieChart(null, dataset, true, true, false);

        chartPie.setBackgroundPaint(C_BG);
        chartPie.setBorderVisible(false);
        chartPie.setPadding(new RectangleInsets(10, 10, 10, 10));

        PiePlot<String> plot = (PiePlot<String>) chartPie.getPlot();
        plot.setBackgroundPaint(C_BG);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelBackgroundPaint(Color.WHITE);
        plot.setLabelOutlinePaint(C_BORDER);
        plot.setLabelShadowPaint(null);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        plot.setLabelPaint(C_TEXT);

        dataset.getKeys().forEach(key -> {
            String k = key.toString();
            if (k.startsWith("✅")) plot.setSectionPaint(key, C_GREEN);
            else if (k.startsWith("❌")) plot.setSectionPaint(key, C_RED);
            else if (k.startsWith("⏳")) plot.setSectionPaint(key, C_ORANGE);
            else plot.setSectionPaint(key, C_BORDER);
        });

        // Explosion légère de la part la plus grande
        if (validees >= refusees && validees >= attente && validees > 0)
            dataset.getKeys().stream().filter(k -> k.toString().startsWith("✅"))
                    .findFirst().ifPresent(k -> plot.setExplodePercent(k, 0.04));

        embedChart(chartPie, chartPieContainer);
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHIQUE 3 : Histogramme — Distribution des scores
    // ══════════════════════════════════════════════════════════════

    private void buildChartHisto(List<Evaluation> evals) {
        // Tranches : 0-2, 2-4, 4-6, 6-8, 8-10
        String[] labels = {"0–2", "2–4", "4–6", "6–8", "8–10"};
        int[] bins = new int[5];

        for (Evaluation e : evals) {
            if (e.getNoteGlobale() != null) {
                double note = e.getNoteGlobale();
                int idx = Math.min((int)(note / 2), 4);
                bins[idx]++;
            }
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < 5; i++) {
            dataset.addValue(bins[i], "Évaluations", labels[i]);
        }

        chartHisto = ChartFactory.createBarChart(
                null, "Score /10", "Nombre d'évaluations", dataset,
                PlotOrientation.VERTICAL, false, true, false);

        chartHisto.setBackgroundPaint(C_BG);
        chartHisto.setBorderVisible(false);
        chartHisto.setPadding(new RectangleInsets(10, 10, 10, 10));

        CategoryPlot plot = chartHisto.getCategoryPlot();
        plot.setBackgroundPaint(C_BG);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(C_BORDER);
        plot.setRangeGridlineStroke(new BasicStroke(1f));
        plot.getRangeAxis().setLabelPaint(C_MUTED);
        plot.getRangeAxis().setTickLabelPaint(C_MUTED);
        plot.getRangeAxis().setAxisLinePaint(C_BORDER);
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.getDomainAxis().setTickLabelPaint(C_TEXT);
        plot.getDomainAxis().setAxisLinePaint(C_BORDER);
        plot.getDomainAxis().setLabelPaint(C_MUTED);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.10);

        // Dégradé de couleur selon la tranche (rouge → vert)
        Color[] barColors = {C_RED, C_ORANGE, new Color(0xfb, 0xbf, 0x24),
                new Color(0x34, 0xd3, 0x99), C_GREEN};
        for (int i = 0; i < 5; i++)
            renderer.setSeriesPaint(0, C_VIOLET); // même série → couleur par item via custom

        // Couleur par colonne
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(C_TEXT);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));

        // Couleurs différentes par colonne (via ItemPainter non disponible simplement → on colore par valeur)
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            renderer.setSeriesPaint(0, C_VIOLET); // fallback violet
        }
        // Coloration progressive
        Color[] cols = {C_RED, C_ORANGE, new Color(0xfb,0xbf,0x24), new Color(0x34,0xd3,0x99), C_GREEN};
        BarRenderer coloredRenderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int col) {
                return cols[Math.min(col, cols.length - 1)];
            }
        };
        coloredRenderer.setBarPainter(new StandardBarPainter());
        coloredRenderer.setShadowVisible(false);
        coloredRenderer.setMaximumBarWidth(0.12);
        coloredRenderer.setDefaultItemLabelsVisible(true);
        coloredRenderer.setDefaultItemLabelPaint(C_TEXT);
        coloredRenderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
        plot.setRenderer(coloredRenderer);

        embedChart(chartHisto, chartHistoContainer);
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER : embed JFreeChart dans un StackPane JavaFX
    // ══════════════════════════════════════════════════════════════

    private void embedChart(JFreeChart chart, StackPane container) {
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> {
            ChartPanel cp = new ChartPanel(chart);
            cp.setBackground(new Color(0xf7, 0xf8, 0xfc));
            cp.setBorder(null);
            cp.setPopupMenu(null); // désactiver menu contextuel swing
            cp.setMouseWheelEnabled(false);
            swingNode.setContent(cp);
        });
        Platform.runLater(() -> {
            container.getChildren().setAll(swingNode);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  EXPORT PNG
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void exporterGraphiques() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choisir le dossier d'export");
        Stage stage = (Stage) chartBarContainer.getScene().getWindow();
        File dir = dc.showDialog(stage);
        if (dir == null) return;

        try {
            if (chartBar   != null) ChartUtils.saveChartAsPNG(
                    new File(dir, "candidatures_par_mois.png"), chartBar, 900, 320);
            if (chartPie   != null) ChartUtils.saveChartAsPNG(
                    new File(dir, "taux_acceptation.png"), chartPie, 480, 320);
            if (chartHisto != null) ChartUtils.saveChartAsPNG(
                    new File(dir, "distribution_scores.png"), chartHisto, 900, 280);

            AlertUtils.showInfo("Export réussi",
                    "3 graphiques exportés en PNG dans :\n" + dir.getAbsolutePath());
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur d'export",
                    "Impossible d'exporter", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  REFRESH
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void refreshDashboard() { initialize(null, null); }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION SIDEBAR
    // ══════════════════════════════════════════════════════════════

    @FXML private void goToDashboard() { /* déjà ici */ }

    @FXML private void goToCandidatures() {
        openView("/fxml/CandidatureBackofficeView.fxml", "Candidatures");
    }
    @FXML private void goToDossiers() {
        openView("/fxml/DossierCandidatureBackofficeView.fxml", "Dossiers");
    }
    @FXML private void goToEvaluations() {
        openView("/fxml/EvaluationBackofficeView.fxml", "Évaluations");
    }

    private void openView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            HBox root = loader.load();
            Scene scene = new Scene(root, 1200, 750);
            loadCss(scene);
            Stage stage = new Stage();
            stage.setTitle("BOOSTUP — Back Office — " + title);
            stage.setScene(scene);
            stage.setMinWidth(900); stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();
            ((Stage) chartBarContainer.getScene().getWindow()).close();
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la vue", e.getMessage());
        }
    }

    private void loadCss(Scene scene) {
        for (String p : new String[]{"/style.css", "/css/style.css"}) {
            URL u = getClass().getResource(p);
            if (u != null) { scene.getStylesheets().add(u.toExternalForm()); return; }
        }
    }
}