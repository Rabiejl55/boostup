package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import services.FinancementService.FinancementStatsService;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Map;

public class StatsFinancementController {

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> cbStatutProjet;
    @FXML private Button btnRefreshStats;

    // KPI labels
    @FXML private Label lblKpiTotalLeve;
    @FXML private Label lblKpiNbProjets;
    @FXML private Label lblKpiNbInvest;
    @FXML private Label lblKpiTauxFinancement;
    @FXML private Label lblStatsToast;

    // Charts
    @FXML private BarChart<String, Number> barTopProjets;
    @FXML private CategoryAxis axisTopProjetsX;
    @FXML private NumberAxis axisTopProjetsY;

    @FXML private PieChart pieModesPaiement;

    @FXML private LineChart<String, Number> lineEvolution;
    @FXML private CategoryAxis axisLineX;
    @FXML private NumberAxis axisLineY;

    private final FinancementStatsService statsService = new FinancementStatsService();
    private final DecimalFormat df = new DecimalFormat("#0.00");

    private static final String[] STATUTS = {"TOUS", "EN_ATTENTE", "FINANCE", "REFUSE"};

    @FXML
    public void initialize() {
        cbStatutProjet.setItems(FXCollections.observableArrayList(STATUTS));
        cbStatutProjet.getSelectionModel().selectFirst();

        // dates par défaut : 90 jours
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusDays(90));

        // auto refresh
        dpFrom.valueProperty().addListener((obs,o,n)-> refresh());
        dpTo.valueProperty().addListener((obs,o,n)-> refresh());
        cbStatutProjet.valueProperty().addListener((obs,o,n)-> refresh());

        refresh();
    }

    @FXML
    private void refresh() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String st = cbStatutProjet.getValue();

        if (from != null && to != null && from.isAfter(to)) {
            showToast("❌ Date début > date fin", "toastError");
            return;
        }

        try {
            // KPI
            double total = statsService.totalLeve(from, to, st);
            int nbProjets = statsService.nbProjets(st);
            int nbInv = statsService.nbInvestissements(from, to, st);
            double taux = statsService.tauxFinancement(from, to, st);

            lblKpiTotalLeve.setText(df.format(total));
            lblKpiNbProjets.setText(String.valueOf(nbProjets));
            lblKpiNbInvest.setText(String.valueOf(nbInv));
            lblKpiTauxFinancement.setText(df.format(taux) + " %");

            // Bar Top projets
            barTopProjets.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Top projets (levé)");

            for (FinancementStatsService.TopProjet p : statsService.topProjets(from, to, st, 7)) {
                String name = (p.titre == null || p.titre.isBlank()) ? ("Projet #" + p.idProjet) : p.titre;
                if (name.length() > 18) name = name.substring(0, 18) + "...";
                series.getData().add(new XYChart.Data<>(name, p.total));
            }
            barTopProjets.getData().add(series);

            // Pie modes paiement
            pieModesPaiement.getData().clear();
            Map<String, Double> map = statsService.repartitionModesPaiement(from, to);
            map.forEach((mode, val) -> pieModesPaiement.getData().add(new PieChart.Data(mode, val)));

            // Line évolution mensuelle
            lineEvolution.getData().clear();
            XYChart.Series<String, Number> sLine = new XYChart.Series<>();
            sLine.setName("Investissements FINANCE (par mois)");

            for (FinancementStatsService.MoisPoint pt : statsService.evolutionMensuelle(from, to, st)) {
                sLine.getData().add(new XYChart.Data<>(pt.mois, pt.total));
            }
            lineEvolution.getData().add(sLine);

            showToast("✅ Stats mises à jour", "toastSuccess");

        } catch (Exception e) {
            showToast("❌ Erreur stats: " + e.getMessage(), "toastError");
            e.printStackTrace();
        }
    }

    private void showToast(String msg, String cssClass) {
        if (lblStatsToast == null) return;
        lblStatsToast.getStyleClass().removeAll("toastInfo","toastSuccess","toastError");
        lblStatsToast.getStyleClass().add(cssClass);
        lblStatsToast.setText(msg);
        lblStatsToast.setVisible(true);
        lblStatsToast.setManaged(true);
    }
}