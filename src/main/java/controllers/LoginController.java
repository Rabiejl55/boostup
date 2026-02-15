package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UtilisateurService.UserService;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Email et mot de passe requis", "error");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");

        new Thread(() -> {
            try {
                // 🔍 Étape 1 : Vérifier d'abord si l'utilisateur existe
                User user = userService.findByEmail(email);

                if (user == null) {
                    // Cas 1 : Utilisateur non trouvé
                    showMessage("Email ou mot de passe incorrect", "error");
                } else if (!user.isActive()) {
                    // Cas 2 : Compte bloqué ⛔
                    showMessage("❌ Compte bloqué. Veuillez contacter l'administrateur.", "error");
                } else {
                    // Cas 3 : Tentative de connexion avec mot de passe
                    User connectedUser = userService.seConnecter(email, password);

                    if (connectedUser != null) {
                        // Succès
                        SessionManager.setCurrentUser(connectedUser);
                        showMessage("Connexion réussie ! Redirection...", "success");
                        Thread.sleep(1000);
                        Platform.runLater(() -> navigateBasedOnRole(event, connectedUser));
                    } else {
                        // Cas 4 : Mot de passe incorrect
                        showMessage("Email ou mot de passe incorrect", "error");
                    }
                }
            }
            catch (SQLException | InterruptedException e) {
                showMessage("Erreur : " + e.getMessage(), "error");
                e.printStackTrace();
            } finally {
                Platform.runLater(() -> {
                    loginButton.setText("Se connecter");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private void navigateBasedOnRole(ActionEvent event, User user) {
        String fxmlPath = user.getRole() == Role_enum.ADMIN ? "/fxml/admin-dashboard.fxml" : "/fxml/homepage.fxml";
        String title = user.getRole() == Role_enum.ADMIN ? "Tableau de bord Admin" : "Accueil";

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, fxmlPath, title);
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/signup.fxml", "Inscription");
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/forgot-password.fxml", "Mot de passe oublié");
    }

    private void showMessage(String text, String type) {
        Platform.runLater(() -> {
            messageLabel.setText(text);
            messageLabel.getStyleClass().removeAll("success", "error");
            messageLabel.getStyleClass().add(type);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        });
    }
}