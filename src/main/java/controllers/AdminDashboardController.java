package controllers;

import entities.GUtilisateurs.User;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.util.Duration;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statsUsersLabel;
    @FXML private Label statsEventsLabel;
    @FXML private Label statsStartupsLabel;
    @FXML private ImageView avatarImageView;

    @FXML
    public void initialize() {
        System.out.println("✅ AdminDashboardController initialized");

        // Charger les informations de l'utilisateur
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String displayName = user.getFullname() != null && !user.getFullname().isEmpty()
                    ? user.getFullname()
                    : user.getNom();

            if (welcomeLabel != null) {
                welcomeLabel.setText(displayName);
            }

            // Charger avatar
            loadAvatar();
        } else {
            if (welcomeLabel != null) {
                welcomeLabel.setText("admin");
            }
        }

        // Charger statistiques
        loadStatistics();
    }

    private void loadAvatar() {
        if (avatarImageView == null) return;

        User user = SessionManager.getCurrentUser();
        String avatarUrl = user != null ? user.getAvatar() : null;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                avatarUrl = cleanAvatarUrl(avatarUrl);

                if (!isValidImageUrl(avatarUrl)) {
                    setDefaultAvatar();
                    return;
                }

                Image image = new Image(avatarUrl, 72, 72, true, true, true);

                if (!image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setPreserveRatio(true);
                    avatarImageView.setSmooth(true);
                    avatarImageView.setFitWidth(72);
                    avatarImageView.setFitHeight(72);

                    Circle clip = new Circle(36, 36, 36);
                    avatarImageView.setClip(clip);
                    avatarImageView.setVisible(true);
                } else {
                    setDefaultAvatar();
                }

            } catch (Exception e) {
                System.err.println("Erreur chargement avatar: " + e.getMessage());
                setDefaultAvatar();
            }
        } else {
            setDefaultAvatar();
        }
    }

    private String cleanAvatarUrl(String url) {
        if (url.contains(" ")) url = url.replace(" ", "%20");
        if (url.contains("'")) url = url.replace("'", "%27");
        if (url.startsWith("C:/") || url.startsWith("D:/") || url.startsWith("/")) {
            if (!url.startsWith("file://")) {
                url = "file:///" + url.replace("\\", "/");
            }
        }
        return url;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)$") || url.startsWith("http");
    }

    private void setDefaultAvatar() {
        if (avatarImageView != null) {
            // Create gradient color based on user
            Color color1 = Color.web("#1b2a4a");
            Color color2 = Color.web("#2d1b4e");

            WritableImage image = new WritableImage(72, 72);
            PixelWriter writer = image.getPixelWriter();

            for (int y = 0; y < 72; y++) {
                for (int x = 0; x < 72; x++) {
                    double ratio = (double)(x + y) / (144.0);
                    Color mixed = color1.interpolate(color2, ratio);
                    writer.setColor(x, y, mixed);
                }
            }

            avatarImageView.setImage(image);
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setFitWidth(72);
            avatarImageView.setFitHeight(72);

            Circle clip = new Circle(36, 36, 36);
            avatarImageView.setClip(clip);
            avatarImageView.setVisible(true);
        }
    }

    private void loadStatistics() {
        // Placeholder - vous pouvez connecter à vos services réels
        if (statsUsersLabel != null) statsUsersLabel.setText("0 utilisateurs");
        if (statsEventsLabel != null) statsEventsLabel.setText("0 événements");
        if (statsStartupsLabel != null) statsStartupsLabel.setText("0 startups");
    }

    // ══════════════════════════════════════════
    // NAVIGATION METHODS
    // ══════════════════════════════════════════

    @FXML
    private void goToDashboard(ActionEvent event) {
        // Déjà sur le dashboard
    }

    @FXML
    private void manageEvents(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/EvenementView.fxml", "Gestion des événements");
    }

    @FXML
    private void openUsersManagement(ActionEvent event) {
        System.out.println("Gestion utilisateurs - À implémenter");
    }

    @FXML
    private void manageFinancements(ActionEvent event) {
        System.out.println("Gestion financements - À implémenter");
    }

    @FXML
    private void viewStats(ActionEvent event) {
        System.out.println("Statistiques - À implémenter");
    }

    @FXML
    private void moderateContent(ActionEvent event) {
        System.out.println("Modération - À implémenter");
    }

    @FXML
    private void systemSettings(ActionEvent event) {
        System.out.println("Paramètres système - À implémenter");
    }

    @FXML
    private void viewStartups(ActionEvent event) {
        System.out.println("Voir startups - À implémenter");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        System.out.println("🔄 Navigation vers Profile depuis AdminDashboard");
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/profile.fxml", "Mon Profil");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Déconnexion...");
        SessionManager.logout();

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Parent root = stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            NavigationHelper.navigateTo(stage, "/login.fxml", "Connexion");
        });
        fadeOut.play();
    }
}
