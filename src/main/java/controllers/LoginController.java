package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Dans le FXML: fx:id="messageLabel"
    @FXML private Label messageLabel;

    // Dans le FXML: fx:id="logoImageView" (optionnel)
    @FXML private ImageView logoImageView;

    @FXML
    private void initialize() {
        // Charge le logo si présent dans resources/images/logo.png (safe: si absent, pas d'erreur)
        if (logoImageView != null) {
            try {
                URL url = getClass().getResource("/images/logo.png");
                if (url != null) {
                    logoImageView.setImage(new Image(url.toExternalForm(), true));
                }
            } catch (Exception ignored) {
                // Ne pas bloquer l'écran de login si l'image manque
            }
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField == null || emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField == null || passwordField.getText() == null ? "" : passwordField.getText();

        // Admin (back-office)
        if ("admin".equals(email) && "admin".equals(password)) {
            openScene("/views/AdminDashboardView.fxml", "BoostUp Admin - Dashboard");
            return;
        }

        // User (front-office) -> HomePage
        if ("admin".equals(email) && "user".equals(password)) {
            openScene("/HomePage.fxml", "BoostUp - Accueil");
            return;
        }

        showMessage("Identifiants incorrects. Réessaie.", true);
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (emailField != null) emailField.requestFocus();
    }

    @FXML
    private void goToForgotPassword() {
        // ✅ Ouvrir l'interface "Mot de passe oublié"
        openScene("/forgot-password.fxml", "Mot de passe oublié - BoostUp");
    }

    @FXML
    private void goToSignup() {
        // ✅ Ouvrir l'interface d'inscription
        openScene("/signup.fxml", "Inscription - BoostUp");
    }

    private void openScene(String fxmlPath, String title) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            if (fxml == null) {
                showMessage("FXML introuvable: " + fxmlPath, true);
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
            showMessage("Impossible d'ouvrir l'écran: " + e.getMessage(), true);
        }
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError
                    ? "-fx-text-fill: #dc3545; -fx-font-weight: 600;"
                    : "-fx-text-fill: #0d6efd; -fx-font-weight: 600;");
        }
    }
}
