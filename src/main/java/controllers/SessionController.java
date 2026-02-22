package controllers;
import services.AccompagnementService.SessionNotificationService;
import services.AccompagnementService.NotificationScheduler;
import entities.GAccompagnement.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.AccompagnementService.SessionService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import java.sql.SQLException;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javafx.scene.control.Alert;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionController {

    private final SessionService sessionService = new SessionService();

    // ================= UI CONTROLS =================

    @FXML
    private ImageView avatarImageView;
    @FXML
    private Label welcomeLabel;

    @FXML
    private TableView<Session> sessionsTable;
    @FXML
    private TableColumn<Session, String> dateColumn;
    @FXML
    private TableColumn<Session, String> dureeColumn;
    @FXML
    private TableColumn<Session, String> lieuColumn;
    @FXML
    private TableColumn<Session, String> typeColumn;
    @FXML
    private TableColumn<Session, String> objectifColumn;
    @FXML
    private TableColumn<Session, String> coachColumn;
    @FXML
    private TableColumn<Session, String> domaineColumn;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        try {
            // =================== Notifications ===================
            List<Session> allSessions = sessionService.afficherAll();
            SessionNotificationService notificationService = new SessionNotificationService(allSessions);
            NotificationScheduler scheduler = new NotificationScheduler(notificationService);
            scheduler.start();

            // =================== Initialisation TableView ===================
            dateColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getDateSession().toString()));
            dureeColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.valueOf(cell.getValue().getDuree())));
            lieuColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getLieu()));
            typeColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getTypeSession()));
            objectifColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getObjectif()));
            coachColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getCoach() != null
                            ? cell.getValue().getCoach().getNom() + " " + cell.getValue().getCoach().getPrenom()
                            : "Aucun"));
            domaineColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getDomaine() != null
                            ? cell.getValue().getDomaine().getNom()
                            : "Aucun"));

            // Chargement des sessions
            loadSessionsTable();

            // Avatar par défaut
            try {
                FileInputStream fis = new FileInputStream("src/images/default_avatar.png");
                avatarImageView.setImage(new Image(fis));
            } catch (FileNotFoundException e) {
                System.out.println("Avatar par défaut non trouvé");
            }

            // Nom du profil
            welcomeLabel.setText("Administrateur");

        } catch (SQLException e) {
            feedbackLabel.setText("Erreur lors du chargement des sessions : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= LOAD SESSIONS =================
    private void loadSessionsTable() {
        try {
            List<Session> sessions = sessionService.afficherAll();
            sessionsTable.setItems(FXCollections.observableArrayList(sessions));
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement des sessions : " + e.getMessage());
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

            loadSessionsTable();
        } catch (Exception e) {
            feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private Label feedbackLabel;
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

                loadSessionsTable();            } catch (Exception e) {
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
                loadSessionsTable();                feedbackLabel.setText("Session supprimée avec succès.");
            } catch (Exception e) {
                feedbackLabel.setText("Erreur suppression : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner une session à supprimer.");
        }
    }
    @FXML
    private void handleLogout() {
        System.out.println("Déconnexion !");
        // TODO: redirection vers login
    }

    @FXML
    private void goToProfile() {
        // TODO: navigation vers profile
    }

    @FXML
    private void goToHome() {
        // TODO: navigation vers tableau de bord
    }
    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void viewSessions() {
        // TODO: rester sur cette page
    }
    @FXML private void goToDomaine(ActionEvent event) { navigateTo(event, "/fxml/Domaine.fxml", "Gestion Domaines"); }
    @FXML private void goToCoach(ActionEvent event) { navigateTo(event, "/fxml/accompagnement.fxml", "Gestion Domaines"); }
    @FXML private void goToHome(ActionEvent event) { navigateTo(event, "/fxml/home.fxml", "Gestion Domaines"); }
    @FXML
    private void onParticiperButtonClicked() {
        Session selected = sessionsTable.getSelectionModel().getSelectedItem();
        int selectedUserId = 1; // temporaire : ID d'utilisateur pour test
        if (selected != null) {
            sessionService.participerSession(selected.getIdSession(), selectedUserId);
            feedbackLabel.setText("Participation ajoutée !");
        } else {
            feedbackLabel.setText("Veuillez sélectionner une session.");
        }
    }
}