package Controllers;

import entities.GCandidature.Evaluation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.EvaluationService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EvaluationBackofficeController implements Initializable {

    @FXML private TableView<Evaluation> evaluationTable;
    @FXML private TableColumn<Evaluation, String> nomCandidatureColumn;
    @FXML private TableColumn<Evaluation, Integer> noteInnovationColumn;
    @FXML private TableColumn<Evaluation, Integer> noteViabiliteColumn;
    @FXML private TableColumn<Evaluation, Integer> noteMarcheColumn;
    @FXML private TableColumn<Evaluation, Integer> noteEquipeColumn;
    @FXML private TableColumn<Evaluation, Double> noteGlobaleColumn;
    @FXML private TableColumn<Evaluation, String> decisionColumn;

    @FXML private Button backButton;

    private final EvaluationService service = new EvaluationService();
    private final ObservableList<Evaluation> evaluationList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        noteInnovationColumn.setCellValueFactory(new PropertyValueFactory<>("noteInnovation"));
        noteViabiliteColumn.setCellValueFactory(new PropertyValueFactory<>("noteViabilite"));
        noteMarcheColumn.setCellValueFactory(new PropertyValueFactory<>("noteMarche"));
        noteEquipeColumn.setCellValueFactory(new PropertyValueFactory<>("noteEquipe"));
        noteGlobaleColumn.setCellValueFactory(new PropertyValueFactory<>("noteGlobale"));
        decisionColumn.setCellValueFactory(new PropertyValueFactory<>("decision"));

        evaluationTable.setItems(evaluationList);
        refreshTable();
    }

    @FXML private void openAddForm() {
        openForm(null);
    }

    @FXML private void openEditForm() {
        Evaluation selected = evaluationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        } else {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection", null, "Veuillez sélectionner une évaluation à modifier.");
        }
    }

    private void openForm(Evaluation evaluation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EvaluationForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(evaluation == null ? "Ajouter Évaluation" : "Modifier Évaluation");

            EvaluationFormController controller = loader.getController();
            controller.setEvaluation(evaluation);
            controller.setParentController(this);

            stage.showAndWait();
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire", e.getMessage());
        }
    }

    @FXML private void hideSelected() {
        Evaluation selected = evaluationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showAlert(Alert.AlertType.WARNING, "Aucune sélection", null, "Veuillez sélectionner une évaluation à cacher.");
            return;
        }

        try {
            service.hideEvaluation(selected.getIdEvaluation());
            AlertUtils.showInfo("Succès", "Évaluation cachée avec succès");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de cacher l'évaluation", e.getMessage());
            e.printStackTrace(); // ← on garde temporairement pour debug
        }
    }

    @FXML public void refreshTable() {
        evaluationList.clear();
        try {
            evaluationList.addAll(service.getAllEvaluations(true)); // true = seulement visibles
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les évaluations", e.getMessage());
            e.printStackTrace(); // ← debug temporaire
        }
    }

    @FXML
    private void goBackToMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBackoffice.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("BoostUp - Backoffice - Menu");
            stage.centerOnScreen();

            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) {
                scene.getStylesheets().add(css);
            }
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au menu", e.getMessage());
            e.printStackTrace(); // ← debug temporaire
        }
    }
}