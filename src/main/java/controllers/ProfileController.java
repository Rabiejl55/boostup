package controllers;

import entities.GUtilisateurs.User;
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
import javafx.stage.Stage;
import services.UtilisateurService.ActivityLogService;
import services.UtilisateurService.FaceRecognitionService;
import services.UtilisateurService.UserService;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ProfileController {

    private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());

    @FXML private Label welcomeLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label phoneLabel;
    @FXML private Label fullnameLabel;
    @FXML private Label dateCreationLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleTag;
    @FXML private Label profileEmailPreview;
    @FXML private Label accountDateLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sms2faStatusLabel;
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

        try {
            User fullUser = userService.findById(user.getId());
            if (fullUser == null) {
                showErrorMessage("Utilisateur non trouvé");
                return;
            }
            SessionManager.setCurrentUser(fullUser);
            user = fullUser;
        } catch (SQLException e) {
            LOGGER.severe("Erreur lors du chargement du profil: " + e.getMessage());
            showErrorMessage("Erreur chargement des données");
            return;
        }

        String displayName = (user.getFullname() != null && !user.getFullname().isEmpty())
                ? user.getFullname() : user.getNom();
        String roleName = getRoleDisplayName(user.getRole());

        setText(welcomeLabel, displayName);
        setText(sidebarRoleLabel, "✦ " + roleName);
        setText(profileNameLabel, displayName);
        setText(profileRoleTag, "👤 " + roleName);
        setText(profileEmailPreview, user.getEmail());
        setText(fullnameLabel, defaultIfEmpty(user.getFullname(), "Non renseigné"));
        setText(nomLabel, user.getNom());
        setText(emailLabel, user.getEmail());
        setText(roleLabel, roleName);
        setText(phoneLabel, defaultIfEmpty(user.getPhone(), "Non renseigné"));

        if (sms2faStatusLabel != null) {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                sms2faStatusLabel.setText("Activé — SMS envoyé à chaque connexion");
                sms2faStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px; -fx-font-weight: 600;");
            } else {
                sms2faStatusLabel.setText("Désactivé — Ajoutez un numéro de téléphone");
                sms2faStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 600;");
            }
        }

        if (user.getDateCreation() != null) {
            String dateFormatted = new java.text.SimpleDateFormat("dd/MM/yyyy").format(
                    java.sql.Timestamp.valueOf(user.getDateCreation()));
            setText(dateCreationLabel, "Membre depuis le " + dateFormatted);
            setText(accountDateLabel, dateFormatted);
        } else {
            setText(dateCreationLabel, "Date d'inscription inconnue");
            setText(accountDateLabel, "—");
        }

        loadAvatar(avatarImageView, user, 120);
        loadAvatar(sidebarAvatarView, user, 72);
    }

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

    private String getRoleDisplayName(String role) {
        if (role == null || role.isEmpty()) return "Utilisateur";
        String roleUpper = role.toUpperCase().trim();
        switch (roleUpper) {
            case "ADMIN":
            case "ADMINISTRATEUR":
                return "Administrateur";
            case "INVESTISSEUR":
                return "Investisseur";
            case "STARTUP":
            case "PORTEUR DE PROJET":
                return "Porteur de projet";
            case "USER":
            case "UTILISATEUR":
            case "MEMBRE":
                return "Utilisateur";
            default:
                return role;
        }
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        System.out.println("🏠 goToDashboard appelé depuis ProfileController");
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            System.out.println("   User: " + user.getNom() + " - Role: " + user.getRole());

            String path = "ADMIN".equalsIgnoreCase(user.getRole())
                    ? "/admin-dashboard.fxml"
                    : "/HomePage.fxml";
            String title = "ADMIN".equalsIgnoreCase(user.getRole())
                    ? "Tableau de bord Admin"
                    : "Accueil";

            System.out.println("   Redirection vers: " + path);

            try {
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                NavigationHelper.navigateTo(stage, path, title);
                System.out.println("✅ Navigation réussie");
            } catch (Exception e) {
                System.err.println("❌ Erreur navigation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ Aucun utilisateur en session");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("🚪 handleLogout appelé depuis ProfileController");
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            System.out.println("   Déconnexion de: " + user.getEmail());
        }
        SessionManager.logout();

        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            NavigationHelper.navigateTo(stage, "/login.fxml", "Connexion");
            System.out.println("✅ Redirection vers login réussie");
        } catch (Exception e) {
            System.err.println("❌ Erreur redirection logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEnrollFaceId(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Info message about Face ID
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("📸 Face ID — Reconnaissance Faciale");
        infoAlert.setHeaderText("Comment ça marche ?");
        infoAlert.setContentText(
                "Face ID utilise Face++ Cloud AI pour une reconnaissance sécurisée.\n\n" +
                "✅ Sélectionnez une photo claire de votre visage\n" +
                "✅ L'image sera stockée localement et de manière sécurisée\n" +
                "✅ Vous pourrez ensuite vous connecter avec votre visage\n\n" +
                "Note: Une connexion Internet est recommandée pour de meilleurs résultats."
        );
        infoAlert.showAndWait();

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("📸 Sélectionnez votre photo pour Face ID");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        java.io.File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            boolean success = FaceRecognitionService.enrollFaceFromFile(user.getId(), selectedFile);

            Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle(success ? "✅ Face ID Activé" : "❌ Erreur");
            alert.setHeaderText(null);
            alert.setContentText(success
                    ? "✅ Votre visage a été enregistré avec succès !\n\n" +
                      "Vous pouvez maintenant vous connecter avec Face ID.\n" +
                      "L'image est stockée de manière sécurisée localement."
                    : "❌ Impossible d'enregistrer votre visage.\n\n" +
                      "Conseils :\n" +
                      "• Utilisez une photo claire et bien éclairée\n" +
                      "• Assurez-vous que votre visage est visible\n" +
                      "• Évitez les lunettes de soleil ou masques");
            alert.showAndWait();

            if (success) {
                ActivityLogService.log(user.getId(), user.getEmail(),
                        ActivityLogService.ACTION_FACE_ENROLL,
                        "Visage enregistré pour Face ID");

                // Reload avatar to show enrolled face
                loadAvatar(avatarImageView, user, 120);
            }
        }
    }

    @FXML
    private void handleRemoveFaceId(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        boolean deleted = FaceRecognitionService.deleteFace(user.getId());

        Alert alert = new Alert(deleted ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(deleted ? "✅ Face ID Désactivé" : "⚠️ Information");
        alert.setHeaderText(null);
        alert.setContentText(deleted
                ? "✅ Face ID a été désactivé avec succès.\n\nVous devrez utiliser email/mot de passe pour vous connecter."
                : "⚠️ Aucun visage enregistré à supprimer.");
        alert.showAndWait();

        if (deleted) {
            ActivityLogService.log(user.getId(), user.getEmail(),
                    ActivityLogService.ACTION_FACE_ENROLL,
                    "Face ID désactivé");
        }
    }

    private void showErrorMessage(String message) {
        if (welcomeLabel != null) {
            welcomeLabel.setText(message);
            welcomeLabel.setStyle("-fx-text-fill: red;");
        }
    }
}









