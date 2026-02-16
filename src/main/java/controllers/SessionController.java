package controllers;

import entities.GAccompagnement.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import services.AccompagnementService.SessionService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SessionController {

    private final SessionService sessionService = new SessionService();

    // Table et colonnes
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> dateColumn;
    @FXML private TableColumn<Session, Integer> dureeColumn;
    @FXML private TableColumn<Session, String> lieuColumn;
    @FXML private TableColumn<Session, String> typeColumn;
    @FXML private TableColumn<Session, String> objectifColumn;
    @FXML private TableColumn<Session, String> coachColumn;
    @FXML private TableColumn<Session, String> domaineColumn; // <-- Colonne pour le domaine

    @FXML private Label feedbackLabel;

    // Sidebar
    @FXML private ImageView avatarImageView;
    @FXML private Label welcomeLabel;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateSession"));
        dureeColumn.setCellValueFactory(new PropertyValueFactory<>("duree"));
        lieuColumn.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeSession"));
        objectifColumn.setCellValueFactory(new PropertyValueFactory<>("objectif"));

        coachColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCoach() != null) {
                return new SimpleStringProperty(
                        cellData.getValue().getCoach().getNom() + " " +
                                cellData.getValue().getCoach().getPrenom()
                );
            }
            return new SimpleStringProperty("Aucun");
        });

        domaineColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDomaine() != null) {
                return new SimpleStringProperty(cellData.getValue().getDomaine().getNom());
            }
            return new SimpleStringProperty("Aucun");
        });

        loadSessions();

        try {
            FileInputStream fis = new FileInputStream("src/images/default_avatar.png");
            avatarImageView.setImage(new Image(fis));
        } catch (FileNotFoundException e) {
            System.out.println("Avatar par défaut non trouvé");
        }

        welcomeLabel.setText("Administrateur");
    }

    // ================= LOAD SESSIONS =================
    private void loadSessions() {
        try {
            sessionsTable.setItems(FXCollections.observableArrayList(sessionService.afficherAll()));
            feedbackLabel.setText("Chargement réussi.");
        } catch (Exception e) {
            feedbackLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION =================
    @FXML
    private void goToFront(ActionEvent event) {
        navigateTo(event, "/fxml/home.fxml", "Dashboard");
    }

    @FXML
    private void goToCoach(ActionEvent event) {
        navigateTo(event, "/fxml/accompagnement.fxml", "Gestion Coachs");
    }

    @FXML
    private void goToDomaine(ActionEvent event) {
        navigateTo(event, "/fxml/Domaine.fxml", "Gestion Domaines");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= ACTIONS =================
    @FXML
    private void ajouterSession() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddSessionForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Session");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadSessions();
        } catch (Exception e) {
            feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierSession() {
        Session selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditSessionForm.fxml"));
                Parent root = loader.load();

                EditSessionController controller = loader.getController();
                controller.setSession(selected);

                Stage stage = new Stage();
                stage.setTitle("Modifier Session");
                stage.setScene(new Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();

                loadSessions();
            } catch (Exception e) {
                feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner une session à modifier.");
        }
    }

    @FXML
    private void supprimerSession() {
        Session selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                sessionService.supprimer(selected.getIdSession());
                loadSessions();
                feedbackLabel.setText("Session supprimée avec succès.");
            } catch (Exception e) {
                feedbackLabel.setText("Erreur suppression : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner une session à supprimer.");
        }
    }

    @FXML
    private void dummyAction(ActionEvent event) {
        System.out.println("Bouton cliqué !");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        feedbackLabel.setText("Déconnexion !");
        // redirection vers login
    }
}
