package controllers;

import entities.GAccompagnement.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.AccompagnementService.SessionService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

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
        // Vérification injection FXML

        // Initialisation des colonnes (conversion int/date en String)
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

        // Chargement des sessions dans le tableau
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
        // TODO: ouverture formulaire AddSessionForm
        loadSessionsTable();
    }

    @FXML
    private void modifierSession() {
        // TODO: modification depuis TableView
    }

    @FXML
    private void supprimerSession() {
        // TODO: suppression depuis TableView
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

    @FXML
    private void viewSessions() {
        // TODO: rester sur cette page
    }

    @FXML
    private void goToCoach() {
        // TODO: navigation vers gestion coach
    }

    @FXML
    private void goToDomaine() {
        // TODO: navigation vers gestion domaine
    }
}