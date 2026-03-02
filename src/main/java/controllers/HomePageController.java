package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HomePageController {

    @FXML private Label welcomeLabel;
    @FXML private Label userRoleLabel;
    @FXML private ImageView avatarImageView;
    @FXML private HBox userInfoBox;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;
    @FXML private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        System.out.println("Initialisation HomePageController...");

        User user = SessionManager.getCurrentUser();
        if (user != null) {
            System.out.println("Utilisateur connecté: " + user.getNom());

            // Mettre à jour les labels avec vérifications null
            String displayName = user.getFullname() != null && !user.getFullname().isEmpty()
                    ? user.getFullname() : user.getNom();

            if (welcomeLabel != null) {
                welcomeLabel.setText(displayName); // Just the name without "Bienvenue, "
            }

            if (userRoleLabel != null) {
                userRoleLabel.setText(getRoleDisplayName(user.getRole()));
            }

            // Charger avatar avec la bonne taille (80x80)
            loadAvatar();
        } else {
            System.err.println("Aucun utilisateur connecté!");
        }

        // Appliquer le thème
        Platform.runLater(() -> {
            if (themeToggle != null && themeToggle.getScene() != null) {
                ThemeHelper.applyTheme(themeToggle.getScene(), themeToggle);
            }
        });
    }

    private void loadAvatar() {
        if (avatarImageView == null) {
            System.err.println("Erreur: avatarImageView est null!");
            return;
        }

        User user = SessionManager.getCurrentUser();
        String avatarUrl = user != null ? user.getAvatar() : null;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                avatarUrl = cleanAvatarUrl(avatarUrl);

                if (!isValidImageUrl(avatarUrl)) {
                    setDefaultAvatar();
                    return;
                }

                // Load image with proper dimensions (80x80 to match FXML)
                Image image = new Image(avatarUrl, 80, 80, true, true, true);

                if (!image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setPreserveRatio(true);
                    avatarImageView.setSmooth(true);

                    // Set fit dimensions to match FXML
                    avatarImageView.setFitWidth(80);
                    avatarImageView.setFitHeight(80);

                    // Create circular clip centered - radius 40 for 80px image
                    Circle clip = new Circle(40, 40, 40);
                    avatarImageView.setClip(clip);

                    // Ensure image is visible
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
        // Corriger les URLs problématiques
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

    private String getRoleDisplayName(Role_enum role) {
        switch (role) {
            case ADMIN: return "Administrateur";
            case INVESTISSEUR: return "Investisseur";
            case STARTUP: return "Porteur de projet";
            default: return role.name();
        }
    }

    @FXML
    private void goToHome(ActionEvent event) {
        System.out.println("Déjà sur l'accueil");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/profile.fxml", "Mon Profil");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Déconnexion...");
        SessionManager.logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Parent root = stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
        });
        fadeOut.play();
    }

    @FXML
    private void viewStartups(ActionEvent event) {
        System.out.println("Voir startups - Redirection...");
        // Navigation vers page startups
    }

    @FXML
    private void viewCandidatures(ActionEvent event) {
        System.out.println("Mes candidatures - Redirection...");
        // Navigation vers page candidatures
    }

    @FXML
    private void viewEvenements(ActionEvent event) {
        System.out.println("Événements - Redirection...");
        // Navigation vers page événements
    }

    @FXML
    private void viewInvestments(ActionEvent event) {
        System.out.println("Mes investissements - Redirection...");
        // À implémenter pour investisseurs
    }

    @FXML
    private void viewMyStartup(ActionEvent event) {
        System.out.println("Ma startup - Redirection...");
        // À implémenter pour startups
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }
}
