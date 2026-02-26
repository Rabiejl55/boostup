package controllers;

import entities.GUtilisateurs.User;
import services.UtilisateurService.UserService;
import services.EmailService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.regex.Pattern;

public class SignupController {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^[+]?[0-9\\s.-]{8,20}$");
    private static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ\\s'-]{2,50}$");

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField mdpField;
    @FXML private PasswordField mdpConfirmField;
    @FXML private TextField phoneField;
    @FXML private ImageView avatarPreview;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label messageLabel;
    @FXML private Button signupButton;
    @FXML private CheckBox termsCheckbox;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label passwordHintLabel;

    // Labels d'erreur sous chaque champ
    @FXML private Label nomErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label mdpErrorLabel;
    @FXML private Label mdpConfirmErrorLabel;
    @FXML private Label phoneErrorLabel;
    @FXML private Label roleErrorLabel;
    @FXML private Label termsErrorLabel;

    private File selectedPhoto;
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Initialiser la ComboBox avec les rôles
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN", "INVESTISSEUR", "STARTUP"));
        roleComboBox.getSelectionModel().selectFirst();
        roleComboBox.valueProperty().addListener((obs, old, newVal) -> validateRole());

        // Masquer le progress indicator au démarrage
        progressIndicator.setVisible(false);

        // Initialiser tous les labels d'erreur
        initializeErrorLabels();

        // Ajouter les validateurs en temps réel
        setupRealTimeValidation();

