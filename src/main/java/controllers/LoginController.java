package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField == null || emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField == null || passwordField.getText() == null ? "" : passwordField.getText();

        // Admin (back-office)
        if ("admin".equals(email) && "admin".equals(password)) {
            openScene("/EvenementView.fxml", "Gestion des Événements - BoostUp");
            return;
        }

        // User (front-office)
        if ("admin".equals(email) && "user".equals(password)) {
            openScene("/FrontEvenementsView.fxml", "BoostUp - Événements");
            return;
        }

        // Identifiants incorrects
        showError("Identifiants incorrects. Réessaie.");
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (emailField != null) emailField.requestFocus();
    }

    private void openScene(String fxmlPath, String title) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            if (fxml == null) {
                showError("FXML introuvable: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) emailField.getScene().getWindow();

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 900, 600));
            } else {
                scene.setRoot(root);
            }

            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir l'écran: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
        }
    }
}
