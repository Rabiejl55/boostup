package controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.UtilisateurService.EmailService;
import services.UtilisateurService.PasswordResetCache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ForgotPasswordController {

    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordController.class.getName());

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Button sendButton;
    @FXML private Button backButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox codeVerificationBox;
    @FXML private TextField resetCodeField;
    @FXML private Button verifyCodeButton;

    private String currentToken = "";

    @FXML
    public void initialize() {
        messageLabel.setWrapText(true);
        codeVerificationBox.setVisible(false);
        codeVerificationBox.setManaged(false);

        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }

        LOGGER.info("✅ ForgotPasswordController initialisé");
    }

    @FXML
    private void handleSendReset(ActionEvent event) {
        String email = emailField.getText().trim();

        if (!validateEmail(email)) {
            return;
        }

        setUiEnabled(false);
        showProgress(true);

        String resetCode = generateResetCode();
        currentToken = PasswordResetCache.generateToken(email, resetCode);

        LOGGER.info("📧 Code généré pour " + email + ": " + resetCode);

        Task<Boolean> emailTask = createEmailTask(email, resetCode);
        emailTask.setOnSucceeded(e -> handleEmailSuccess(emailTask.getValue(), email));
        emailTask.setOnFailed(e -> handleEmailFailure(emailTask.getException()));

        new Thread(emailTask).start();
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showMessage("Veuillez entrer votre adresse email", "error");
            emailField.requestFocus();
            return false;
        }

        if (!EmailService.isValidEmail(email)) {
            showMessage("Format d'email invalide", "error");
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    private String generateResetCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private Task<Boolean> createEmailTask(String email, String resetCode) {
        return new Task<>() {
            @Override
            protected Boolean call() {
                return EmailService.sendVerificationCode(email, resetCode);
            }
        };
    }

    private void handleEmailSuccess(boolean success, String email) {
        showProgress(false);

        if (success) {
            showMessage("✅ Code envoyé à " + maskEmail(email), "success");
            showCodeVerification();
        } else {
            setUiEnabled(true);
            showMessage("❌ Échec d'envoi. Vérifiez votre connexion.", "error");
        }
    }

    private void handleEmailFailure(Throwable exception) {
        showProgress(false);
        setUiEnabled(true);
        showMessage("❌ Erreur: " + exception.getMessage(), "error");
        LOGGER.log(Level.SEVERE, "Erreur envoi email", exception);
    }

    @FXML
    private void handleVerifyCode(ActionEvent event) {
        String enteredCode = resetCodeField.getText().trim();

        if (enteredCode.isEmpty()) {
            showMessage("Veuillez entrer le code", "error");
            return;
        }

        boolean valid = PasswordResetCache.verifyCode(currentToken, enteredCode);

        if (valid) {
            showMessage("✅ Code vérifié ! Redirection vers la connexion...", "success");
            redirectToLogin();
        } else {
            showMessage("❌ Code incorrect ou expiré", "error");
            resetCodeField.clear();
            resetCodeField.requestFocus();
        }
    }

    private void redirectToLogin() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> goToLogin(null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interruption pendant la redirection", e);
            }
        }).start();
    }

    private void showCodeVerification() {
        codeVerificationBox.setVisible(true);
        codeVerificationBox.setManaged(true);
        resetCodeField.clear();
        resetCodeField.requestFocus();
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.substring(0, 2) + "****" + email.substring(atIndex - 1);
    }

    private void setUiEnabled(boolean enabled) {
        emailField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        backButton.setDisable(!enabled);

        if (codeVerificationBox.isVisible()) {
            resetCodeField.setDisable(!enabled);
            verifyCodeButton.setDisable(!enabled);
        }
    }

    private void showProgress(boolean show) {
        if (progressIndicator != null) {
            progressIndicator.setVisible(show);
        }
        sendButton.setText(show ? "Envoi..." : "Envoyer le code");
    }

    private void showMessage(String text, String type) {
        javafx.application.Platform.runLater(() -> {
            messageLabel.setText(text);
            messageLabel.getStyleClass().removeAll("success", "error", "info");
            messageLabel.getStyleClass().add(type);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        });
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        Stage stage = (Stage) backButton.getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
    }
}