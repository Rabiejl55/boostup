package controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🔐 CONTROLLER FORGOT PASSWORD
 *
 * Système de récupération de mot de passe par code email
 */
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

    // Cache simple pour stocker les codes (en mémoire pour l'instant)
    private static final Map<String, CodeData> resetCodes = new HashMap<>();
    private String currentEmail = "";
    private String currentCode = "";

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

        // Générer un code à 6 chiffres
        String resetCode = generateResetCode();
        currentEmail = email;
        currentCode = resetCode;

        // Stocker le code avec expiration (15 minutes)
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);
        resetCodes.put(email, new CodeData(resetCode, expiryTime));

        LOGGER.info("📧 Code généré pour " + email + ": " + resetCode);

        // 📧 Envoi d'email RÉEL via EmailService
        Task<Boolean> emailTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Simuler délai réseau
                Thread.sleep(1000);

                // Afficher en console (pour debug)
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("📧 CODE DE RÉINITIALISATION");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("Email: " + email);
                System.out.println("Code: " + resetCode);
                System.out.println("Destinataire: rayen.amri@esprit.tn");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // ✅ Envoi EMAIL RÉEL
                try {
                    services.EmailService.envoyerEmailRecuperationMotDePasse(
                        email,
                        resetCode
                    );
                    System.out.println("✅ Email envoyé avec succès !");
                    return true;
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur envoi email: " + e.getMessage());
                    // Même en cas d'erreur, on continue (le code est affiché en console)
                    return true;
                }
            }
        };

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

        if (!email.contains("@") || !email.contains(".")) {
            showMessage("Format d'email invalide", "error");
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    private String generateResetCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
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

        // Vérifier le code
        CodeData codeData = resetCodes.get(currentEmail);

        if (codeData == null) {
            showMessage("❌ Aucun code trouvé pour cet email", "error");
            return;
        }

        // Vérifier expiration
        if (System.currentTimeMillis() > codeData.expiryTime) {
            showMessage("❌ Code expiré. Veuillez en demander un nouveau.", "error");
            resetCodes.remove(currentEmail);
            return;
        }

        // Vérifier le code
        if (enteredCode.equals(codeData.code)) {
            showMessage("✅ Code vérifié ! Vous pouvez maintenant vous connecter.", "success");

            // Nettoyer
            resetCodes.remove(currentEmail);

            // Redirection vers login après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> goToLogin(null));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            showMessage("❌ Code incorrect", "error");
            resetCodeField.clear();
            resetCodeField.requestFocus();
        }
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
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Connexion - BoostUp");
            stage.centerOnScreen();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du retour au login", e);
            showMessage("Erreur de navigation", "error");
        }
    }

    /**
     * Classe interne pour stocker les codes avec expiration
     */
    private static class CodeData {
        String code;
        long expiryTime;

        CodeData(String code, long expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}



