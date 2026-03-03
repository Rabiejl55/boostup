package controllers;

import entities.GUtilisateurs.User;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.UtilisateurService.ActivityLogService;

/**
 * Controller for the unified Admin sidebar (sidebarADMIN.fxml).
 * Handles all navigation, user avatar/info, and logout.
 */
public class SidebarAdminController {

    @FXML private ImageView avatarImageView;
    @FXML private Label welcomeLabel;

    // ═══════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String displayName = user.getFullname() != null && !user.getFullname().isEmpty()
                    ? user.getFullname() : user.getNom();
            if (welcomeLabel != null) {
                welcomeLabel.setText(displayName);
            }
            loadAvatar();
        }
    }

    // ═══════════════════════════════════════════════════
    //  AVATAR
    // ═══════════════════════════════════════════════════

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
                Image image = new Image(avatarUrl, 80, 80, true, true, true);
                if (!image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setPreserveRatio(true);
                    avatarImageView.setSmooth(true);
                    avatarImageView.setFitWidth(80);
                    avatarImageView.setFitHeight(80);
                    Circle clip = new Circle(40, 40, 40);
                    avatarImageView.setClip(clip);
                    avatarImageView.setVisible(true);
                } else {
                    setDefaultAvatar();
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement avatar (sidebar admin): " + e.getMessage());
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
        if (avatarImageView == null) return;
        User user = SessionManager.getCurrentUser();

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

        WritableImage image = new WritableImage(80, 80);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 80; y++) {
            for (int x = 0; x < 80; x++) {
                double ratio = (double) (x + y) / 160.0;
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

    // ═══════════════════════════════════════════════════
    //  HELPER — get Stage from ActionEvent
    // ═══════════════════════════════════════════════════

    private Stage getStage(ActionEvent event) {
        return (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
    }

    // ═══════════════════════════════════════════════════
    //  PROFIL BUTTONS
    // ═════════════════════════════════════════════��═════

    @FXML
    private void goToProfile(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/profile.fxml", "Mon Profil");
    }

    // ═══════════════════════════════════════════════════
    //  MENU PRINCIPAL
    // ═══════════════════════════════════════════════════

    @FXML
    private void goToDashboard(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/admin-dashboard.fxml", "Tableau de bord Admin");
    }

    @FXML
    private void openUsersManagement(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/users-management.fxml", "Gestion Utilisateurs");
    }

    @FXML
    private void goToEvenements(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/EvenementView.fxml", "Événements");
    }

    @FXML
    private void goToFrontEvenements(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/FrontEvenementsView.fxml", "Front Événements");
    }

    @FXML
    private void goToParticipation(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/ParticipationView.fxml", "Participations");
    }

    @FXML
    private void goDashboard(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/dashboard.fxml", "Mon Dashboard");
    }
    @FXML
    private void manageFinancements(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/financement_front.fxml", "Financements");
    }

    @FXML
    private void goToInvestissements(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/investissement.fxml", "Investissements");
    }

    @FXML
    private void goToTransactions(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/transaction.fxml", "Transactions");
    }

    @FXML
    private void goToStatsFinancement(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/stats_financement.fxml", "Stats Financement");
    }

    @FXML
    private void viewStats(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/stats_financement.fxml", "Statistiques");
    }

    @FXML
    private void moderateContent(ActionEvent event) {
        System.out.println("🛡️ Modérer contenus — À implémenter");
    }

    @FXML
    private void systemSettings(ActionEvent event) {
        System.out.println("⚙️ Paramètres système — À implémenter");
    }

    // ═══════════════════════════════════════════════════
    //  GESTION
    // ═══════════════════════════════════════════════════

    @FXML
    private void goToDomaine(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/Domaine.fxml", "Gestion Domaines");
    }

    @FXML
    private void goToSession(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/Session.fxml", "Gestion Sessions");
    }

    @FXML
    private void goToCoach(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/accompagnement.fxml", "Gestion Coachs");
    }

    @FXML
    private void goToAccompagnement(ActionEvent event) {
        // Rediriger vers la page d'accueil FrontOffice avec le même sidebar
        NavigationHelper.navigateTo(getStage(event), "/fxml/home.fxml", "Accompagnement");
    }

 /*   @FXML
    private void viewStartups(ActionEvent event) {
        System.out.println("🚀 Voir startups — À implémenter");
    }*/

    @FXML
    private void viewCandidatures(ActionEvent event) {
        System.out.println("📝 Voir candidatures — À implémenter");
    }

    @FXML
    private void handleFeedbackClick(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/Feedback.fxml", "Feedback");
    }

    // ═══════════════════════════════════════════════════
    //  FOOTER
    // ═══════════════════════════════════════════════════

    @FXML
    private void handleViewActivityLog(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/activity-log.fxml", "Journal d'activité");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            try {
                ActivityLogService.log(user.getId(), user.getEmail(),
                        ActivityLogService.ACTION_LOGOUT, "Déconnexion via sidebar admin");
            } catch (Exception e) {
                System.err.println("Erreur log déconnexion: " + e.getMessage());
            }
        }
        SessionManager.logout();

        Stage stage = getStage(event);
        Parent root = stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e ->
                NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion")
        );
        fadeOut.play();
    }
    @FXML
    private void goToDomaineFrontOffice(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/home.fxml", "Domaines FrontOffice");
    }
    @FXML private ToggleButton themeToggle;
    @FXML
    private void toggleTheme(javafx.event.ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }


    @FXML
    private void handleCondidatureClick(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/DashboardBackofficeView.fxml", "Condidature");
    }

    @FXML
    private void handleCandidatureListClick(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/CandidatureFrontAdmin.fxml", "Liste Candidatures");
    }

    @FXML
    private void handleDossierListClick(ActionEvent event) {
        NavigationHelper.navigateTo(getStage(event), "/fxml/DossierFrontAdmin.fxml", "Liste Dossiers");
    }
}
