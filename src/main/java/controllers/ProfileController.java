package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.UtilisateurService.ActivityLogService;
import services.UtilisateurService.FaceRecognitionService;
import services.UtilisateurService.UserService;

import java.io.File;
import java.sql.SQLException;

public class ProfileController {

    // ── Labels du contenu principal ──
    @FXML private Label welcomeLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label phoneLabel;
    @FXML private Label fullnameLabel;
    @FXML private Label dateCreationLabel;

    // ── Labels de la hero card ──
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleTag;
    @FXML private Label profileEmailPreview;

    // ── Label du compte ──
    @FXML private Label accountDateLabel;

    // ── Labels sidebar ──
    @FXML private Label sidebarRoleLabel;

    // ── Sécurité ──
    @FXML private Label sms2faStatusLabel;

    // ── Images ──
    @FXML private ImageView avatarImageView;
    @FXML private ImageView sidebarAvatarView;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            if (welcomeLabel != null) welcomeLabel.setText("Erreur : non connecté");
            return;
        }

        // Récupérer les données complètes depuis la base
        try {
            User fullUser = userService.findById(user.getId());
            if (fullUser == null) {
                showErrorMessage("Utilisateur non trouvé");
                return;
            }
            SessionManager.setCurrentUser(fullUser);
            user = fullUser;
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur chargement des données");
            return;
        }

        // ── Remplir toutes les informations en lecture seule ──
        String displayName = (user.getFullname() != null && !user.getFullname().isEmpty())
                ? user.getFullname() : user.getNom();
        String roleName = getRoleDisplayName(user.getRole());

        // Sidebar
        setText(welcomeLabel, displayName);
        setText(sidebarRoleLabel, "✦ " + roleName);

        // Hero card
        setText(profileNameLabel, displayName);
        setText(profileRoleTag, "👤 " + roleName);
        setText(profileEmailPreview, user.getEmail());

        // Informations personnelles
        setText(fullnameLabel, defaultIfEmpty(user.getFullname(), "Non renseigné"));
        setText(nomLabel, user.getNom());
        setText(emailLabel, user.getEmail());
        setText(roleLabel, roleName);
        setText(phoneLabel, defaultIfEmpty(user.getPhone(), "Non renseigné"));

        // 2FA Status
        if (sms2faStatusLabel != null) {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                sms2faStatusLabel.setText("Activé — SMS envoyé à chaque connexion");
                sms2faStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px; -fx-font-weight: 600;");
            } else {
                sms2faStatusLabel.setText("Désactivé — Ajoutez un numéro de téléphone");
                sms2faStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 600;");
            }
        }

        // Dates
        if (user.getDateCreation() != null) {
            String dateFormatted = new java.text.SimpleDateFormat("dd/MM/yyyy").format(user.getDateCreation());
            setText(dateCreationLabel, "Membre depuis le " + dateFormatted);
            setText(accountDateLabel, dateFormatted);
        } else {
            setText(dateCreationLabel, "Date d'inscription inconnue");
            setText(accountDateLabel, "—");
        }

        // Avatars (principal + sidebar)
        loadAvatar(avatarImageView, user, 120);
        loadAvatar(sidebarAvatarView, user, 72);
    }

    // ═══════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private String defaultIfEmpty(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private void loadAvatar(ImageView imageView, User user, double size) {
        if (imageView == null) return;

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);

        boolean loaded = false;

        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                String avatarUrl = cleanAvatarUrl(user.getAvatar());
                Image image = new Image(avatarUrl, size, size, true, true, true);

                if (!image.isError()) {
                    imageView.setImage(image);
                    loaded = true;
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement avatar: " + e.getMessage());
            }
        }

        if (!loaded) {
            setDefaultAvatar(imageView, user, size);
        }

        // Clip circulaire
        double radius = size / 2;
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
        imageView.setVisible(true);
    }

    private void setDefaultAvatar(ImageView imageView, User user, double size) {
        int colorIndex = Math.abs(user.getId() % 8);
        String[] colors = {"#0d6efd", "#198754", "#6f42c1", "#fd7e14",
                "#dc3545", "#20c997", "#6610f2", "#ffc107"};

        int intSize = (int) size;
        WritableImage image = new WritableImage(intSize, intSize);
        PixelWriter writer = image.getPixelWriter();
        Color color = Color.web(colors[colorIndex]);

        for (int y = 0; y < intSize; y++) {
            for (int x = 0; x < intSize; x++) {
                writer.setColor(x, y, color);
            }
        }

        imageView.setImage(image);
    }

    private String cleanAvatarUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("\\")) url = url.replace("\\", "/");
        if ((url.startsWith("C:/") || url.startsWith("D:/") || url.startsWith("/"))
                && !url.startsWith("file://")) {
            url = "file:///" + url;
        }
        if (url.contains(" ")) url = url.replace(" ", "%20");
        if (url.contains("'")) url = url.replace("'", "%27");
        return url;
    }

    private String getRoleDisplayName(Role_enum role) {
        switch (role) {
            case ADMIN: return "Administrateur";
            case INVESTISSEUR: return "Investisseur";
            case STARTUP: return "Porteur de projet";
            default: return role.name();
        }
    }

    // ═══════════════════════════════════════════════════════
    // NAVIGATION (seules actions autorisées)
    // ═══════════════════════════════════════════════════════

    @FXML
    private void goToDashboard(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String path = user.getRole() == Role_enum.ADMIN
                    ? "/fxml/admin-dashboard.fxml"
                    : "/fxml/homepage.fxml";
            String title = user.getRole() == Role_enum.ADMIN
                    ? "Tableau de bord Admin"
                    : "Accueil";

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            NavigationHelper.navigateTo(stage, path, title);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            ActivityLogService.log(user.getId(), user.getEmail(),
                    ActivityLogService.ACTION_LOGOUT, "Déconnexion depuis la page profil");
        }
        SessionManager.logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
    }

    // ═══════════════════════════════════════════════════════
    // FACE ID — Enregistrement du visage
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleEnrollFaceId(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("📸 Sélectionnez votre photo pour Face ID");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            boolean success = FaceRecognitionService.enrollFaceFromFile(user.getId(), selectedFile);

            Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle(success ? "✅ Face ID Activé" : "❌ Erreur");
            alert.setHeaderText(null);
            alert.setContentText(success
                    ? "Votre visage a été enregistré avec succès !\nVous pouvez maintenant vous connecter avec Face ID."
                    : "Impossible d'enregistrer votre visage. Réessayez avec une photo plus nette.");
            alert.showAndWait();

            if (success) {
                ActivityLogService.log(user.getId(), user.getEmail(),
                        ActivityLogService.ACTION_FACE_ENROLL,
                        "Visage enregistré pour Face ID");
            }
        }
    }

    @FXML
    private void handleRemoveFaceId(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        boolean deleted = FaceRecognitionService.deleteFace(user.getId());

        Alert alert = new Alert(deleted ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(deleted ? "Face ID Désactivé" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(deleted
                ? "Face ID a été désactivé. Vous devrez utiliser email/mot de passe pour vous connecter."
                : "Aucun visage enregistré à supprimer.");
        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        if (welcomeLabel != null) {
            welcomeLabel.setText(message);
            welcomeLabel.setStyle("-fx-text-fill: red;");
        }
    }
}