package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;

public class HomePageController {

    @FXML private Label welcomeLabel;
    @FXML private Label userRoleLabel;
    @FXML private ImageView avatarImageView;

    // Simple: on donne le prénom/role via une méthode d'initialisation après login
    public void setUser(String username, String role) {
        if (welcomeLabel != null) {
            welcomeLabel.setText(username == null ? "" : username);
        }
        if (userRoleLabel != null && role != null && !role.isBlank()) {
            userRoleLabel.setText(role);
        }
    }

    @FXML
    private void initialize() {
        // Avatar/logo safe
        if (avatarImageView != null) {
            try {
                URL url = getClass().getResource("/images/logo.png");
                if (url != null) {
                    avatarImageView.setImage(new Image(url.toExternalForm(), true));
                }
            } catch (Exception ignored) {
            }
        }

        // Valeurs par défaut si on ne passe pas par setUser
        if (welcomeLabel != null && (welcomeLabel.getText() == null || welcomeLabel.getText().isBlank())) {
            welcomeLabel.setText("admin");
        }
    }

    // Sidebar actions (pour l'instant: navigation stable)

    @FXML
    private void goToHome() {
        // Déjà sur Home
    }

    @FXML
    private void viewEvenements() {
        // Exigence: ouvrir le FRONT
        openScene("/FrontEvenementsView.fxml", "BoostUp - Événements");
    }

    // Placeholders (ne rien casser si cliqué)
    @FXML private void viewStartups() {}
    @FXML private void viewCandidatures() {}
    @FXML private void viewInvestments() {}
    @FXML private void viewMyStartup() {}
    @FXML private void goToProfile() {}

    @FXML
    private void handleLogout() {
        openScene("/login.fxml", "BoostUp - Connexion");
    }

    private void openScene(String fxmlPath, String title) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            if (fxml == null) {
                return;
            }

            Parent root = FXMLLoader.load(fxml);

            Stage stage;
            if (welcomeLabel != null && welcomeLabel.getScene() != null) {
                stage = (Stage) welcomeLabel.getScene().getWindow();
            } else if (avatarImageView != null && avatarImageView.getScene() != null) {
                stage = (Stage) avatarImageView.getScene().getWindow();
            } else {
                return;
            }

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1000, 650));
            } else {
                scene.setRoot(root);
            }
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

