package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
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
import services.UtilisateurService.UserService;

import java.sql.SQLException;

public class ProfileController {

    @FXML private Label welcomeLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label phoneLabel;
    @FXML private Label fullnameLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Label dateCreationLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            if (welcomeLabel != null) welcomeLabel.setText("Erreur : non connecté");
            return;
        }

        // Récupérer les données complètes de l'utilisateur depuis la base
        try {
            User fullUser = userService.findById(user.getId());
            if (fullUser == null) {
                showErrorMessage("Utilisateur non trouvé");
                return;
            }

            // Mettre à jour la session avec les données complètes
            SessionManager.setCurrentUser(fullUser);
            user = fullUser;
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur chargement des données");
        }

        // Afficher les informations
        String displayName = user.getFullname() != null && !user.getFullname().isEmpty()
                ? user.getFullname() : user.getNom();

        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue " + displayName);
        if (nomLabel != null) nomLabel.setText(user.getNom());
        if (emailLabel != null) emailLabel.setText(user.getEmail());
        if (roleLabel != null) roleLabel.setText(getRoleDisplayName(user.getRole()));

        if (fullnameLabel != null) {
            if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                fullnameLabel.setText(user.getFullname());
            } else {
                fullnameLabel.setText("Non renseigné");
            }
        }

        if (phoneLabel != null) {
            phoneLabel.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                    ? user.getPhone() : "Non renseigné");
        }

        if (dateCreationLabel != null && user.getDateCreation() != null) {
            dateCreationLabel.setText("Membre depuis: " +
                    new java.text.SimpleDateFormat("dd/MM/yyyy").format(user.getDateCreation()));
        }

        // Charger l'avatar
        // Charger l'avatar
        if (avatarImageView != null) {
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setSmooth(true);

            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                try {
                    System.out.println("Chargement avatar: " + user.getAvatar());
                    String avatarUrl = cleanAvatarUrl(user.getAvatar());

                    // Load image with proper dimensions
                    Image image = new Image(avatarUrl, 150, 150, true, true, true);

                    if (!image.isError()) {
                        avatarImageView.setImage(image);

                        // Create circular clip
                        Circle clip = new Circle(75, 75, 75);
                        avatarImageView.setClip(clip);
                        avatarImageView.setFitWidth(150);
                        avatarImageView.setFitHeight(150);
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
    }

    private String cleanAvatarUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        // Si c'est un chemin de fichier local avec backslashes, les convertir en forward slashes
        if (url.contains("\\")) {
            url = url.replace("\\", "/");
        }
        // Si c'est un chemin local sans file://, ajouter file://
        if (url.startsWith("C:/") || url.startsWith("D:/") || url.startsWith("/")) {
            if (!url.startsWith("file://")) {
                url = "file:///" + url;
            }
        }
        // Corriger les URLs problématiques
        if (url.contains(" ")) url = url.replace(" ", "%20");
        if (url.contains("'")) url = url.replace("'", "%27");
        return url;
    }

    private void setDefaultAvatar() {
        User user = SessionManager.getCurrentUser();
        if (user != null && avatarImageView != null) {
            int colorIndex = Math.abs(user.getId() % 8);
            String[] colors = {"#0d6efd", "#198754", "#6f42c1", "#fd7e14",
                    "#dc3545", "#20c997", "#6610f2", "#ffc107"};

            WritableImage image = new WritableImage(150, 150);
            PixelWriter writer = image.getPixelWriter();
            Color color = Color.web(colors[colorIndex]);

            for (int y = 0; y < 150; y++) {
                for (int x = 0; x < 150; x++) {
                    writer.setColor(x, y, color);
                }
            }

            avatarImageView.setImage(image);
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setFitWidth(150);
            avatarImageView.setFitHeight(150);

            Circle clip = new Circle(75, 75, 75);
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
    private void editProfile(ActionEvent event) {
        System.out.println("Modifier profil cliqué");

        // Afficher un message que cette fonctionnalité n'est pas encore disponible
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à venir");
        alert.setHeaderText("Modification du profil");
        alert.setContentText("Cette fonctionnalité sera disponible prochainement !");
        alert.showAndWait();

        // OU créer un dialogue simple de modification ici
        // createSimpleEditDialog();
    }

    // Méthode alternative pour créer un dialogue de modification simple
    private void createSimpleEditDialog() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Créer un dialogue simple
        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog(user.getFullname());
        dialog.setTitle("Modifier le nom complet");
        dialog.setHeaderText("Modifier votre nom complet");
        dialog.setContentText("Nom complet:");

        dialog.showAndWait().ifPresent(result -> {
            try {
                user.setFullname(result);
                userService.update(user);
                // Rafraîchir la page
                initialize();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

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
        SessionManager.logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
    }

    private void showErrorMessage(String message) {
        if (welcomeLabel != null) {
            welcomeLabel.setText(message);
            welcomeLabel.setStyle("-fx-text-fill: red;");
        }
    }
}