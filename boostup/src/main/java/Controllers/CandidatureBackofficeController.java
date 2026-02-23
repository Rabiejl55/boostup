package Controllers;

import entities.GCandidature.Candidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CandidatureBackofficeController implements Initializable {

    @FXML private TableView<Candidature> candidatureTable;
    @FXML private TableColumn<Candidature, String> nomCandidatureColumn;
    @FXML private TableColumn<Candidature, String> nomStartupColumn;
    @FXML private TableColumn<Candidature, java.sql.Date> dateDepotColumn;
    @FXML private TableColumn<Candidature, String> statutColumn;
    @FXML private TableColumn<Candidature, Double> scoreColumn;
    @FXML private TableColumn<Candidature, String> commentaireColumn;

    @FXML private Button backButton;

    private final CandidatureService service = new CandidatureService();
    private final ObservableList<Candidature> candidatureList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Bindings clairs et dans le même ordre que le FXML
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        nomStartupColumn.setCellValueFactory(new PropertyValueFactory<>("nomStartup"));
        dateDepotColumn.setCellValueFactory(new PropertyValueFactory<>("dateDepot"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        commentaireColumn.setCellValueFactory(new PropertyValueFactory<>("commentaire"));

        candidatureTable.setItems(candidatureList);
        refreshTable();
    }

    @FXML
    private void hideSelected() {
        Candidature selected = candidatureTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                service.hideCandidature(selected.getIdCandidature());
                refreshTable();
            } catch (SQLException e) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de cacher la candidature", e.getMessage());
            }
        }
    }

    @FXML
    void refreshTable() {
        candidatureList.clear();
        try {
            candidatureList.addAll(service.getAllCandidatures(false));
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les candidatures", e.getMessage());
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
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de retourner au menu", e.getMessage());
        }
    }
}