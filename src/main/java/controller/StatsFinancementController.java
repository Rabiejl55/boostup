package controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import services.FinancementService.FinancementStatsService;

public class StatsFinancementController {

    @FXML private Label lblTotalInvesti;
    @FXML private Label lblNbInvest;
    @FXML private Label lblNbInvestFinances;
    @FXML private Label lblTauxReussite;

    @FXML private TableView<FinancementStatsService.TopProjet> tableTopProjets;
    @FXML private TableColumn<FinancementStatsService.TopProjet, String> colTopTitre;
    @FXML private TableColumn<FinancementStatsService.TopProjet, Double> colTopCollecte;

    @FXML private PieChart pieTxStatuts;
    @FXML private LineChart<String, Number> lineInvestMois;

    private final FinancementStatsService stats = new FinancementStatsService();

    @FXML
    public void initialize() {
        colTopTitre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().titre));
        colTopCollecte.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().collecte));

        refreshStats();
    }

    @FXML
    private void refreshStats() {
        try {
            lblTotalInvesti.setText(String.format("%.2f", stats.totalInvesti()));
            lblNbInvest.setText(String.valueOf(stats.countInvestissements()));
            lblNbInvestFinances.setText(String.valueOf(stats.countInvestissementsFinances()));
            lblTauxReussite.setText(String.format("%.1f%%", stats.tauxReussiteProjets()));

            tableTopProjets.setItems(FXCollections.observableArrayList(stats.topProjetsCollecte(5)));

            // Pie chart
            pieTxStatuts.setData(FXCollections.observableArrayList());
            for (var sc : stats.transactionsParStatut()) {
                pieTxStatuts.getData().add(new PieChart.Data(sc.statut, sc.count));
            }

            // Line chart
            lineInvestMois.getData().clear();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName("Investissements (FINANCE) / mois");
            for (var mm : stats.investissementsParMois()) {
                s.getData().add(new XYChart.Data<>(mm.mois, mm.total));
            }
            lineInvestMois.getData().add(s);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}