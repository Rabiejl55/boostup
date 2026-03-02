package controllers;

import entities.GUtilisateurs.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.UtilisateurService.ActivityLogService;
import services.UtilisateurService.PdfExportService;
import services.UtilisateurService.UserService;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label statsUsersLabel;
    @FXML private Label statsEventsLabel;
    @FXML private Label statsStartupsLabel;
    @FXML private ImageView avatarImageView;
    @FXML private ToggleButton themeToggle;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            // Mettre à jour le welcome label
            String displayName = user.getFullname() != null && !user.getFullname().isEmpty()
                    ? user.getFullname() : user.getNom();
            welcomeLabel.setText(displayName);

            if (userInfoLabel != null) {
                userInfoLabel.setText(user.getRole() != null ? user.getRole().name() : "Admin");
            }

            // Charger avatar
            loadAvatar();

            // Charger statistiques
            loadStatistics();
        }

        // Initialiser le thème
        Platform.runLater(() -> {
            if (themeToggle != null && themeToggle.getScene() != null) {
                ThemeHelper.applyTheme(themeToggle.getScene(), themeToggle);
            }
        });
    }

    private void loadAvatar() {
        if (avatarImageView == null) return;

        User user = SessionManager.getCurrentUser();
        String avatarUrl = user != null ? user.getAvatar() : null;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                // Clean URL
                avatarUrl = cleanAvatarUrl(avatarUrl);

                if (!isValidImageUrl(avatarUrl)) {
                    setDefaultAvatar();
                    return;
                }

                // Load image with proper dimensions (80x80)
                Image image = new Image(avatarUrl, 80, 80, true, true, true);

                if (!image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setPreserveRatio(true);
                    avatarImageView.setSmooth(true);
                    avatarImageView.setFitWidth(80);
                    avatarImageView.setFitHeight(80);

                    // Create circular clip - radius 40 for 80px image
                    Circle clip = new Circle(40, 40, 40);
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
            User user = SessionManager.getCurrentUser();

            // Create gradient color based on user ID or name
            Color color1, color2;
            if (user != null) {
                int hash = user.getNom().hashCode();
                double hue = Math.abs(hash % 360);
                color1 = Color.hsb(hue, 0.7, 0.9);
                color2 = Color.hsb((hue + 30) % 360, 0.8, 0.8);
            } else {
                color1 = Color.web("#0d6efd");
                color2 = Color.web("#6f42c1");
            }

            // Create gradient image
            WritableImage image = new WritableImage(80, 80);
            PixelWriter writer = image.getPixelWriter();

            for (int y = 0; y < 80; y++) {
                for (int x = 0; x < 80; x++) {
                    double ratio = (double)(x + y) / (160.0);
                    Color mixed = color1.interpolate(color2, ratio);
                    writer.setColor(x, y, mixed);
                }
            }

            avatarImageView.setImage(image);
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setFitWidth(80);
            avatarImageView.setFitHeight(80);

            Circle clip = new Circle(40, 40, 40);
            avatarImageView.setClip(clip);
            avatarImageView.setVisible(true);
        }
    }

    private void loadStatistics() {
        try {
            int userCount = userService.read().size();
            statsUsersLabel.setText(userCount + " utilisateurs");
            statsEventsLabel.setText("42 événements");
            statsStartupsLabel.setText("18 startups");
        } catch (Exception e) {
            e.printStackTrace();
            statsUsersLabel.setText("Erreur");
            statsEventsLabel.setText("Erreur");
            statsStartupsLabel.setText("Erreur");
        }
    }

    // Navigation methods
    @FXML private void goToDashboard(ActionEvent event) {}
    @FXML private void manageUsers(ActionEvent event) { openUsersManagement(event); }
    @FXML private void manageEvents(ActionEvent event) { System.out.println("Gérer les événements"); }
    @FXML private void manageFinancements(ActionEvent event) { System.out.println("Gérer les financements"); }
    @FXML private void viewStats(ActionEvent event) { System.out.println("Voir statistiques"); }
    @FXML private void moderateContent(ActionEvent event) { System.out.println("Modérer contenus"); }
    @FXML private void systemSettings(ActionEvent event) { System.out.println("Paramètres système"); }

    @FXML
    private void handleLogout(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            ActivityLogService.log(user.getId(), user.getEmail(),
                    ActivityLogService.ACTION_LOGOUT, "Déconnexion depuis le dashboard admin");
        }
        SessionManager.logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Parent root = stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
        });
        fadeOut.play();
    }

    // ═══════════════════════════════════════════════════════
    // EXPORT PDF
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleExportUsersPdf(ActionEvent event) {
        try {
            List<User> users = userService.read();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter la liste des utilisateurs en PDF");
            fileChooser.setInitialFileName("boostup_utilisateurs.pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                boolean success = PdfExportService.exportUsersList(users, file.getAbsolutePath());

                User currentUser = SessionManager.getCurrentUser();
                if (currentUser != null) {
                    ActivityLogService.log(currentUser.getId(), currentUser.getEmail(),
                            ActivityLogService.ACTION_EXPORT_PDF,
                            "Export PDF utilisateurs : " + users.size() + " enregistrements");
                }

                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle(success ? "Export réussi" : "Erreur d'export");
                alert.setHeaderText(null);
                alert.setContentText(success
                        ? "Le fichier PDF a été exporté avec succès !\n" + file.getAbsolutePath()
                        : "Erreur lors de l'export du fichier PDF.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportStatsPdf(ActionEvent event) {
        try {
            List<User> users = userService.read();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter le rapport de statistiques en PDF");
            fileChooser.setInitialFileName("boostup_statistiques.pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                boolean success = PdfExportService.exportStatsReport(users, file.getAbsolutePath());

                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle(success ? "Export réussi" : "Erreur d'export");
                alert.setHeaderText(null);
                alert.setContentText(success
                        ? "Le rapport PDF a été exporté avec succès !\n" + file.getAbsolutePath()
                        : "Erreur lors de l'export du rapport PDF.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    // JOURNAL D'ACTIVITÉ
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleViewActivityLog(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/activity-log.fxml", "Journal d'activité");
    }

    @FXML
    private void openUsersManagement(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/users-management.fxml", "Gestion Utilisateurs");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/profile.fxml", "Mon Profil");
    }

    @FXML
    private void viewStartups(ActionEvent event) {
        System.out.println("Voir startups - À implémenter");
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }
}

