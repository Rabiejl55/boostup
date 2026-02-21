package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;

public class AdminDashboardController {

    @FXML private ImageView avatarImageView;

    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;

    @FXML private Label statsUsersLabel;
    @FXML private Label statsEventsLabel;
    @FXML private Label statsStartupsLabel;

    @FXML
    private void initialize() {
        // Avatar (optionnel)
        if (avatarImageView != null) {
            try {
                URL url = getClass().getResource("/images/logo.png");
                if (url != null) {
                    avatarImageView.setImage(new Image(url.toExternalForm(), true));
                }
            } catch (Exception ignored) {
            }
        }

        // Valeurs par défaut
        if (welcomeLabel != null && (welcomeLabel.getText() == null || welcomeLabel.getText().isBlank())) {
            welcomeLabel.setText("admin");
        }
        if (userInfoLabel != null && (userInfoLabel.getText() == null || userInfoLabel.getText().isBlank())) {
            userInfoLabel.setText("ADMIN");
        }

        // Stats placeholder (sans toucher à la logique métier)
        if (statsUsersLabel != null) statsUsersLabel.setText("0 utilisateurs");
        if (statsEventsLabel != null) statsEventsLabel.setText("0 événements");
        if (statsStartupsLabel != null) statsStartupsLabel.setText("0 startups");
    }

    // ===== Navigation sidebar =====

    @FXML
    private void goToDashboard() {
        // Déjà sur le dashboard : rien à faire
    }

    @FXML
    private void manageEvents() {
        openScene("/EvenementView.fxml", "Gestion des Événements - BoostUp");
    }

    @FXML
    private void openUsersManagement() {
        showInfo("Pas encore intégré", "La gestion des utilisateurs sera branchée ici.");
    }

    @FXML
    private void manageFinancements() {
        showInfo("Pas encore intégré", "La gestion des financements sera branchée ici.");
    }

    @FXML
    private void viewStats() {
        showInfo("Pas encore intégré", "Les statistiques seront branchées ici.");
    }

    @FXML
    private void moderateContent() {
        showInfo("Pas encore intégré", "La modération sera branchée ici.");
    }

    @FXML
    private void systemSettings() {
        showInfo("Pas encore intégré", "Les paramètres système seront branchés ici.");
    }

    @FXML
    private void viewStartups() {
        showInfo("Pas encore intégré", "La section Startups sera branchée ici.");
    }

    @FXML
    private void goToProfile() {
        showInfo("Profil", "Écran profil admin à brancher.");
    }

    @FXML
    private void handleLogout() {
        openScene("/views/login.fxml", "BoostUp - Connexion");
    }

    // ===== Helpers =====

    private void openScene(String fxmlPath, String title) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            if (fxml == null) {
                showError("FXML introuvable", fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1200, 800));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle(title);
        } catch (Exception e) {
            showError("Impossible d'ouvrir l'écran", e.toString());
        }
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

