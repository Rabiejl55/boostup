package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.UtilisateurService.UserService;

import java.io.File;
import java.sql.SQLException;
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
    @FXML private ComboBox<Role_enum> roleComboBox;
    @FXML private Label messageLabel;
    @FXML private Button signupButton;
    @FXML private CheckBox termsCheckbox;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label passwordHintLabel;

    // Theme toggle
    @FXML private ToggleButton themeToggle;

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
        roleComboBox.setItems(FXCollections.observableArrayList(Role_enum.values()));
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

        // Appliquer le thème
        Platform.runLater(() -> {
            if (themeToggle != null && themeToggle.getScene() != null) {
                ThemeHelper.applyTheme(themeToggle.getScene(), themeToggle);
            }
        });
    }

    private void initializeErrorLabels() {
        // Créer les labels d'erreur s'ils n'existent pas dans le FXML
        // Cette méthode sera appelée après le chargement FXML
        if (nomErrorLabel == null) {
            nomErrorLabel = createErrorLabel();
        }
        if (emailErrorLabel == null) {
            emailErrorLabel = createErrorLabel();
        }
        if (mdpErrorLabel == null) {
            mdpErrorLabel = createErrorLabel();
        }
        if (mdpConfirmErrorLabel == null) {
            mdpConfirmErrorLabel = createErrorLabel();
        }
        if (phoneErrorLabel == null) {
            phoneErrorLabel = createErrorLabel();
        }
        if (roleErrorLabel == null) {
            roleErrorLabel = createErrorLabel();
        }
        if (termsErrorLabel == null) {
            termsErrorLabel = createErrorLabel();
        }
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
        // Validation nom
        nomField.textProperty().addListener((obs, old, newVal) -> validateNom());

        // Validation email
        emailField.textProperty().addListener((obs, old, newVal) -> validateEmail());

        // Validation mot de passe
        mdpField.textProperty().addListener((obs, old, newVal) -> {
            validatePassword();
            validatePasswordConfirmation();
        });

        // Validation confirmation mot de passe
        mdpConfirmField.textProperty().addListener((obs, old, newVal) -> validatePasswordConfirmation());

        // Validation téléphone
        phoneField.textProperty().addListener((obs, old, newVal) -> validatePhone());

        // Validation conditions
        termsCheckbox.selectedProperty().addListener((obs, old, newVal) -> validateTerms());
    }

    // ==================== VALIDATEURS INDIVIDUELS ====================

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
            showFieldError(nomField, nomErrorLabel, "Caractères invalides (lettres, espaces, apostrophes et tirets uniquement)");
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
            showFieldError(emailField, emailErrorLabel, "Format d'email invalide (exemple: nom@domaine.com)");
            return false;
        }

        if (email.length() > 100) {
            showFieldError(emailField, emailErrorLabel, "L'email ne peut pas dépasser 100 caractères");
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
            showFieldError(mdpField, mdpErrorLabel, "Le mot de passe doit contenir au moins 6 caractères");
            passwordHintLabel.setText("❌ Trop court (min 6 caractères)");
            passwordHintLabel.getStyleClass().removeAll("valid-text");
            passwordHintLabel.getStyleClass().add("error-text");
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            showFieldError(mdpField, mdpErrorLabel, "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre");
            passwordHintLabel.setText("⚠️ Doit contenir majuscule, minuscule et chiffre");
            passwordHintLabel.getStyleClass().removeAll("valid-text");
            passwordHintLabel.getStyleClass().add("error-text");
            return false;
        }

        clearFieldError(mdpField, mdpErrorLabel);
        passwordHintLabel.setText("✅ Mot de passe sécurisé");
        passwordHintLabel.getStyleClass().removeAll("error-text");
        passwordHintLabel.getStyleClass().add("valid-text");
        return true;
    }

    private boolean validatePasswordConfirmation() {
        String password = mdpField.getText();
        String confirm = mdpConfirmField.getText();

        if (confirm.isEmpty()) {
            showFieldError(mdpConfirmField, mdpConfirmErrorLabel, "Veuillez confirmer votre mot de passe");
            return false;
        }

        if (!confirm.equals(password)) {
            showFieldError(mdpConfirmField, mdpConfirmErrorLabel, "Les mots de passe ne correspondent pas");
            return false;
        }

        if (password.isEmpty()) {
            showFieldError(mdpConfirmField, mdpConfirmErrorLabel, "Veuillez d'abord saisir un mot de passe");
            return false;
        }

        clearFieldError(mdpConfirmField, mdpConfirmErrorLabel);
        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            clearFieldError(phoneField, phoneErrorLabel);
            return true; // Champ optionnel
        }

        if (!PHONE_REGEX.matcher(phone).matches()) {
            showFieldError(phoneField, phoneErrorLabel, "Format de téléphone invalide (ex: +33 6 12 34 56 78)");
            return false;
        }

        if (phone.length() > 20) {
            showFieldError(phoneField, phoneErrorLabel, "Numéro trop long (max 20 caractères)");
            return false;
        }

        clearFieldError(phoneField, phoneErrorLabel);
        return true;
    }

    private boolean validateRole() {
        Role_enum role = roleComboBox.getValue();

        if (role == null) {
            showFieldError(roleComboBox, roleErrorLabel, "Veuillez sélectionner un rôle");
            return false;
        }

        clearFieldError(roleComboBox, roleErrorLabel);
        return true;
    }

    private boolean validateTerms() {
        if (!termsCheckbox.isSelected()) {
            showFieldError(termsCheckbox, termsErrorLabel, "Vous devez accepter les conditions d'utilisation");
            return false;
        }

        clearFieldError(termsCheckbox, termsErrorLabel);
        return true;
    }

    // ==================== VALIDATION GLOBALE ====================

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

    // ==================== GESTION DES ERREURS ====================

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

        // Ajouter tooltip pour plus d'info
        field.setTooltip(new Tooltip(message));
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("error");
        if (!field.getStyleClass().contains("valid")) {
            field.getStyleClass().add("valid");
        }
        field.setTooltip(null);

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    private void chooseAvatar(ActionEvent event) {
        if (avatarPreview == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) avatarPreview.getScene().getWindow();
        selectedPhoto = fileChooser.showOpenDialog(stage);

        if (selectedPhoto != null) {
            try {
                long fileSize = selectedPhoto.length();
                if (fileSize > 5 * 1024 * 1024) {
                    showMessage("L'image est trop volumineuse (max 5MB)", "error");
                    selectedPhoto = null;
                    return;
                }

                String extension = selectedPhoto.getName().substring(selectedPhoto.getName().lastIndexOf(".") + 1).toLowerCase();
                if (!extension.matches("png|jpg|jpeg|gif")) {
                    showMessage("Format d'image non supporté (PNG, JPG, JPEG, GIF)", "error");
                    selectedPhoto = null;
                    return;
                }

                Image image = new Image(selectedPhoto.toURI().toString(), 100, 100, true, true);
                avatarPreview.setImage(image);
                showMessage("Photo sélectionnée avec succès", "success");
            } catch (Exception e) {
                showMessage("Erreur lors du chargement de l'image", "error");
                selectedPhoto = null;
                setDefaultAvatar();
            }
        }
    }

    @FXML
    private void removeAvatar(ActionEvent event) {
        selectedPhoto = null;
        setDefaultAvatar();
        showMessage("Photo retirée", "success");
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        // Valider tous les champs avant soumission
        if (!validateAll()) {
            showMessage("Veuillez corriger les erreurs dans le formulaire", "error");
            return;
        }

        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp = mdpField.getText();
        String phone = phoneField.getText().trim();
        Role_enum role = roleComboBox.getValue();

        // Désactiver les champs pendant l'inscription
        setFormEnabled(false);
        progressIndicator.setVisible(true);
        signupButton.setText("Création en cours...");

        new Thread(() -> {
            try {
                Thread.sleep(500);

                User existingUser = userService.findByEmail(email);
                if (existingUser != null) {
                    Platform.runLater(() -> {
                        showFieldError(emailField, emailErrorLabel, "Cette adresse email est déjà utilisée");
                        showMessage("Cette adresse email est déjà utilisée", "error");
                        resetForm();
                        emailField.requestFocus();
                    });
                    return;
                }

                User user = new User(nom, email, mdp, role);
                user.setFullname(nom);

                if (!phone.isEmpty()) {
                    user.setPhone(phone);
                }

                if (selectedPhoto != null) {
                    user.setAvatar(selectedPhoto.toURI().toString());
                }

                userService.ajouter(user);

                User created = userService.findByEmail(email);
                if (created == null) {
                    throw new SQLException("Échec de la création de l'utilisateur");
                }

                SessionManager.setCurrentUser(created);

                Platform.runLater(() -> {
                    showMessage("✅ Compte créé avec succès ! Redirection...", "success");
                    progressIndicator.setProgress(1.0);
                });

                Thread.sleep(1500);

                Platform.runLater(() -> {
                    goToDashboard(event, created);
                });

            } catch (Exception e) {
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

                if ("success".equals(type)) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            Platform.runLater(() -> {
                                if (messageLabel != null) {
                                    messageLabel.setVisible(false);
                                    messageLabel.setManaged(false);
                                }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
        });
    }

    private void goToDashboard(ActionEvent event, User user) {
        String fxml = user.getRole() == Role_enum.ADMIN ? "/fxml/admin-dashboard.fxml" : "/fxml/homepage.fxml";
        String title = user.getRole() == Role_enum.ADMIN ? "Tableau de bord Admin" : "Accueil";

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, fxml, title);
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
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

        clearFieldError(nomField, nomErrorLabel);
        clearFieldError(emailField, emailErrorLabel);
        clearFieldError(mdpField, mdpErrorLabel);
        clearFieldError(mdpConfirmField, mdpConfirmErrorLabel);
        clearFieldError(phoneField, phoneErrorLabel);
        clearFieldError(roleComboBox, roleErrorLabel);
        clearFieldError(termsCheckbox, termsErrorLabel);

        passwordHintLabel.setText("");
        showMessage("Formulaire réinitialisé", "info");
        nomField.requestFocus();
    }

    private void setDefaultAvatar() {
        if (avatarPreview != null) {
            Image defaultAvatar = SessionManager.createDefaultAvatarImage("GU");
            avatarPreview.setImage(defaultAvatar);
        }
    }

    @FXML
    private void showTerms(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conditions d'utilisation");
        alert.setHeaderText("Conditions Générales d'Utilisation");
        alert.setContentText("En utilisant BoostUp, vous acceptez :\n\n" +
                "1. D'utiliser la plateforme de manière responsable\n" +
                "2. De fournir des informations exactes et à jour\n" +
                "3. De respecter la confidentialité des autres utilisateurs\n" +
                "4. De ne pas utiliser la plateforme à des fins illégales\n" +
                "5. D'accepter nos politiques de confidentialité\n\n" +
                "BoostUp se réserve le droit de suspendre tout compte ne respectant pas ces conditions.");
        alert.getDialogPane().setPrefSize(400, 300);
        alert.showAndWait();
    }

    @FXML
    private void showPrivacyPolicy(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Politique de confidentialité");
        alert.setHeaderText("Politique de Confidentialité");
        alert.setContentText("Nous nous engageons à protéger vos données personnelles :\n\n" +
                "• Vos informations sont stockées de manière sécurisée\n" +
                "• Nous ne partageons pas vos données avec des tiers sans consentement\n" +
                "• Vous pouvez à tout moment demander la suppression de vos données\n" +
                "• Nous utilisons des mesures de sécurité avancées pour protéger vos informations\n" +
                "• Pour toute question, contactez-nous à privacy@boostup.com");
        alert.getDialogPane().setPrefSize(400, 300);
        alert.showAndWait();
    }

    @FXML
    private void showPasswordHelp(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conseils pour mot de passe sécurisé");
        alert.setHeaderText("Comment créer un mot de passe sécurisé ?");
        alert.setContentText("Pour votre sécurité, nous recommandons :\n\n" +
                "✓ Minimum 6 caractères\n" +
                "✓ Combinez lettres majuscules et minuscules\n" +
                "✓ Ajoutez des chiffres (0-9)\n" +
                "✓ Évitez les mots courants (password123)\n" +
                "✓ N'utilisez pas d'informations personnelles\n" +
                "✓ Changez votre mot de passe régulièrement\n\n" +
                "Exemple de mot de passe fort: BoostUp2024!");
        alert.showAndWait();
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }
}