package Controllers;

import entities.GCandidature.DossierCandidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import services.CandidatureService.DossierCandidatureService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DossierCandidatureBackofficeController implements Initializable {

    @FXML private TableView<DossierCandidature> dossierTable;
    @FXML private TableColumn<DossierCandidature, Integer> idColumn;
    @FXML private TableColumn<DossierCandidature, Integer> nomCandidatureColumn;
    @FXML private TableColumn<DossierCandidature, String> descriptionColumn;
    @FXML private TableColumn<DossierCandidature, String> businessPlanColumn;
    @FXML private TableColumn<DossierCandidature, java.sql.Date> dateCreationColumn;
    @FXML private TableColumn<DossierCandidature, String> etatColumn;
    @FXML private TableColumn<DossierCandidature, Boolean> visibleColumn;

    @FXML private Button backButton;

    private final DossierCandidatureService service = new DossierCandidatureService();
    private final ObservableList<DossierCandidature> dossierList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idDossier"));
        nomCandidatureColumn.setCellValueFactory(new PropertyValueFactory<>("nomCandidature"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("descriptionProjet"));
        businessPlanColumn.setCellValueFactory(new PropertyValueFactory<>("businessPlan"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        etatColumn.setCellValueFactory(new PropertyValueFactory<>("etat"));
        visibleColumn.setCellValueFactory(new PropertyValueFactory<>("visible"));

        dossierTable.setItems(dossierList);
        refreshTable();
    }

    @FXML
    private void hideSelected() {
        DossierCandidature selected = dossierTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                service.hideDossier(selected.getIdDossier());
                refreshTable();
            } catch (SQLException e) {
                AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de cacher le dossier", e.getMessage());
            }
        }
    }

    @FXML
    private void refreshTable() {
        dossierList.clear();
        try {
            dossierList.addAll(service.getAllDossiers(false));
        } catch (SQLException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les dossiers", e.getMessage());
        }
    }

    @FXML
    private void goBackToMenu() {
        loadMenu();
    }

    private void loadMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBackoffice.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("BoostUp - Backoffice - Menu");
            stage.centerOnScreen();

            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (css != null) scene.getStylesheets().add(css);
        } catch (IOException e) {
            AlertUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au menu", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}