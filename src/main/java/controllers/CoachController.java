package controllers;

import entities.GAccompagnement.Coach;
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
import services.AccompagnementService.CoachService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CoachController {

    private final CoachService coachService = new CoachService();

    // ===== TABLE COACH =====
    @FXML private TableView<Coach> coachsTable;
    @FXML private TableColumn<Coach, String> nomColumn;
    @FXML private TableColumn<Coach, String> prenomColumn;
    @FXML private TableColumn<Coach, String> emailColumn;
    @FXML private TableColumn<Coach, String> telephoneColumn;
    @FXML private TableColumn<Coach, String> imageColumn; // nouvelle colonne image

    @FXML private Label feedbackLabel;

    // ===== SIDEBAR AVATAR =====
    @FXML private ImageView avatarImageView;
    @FXML private Label welcomeLabel;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        // Lier les colonnes
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("imagecoach")); // colonne image

        // Colonne image personnalisée
        imageColumn.setCellFactory(column -> new TableCell<Coach, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new FileInputStream(imagePath));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                        System.out.println("Image non trouvée : " + imagePath);
                    }
                }
            }
        });

        // Charger les coachs
        loadCoachs();

        // Avatar sidebar par défaut
        try {
            FileInputStream fis = new FileInputStream("src/images/default_avatar.png");
            avatarImageView.setImage(new Image(fis));
        } catch (FileNotFoundException e) {
            System.out.println("Avatar par défaut non trouvé");
        }
        welcomeLabel.setText("Administrateur");

        // Mettre à jour l'avatar sidebar selon le coach sélectionné
        coachsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                String path = newSel.getImagecoach();
                if (path != null && !path.isEmpty()) {
                    try {
                        FileInputStream fis = new FileInputStream(path);
                        avatarImageView.setImage(new Image(fis));
                    } catch (FileNotFoundException e) {
                        System.out.println("Image du coach non trouvée : " + path);
                    }
                } else {
                    try {
                        FileInputStream fis = new FileInputStream("src/images/default_avatar.png");
                        avatarImageView.setImage(new Image(fis));
                    } catch (FileNotFoundException e) {
                        System.out.println("Avatar par défaut non trouvé");
                    }
                }
            }
        });
    }

    // ================= LOAD DATA =================
    private void loadCoachs() {
        try {
            coachsTable.setItems(FXCollections.observableArrayList(coachService.afficherAll()));
            feedbackLabel.setText("Chargement réussi.");
        } catch (Exception e) {
            feedbackLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION =================
    @FXML private void goToFront(ActionEvent event) {
        navigateTo(event, "/fxml/home.fxml", "HomePage");
    }
    @FXML private void goToDomaine(ActionEvent event) {
        navigateTo(event, "/fxml/Domaine.fxml", "Gestion Domaines");
    }
    @FXML private void goToSession(ActionEvent event) {
        navigateTo(event, "/fxml/Session.fxml", "Gestion Sessions");
    }

    @FXML private void handleLogout(ActionEvent event) {
        feedbackLabel.setText("Déconnexion !");
        // TODO : redirection vers login
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

    // ================= ACTIONS TABLE =================
    @FXML
    private void ajouterCoach() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddCoachForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Coach");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadCoachs();
        } catch (Exception e) {
            feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierCoach() {
        Coach selected = coachsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditCoachForm.fxml"));
                Parent root = loader.load();
                EditCoachController controller = loader.getController();
                controller.setCoach(selected);
                Stage stage = new Stage();
                stage.setTitle("Modifier Coach");
                stage.setScene(new Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadCoachs();
            } catch (Exception e) {
                feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner un coach à modifier.");
        }
    }

    @FXML
    private void supprimerCoach() {
        Coach selected = coachsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                coachService.supprimer(selected.getIdCoach());
                loadCoachs();
                feedbackLabel.setText("Coach supprimé avec succès.");
            } catch (Exception e) {
                feedbackLabel.setText("Erreur suppression : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner un coach à supprimer.");
        }
    }
}