        // Avatar par défaut
        setDefaultAvatar();
    }

    private void initializeErrorLabels() {
        if (nomErrorLabel == null) nomErrorLabel = createErrorLabel();
        if (emailErrorLabel == null) emailErrorLabel = createErrorLabel();
        if (mdpErrorLabel == null) mdpErrorLabel = createErrorLabel();
        if (mdpConfirmErrorLabel == null) mdpConfirmErrorLabel = createErrorLabel();
        if (phoneErrorLabel == null) phoneErrorLabel = createErrorLabel();
        if (roleErrorLabel == null) roleErrorLabel = createErrorLabel();
        if (termsErrorLabel == null) termsErrorLabel = createErrorLabel();
    }

    private Label createErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-label");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void setupRealTimeValidation() {
        nomField.textProperty().addListener((obs, old, newVal) -> validateNom());
        emailField.textProperty().addListener((obs, old, newVal) -> validateEmail());
        mdpField.textProperty().addListener((obs, old, newVal) -> {
            validatePassword();
            validatePasswordConfirmation();
        });
        mdpConfirmField.textProperty().addListener((obs, old, newVal) -> validatePasswordConfirmation());
        phoneField.textProperty().addListener((obs, old, newVal) -> validatePhone());
        termsCheckbox.selectedProperty().addListener((obs, old, newVal) -> validateTerms());
    }

    private boolean validateNom() {
        String nom = nomField.getText().trim();

        if (nom.isEmpty()) {
            showFieldError(nomField, nomErrorLabel, "Le nom complet est requis");
            return false;
        }

        if (nom.length() < 2) {
            showFieldError(nomField, nomErrorLabel, "Le nom doit contenir au moins 2 caractères");
            return false;
        }

        if (nom.length() > 50) {
            showFieldError(nomField, nomErrorLabel, "Le nom ne peut pas dépasser 50 caractères");
            return false;
        }

        if (!NAME_REGEX.matcher(nom).matches()) {
            showFieldError(nomField, nomErrorLabel, "Caractères invalides");
            return false;
        }

        clearFieldError(nomField, nomErrorLabel);
        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showFieldError(emailField, emailErrorLabel, "L'adresse email est requise");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError(emailField, emailErrorLabel, "Format d'email invalide");
            return false;
        }

        clearFieldError(emailField, emailErrorLabel);
        return true;
    }

    private boolean validatePassword() {
        String password = mdpField.getText();

        if (password.isEmpty()) {
            showFieldError(mdpField, mdpErrorLabel, "Le mot de passe est requis");
            passwordHintLabel.setText("");
            return false;
        }

        if (password.length() < 6) {
            showFieldError(mdpField, mdpErrorLabel, "Minimum 6 caractères");
            passwordHintLabel.setText("❌ Trop court");
            passwordHintLabel.setStyle("-fx-text-fill: #dc3545;");
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            showFieldError(mdpField, mdpErrorLabel, "Doit contenir majuscule, minuscule et chiffre");
            passwordHintLabel.setText("⚠️ Doit contenir majuscule, minuscule et chiffre");
            passwordHintLabel.setStyle("-fx-text-fill: #ffc107;");
            return false;
        }

        clearFieldError(mdpField, mdpErrorLabel);
        passwordHintLabel.setText("✅ Mot de passe sécurisé");
        passwordHintLabel.setStyle("-fx-text-fill: #198754;");
        return true;
    }

    private boolean validatePasswordConfirmation() {
        String password = mdpField.getText();
        String confirm = mdpConfirmField.getText();

        if (confirm.isEmpty()) {
            showFieldError(mdpConfirmField, mdpConfirmErrorLabel, "Confirmez votre mot de passe");
            return false;
        }

        if (!confirm.equals(password)) {
            showFieldError(mdpConfirmField, mdpConfirmErrorLabel, "Les mots de passe ne correspondent pas");
            return false;
        }

        clearFieldError(mdpConfirmField, mdpConfirmErrorLabel);
        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            clearFieldError(phoneField, phoneErrorLabel);
            return true;
        }

        if (!PHONE_REGEX.matcher(phone).matches()) {
            showFieldError(phoneField, phoneErrorLabel, "Format invalide");
            return false;
        }

        clearFieldError(phoneField, phoneErrorLabel);
        return true;
    }

    private boolean validateRole() {
        if (roleComboBox.getValue() == null) {
            showFieldError(roleComboBox, roleErrorLabel, "Sélectionnez un rôle");
            return false;
        }

        clearFieldError(roleComboBox, roleErrorLabel);
        return true;
    }

    private boolean validateTerms() {
        if (!termsCheckbox.isSelected()) {
            showFieldError(termsCheckbox, termsErrorLabel, "Vous devez accepter les conditions");
            return false;
        }

        clearFieldError(termsCheckbox, termsErrorLabel);
        return true;
    }

    private boolean validateAll() {
        boolean isValid = true;

        isValid &= validateNom();
        isValid &= validateEmail();
        isValid &= validatePassword();
        isValid &= validatePasswordConfirmation();
        isValid &= validatePhone();
        isValid &= validateRole();
        isValid &= validateTerms();

        return isValid;
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        field.getStyleClass().removeAll("valid", "error");
        if (!field.getStyleClass().contains("error")) {
            field.getStyleClass().add("error");
        }

        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("error");
        if (!field.getStyleClass().contains("valid")) {
            field.getStyleClass().add("valid");
        }

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    private void chooseAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) avatarPreview.getScene().getWindow();
        selectedPhoto = fileChooser.showOpenDialog(stage);

        if (selectedPhoto != null) {
            try {
                Image image = new Image(selectedPhoto.toURI().toString(), 100, 100, true, true);
                avatarPreview.setImage(image);
                showMessage("Photo sélectionnée", "success");
            } catch (Exception e) {
                showMessage("Erreur lors du chargement", "error");
                selectedPhoto = null;
            }
        }
    }

    @FXML
    private void removeAvatar(ActionEvent event) {
        selectedPhoto = null;
        setDefaultAvatar();
        showMessage("Photo retirée", "info");
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        System.out.println("🚀 Début handleSignup");

        if (!validateAll()) {
            showMessage("Veuillez corriger les erreurs", "error");
            System.out.println("❌ Validation échouée");
            return;
        }

        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp = mdpField.getText();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue();

        System.out.println("📝 Données collectées:");
        System.out.println("   Nom: " + nom);
        System.out.println("   Email: " + email);
        System.out.println("   Phone: " + phone);
        System.out.println("   Role: " + role);

        // Désactiver les champs pendant l'inscription
        setFormEnabled(false);
        progressIndicator.setVisible(true);
        signupButton.setText("Création en cours...");

        new Thread(() -> {
            try {
                System.out.println("⏳ Thread d'inscription démarré");
                Thread.sleep(500);

                // Vérifier si l'email existe déjà
                System.out.println("🔍 Vérification email existant...");
                User existingUser = userService.findByEmail(email);
                if (existingUser != null) {
                    System.out.println("❌ Email déjà utilisé");
                    Platform.runLater(() -> {
                        showFieldError(emailField, emailErrorLabel, "Cette adresse email est déjà utilisée");
                        showMessage("Cette adresse email est déjà utilisée", "error");
                        resetForm();
                        emailField.requestFocus();
                    });
                    return;
                }

                System.out.println("✅ Email disponible, création de l'utilisateur...");

                // Créer le nouvel utilisateur
                User user = new User();
                user.setNom(nom);
                user.setEmail(email);
                user.setMdp(mdp);
                user.setRole(role != null ? role : "USER");
                user.setFullname(nomField.getText().trim());

                if (!phone.isEmpty()) {
                    user.setPhone(phone);
                }

                if (selectedPhoto != null) {
                    user.setAvatar(selectedPhoto.toURI().toString());
                }

                user.setActive(true);

                // Ajouter dans la base de données
                System.out.println("💾 Ajout dans la base de données...");
                userService.ajouter(user);
                System.out.println("✅ Utilisateur ajouté");

                // Récupérer l'utilisateur créé depuis la base
                System.out.println("🔄 Récupération de l'utilisateur créé...");
                User created = userService.findByEmail(email);
                if (created == null) {
                    System.err.println("❌ Échec récupération utilisateur créé");
                    throw new Exception("Échec de la création de l'utilisateur");
                }
                System.out.println("✅ Utilisateur récupéré, ID: " + created.getId());

                // Envoyer l'email de bienvenue
                System.out.println("📧 Envoi email de bienvenue...");
                EmailService.sendWelcomeEmail(email, nom);

                // Mettre à jour la session
                System.out.println("🔐 Mise à jour de la session...");
                SessionManager.setCurrentUser(created);
                System.out.println("✅ Session créée pour: " + created.getNom());

                Platform.runLater(() -> {
                    showMessage("✅ Compte créé avec succès ! Redirection...", "success");
                    progressIndicator.setProgress(1.0);
                });

                Thread.sleep(1500);

                System.out.println("🚀 Lancement de la redirection...");
                Platform.runLater(() -> {
                    // Rediriger vers le dashboard approprié
                    goToDashboard(event, created);
                });

            } catch (Exception e) {
                System.err.println("❌ ERREUR dans handleSignup: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showMessage("Erreur lors de l'inscription: " + e.getMessage(), "error");
                    e.printStackTrace();
                    resetForm();
                });
            }
        }).start();
    }

    private void setFormEnabled(boolean enabled) {
        nomField.setDisable(!enabled);
        emailField.setDisable(!enabled);
        mdpField.setDisable(!enabled);
        mdpConfirmField.setDisable(!enabled);
        phoneField.setDisable(!enabled);
        roleComboBox.setDisable(!enabled);
        termsCheckbox.setDisable(!enabled);
        signupButton.setDisable(!enabled);
    }

    private void resetForm() {
        Platform.runLater(() -> {
            setFormEnabled(true);
            progressIndicator.setVisible(false);
            progressIndicator.setProgress(0);
            signupButton.setText("S'inscrire");
        });
    }

    private void showMessage(String text, String type) {
        Platform.runLater(() -> {
            if (messageLabel != null) {
                messageLabel.setText(text);
                messageLabel.getStyleClass().removeAll("success", "error", "info");
                messageLabel.getStyleClass().add(type);
                messageLabel.setVisible(true);
                messageLabel.setManaged(true);
            }
        });
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        Stage stage = (Stage) signupButton.getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
    }

    private void goToDashboard(ActionEvent event, User user) {
        System.out.println("🔄 goToDashboard appelé avec user: " + user.getNom());
        System.out.println("   Role: " + user.getRole());

        String fxml = "ADMIN".equalsIgnoreCase(user.getRole())
                ? "/admin-dashboard.fxml"
                : "/HomePage.fxml";
        String title = "ADMIN".equalsIgnoreCase(user.getRole())
                ? "Tableau de bord Admin"
                : "Accueil";

        System.out.println("   Redirection vers: " + fxml);
        System.out.println("   Titre: " + title);

        try {
            Stage stage = (Stage) signupButton.getScene().getWindow();
            NavigationHelper.navigateTo(stage, fxml, title);
            System.out.println("✅ Redirection effectuée avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la redirection: " + e.getMessage());
            e.printStackTrace();
            showMessage("Erreur lors de la redirection: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void clearForm(ActionEvent event) {
        nomField.clear();
        emailField.clear();
        mdpField.clear();
        mdpConfirmField.clear();
        phoneField.clear();
        roleComboBox.getSelectionModel().selectFirst();
        termsCheckbox.setSelected(false);
        selectedPhoto = null;
        setDefaultAvatar();
        passwordHintLabel.setText("");
        showMessage("Formulaire réinitialisé", "info");
    }

    private void setDefaultAvatar() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
            avatarPreview.setImage(defaultImage);
        } catch (Exception e) {
            // Ignore si pas de logo
        }
    }

    @FXML
    private void showTerms(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conditions d'utilisation");
        alert.setHeaderText("Conditions Générales");
        alert.setContentText("En utilisant BoostUp, vous acceptez :\n\n" +
                "1. Utilisation responsable\n" +
                "2. Informations exactes\n" +
                "3. Respect de la confidentialité\n" +
                "4. Pas d'usage illégal");
        alert.showAndWait();
    }

    @FXML
    private void showPrivacyPolicy(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Politique de confidentialité");
        alert.setHeaderText("Protection des données");
        alert.setContentText("Vos données sont protégées et sécurisées.\n\n" +
                "Nous ne partageons pas vos informations.");
        alert.showAndWait();
    }

    @FXML
    private void showPasswordHelp(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conseils mot de passe");
        alert.setHeaderText("Créer un mot de passe sécurisé");
        alert.setContentText("✓ Minimum 6 caractères\n" +
                "✓ Majuscules et minuscules\n" +
                "✓ Au moins un chiffre\n\n" +
                "Exemple : BoostUp2024!");
        alert.showAndWait();
    }
}

