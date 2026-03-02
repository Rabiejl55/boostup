package controllers;

import entities.GAccompagnement.Coach;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import services.AccompagnementService.CoachService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CoachController {

    private final CoachService coachService = new CoachService();

    // ===== TABLE COACH =====
    @FXML private TableView<Coach> coachsTable;
    @FXML private TableColumn<Coach, String> nomColumn;
    @FXML private TableColumn<Coach, String> prenomColumn;
    @FXML private TableColumn<Coach, String> emailColumn;
    @FXML private TableColumn<Coach, String> telephoneColumn;
    @FXML private TableColumn<Coach, String> imageColumn;

    @FXML private Label feedbackLabel;

    // ===== SIDEBAR AVATAR =====
    @FXML private ImageView avatarImageView;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        // Vérifications préliminaires
        if (avatarImageView == null) System.err.println("avatarImageView est null !");
        if (welcomeLabel == null) System.err.println("welcomeLabel est null !");
        if (coachsTable == null) System.err.println("coachsTable est null !");

        // Lier les colonnes
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("imagecoach"));

        // Colonne image personnalisée
        imageColumn.setCellFactory(column -> new TableCell<Coach, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new FileInputStream(imagePath));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                        System.out.println("Image non trouvée : " + imagePath);
                    }
                }
            }
        });

        // Charger les coachs
        loadCoachs();

        // Mettre un avatar par défaut et texte
        setSidebarAvatar(null);
        setSidebarWelcomeText("Administrateur");

        // Mettre à jour l'avatar sidebar selon le coach sélectionné
        coachsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            setSidebarAvatar(newSel);
        });
    }

    // ================= LOAD DATA =================
    private void loadCoachs() {
        try {
            coachsTable.setItems(FXCollections.observableArrayList(coachService.afficherAll()));
            setFeedback("Chargement réussi.");
        } catch (Exception e) {
            setFeedback("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= SIDEBAR UTILITIES =================
    private void setSidebarAvatar(Coach coach) {
        if (avatarImageView == null) return;

        Image img = null;

        if (coach != null && coach.getImagecoach() != null && !coach.getImagecoach().isEmpty()) {
            try {
                img = new Image(new FileInputStream(coach.getImagecoach()));
            } catch (FileNotFoundException e) {
                System.out.println("Image du coach non trouvée : " + coach.getImagecoach());
            }
        }

        if (img == null) {
            // Image par défaut dynamique
            img = generateDefaultAvatar(avatarImageView.getFitWidth(), coach);
        }

        avatarImageView.setImage(img);

        // Clip circulaire
        double radius = avatarImageView.getFitWidth() / 2;
        Circle clip = new Circle(radius, radius, radius);
        avatarImageView.setClip(clip);
    }

    private void setSidebarWelcomeText(String text) {
        if (welcomeLabel != null) welcomeLabel.setText(text);
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) feedbackLabel.setText(message);
    }

    private Image generateDefaultAvatar(double size, Coach coach) {
        int intSize = (int) size;
        WritableImage image = new WritableImage(intSize, intSize);
        PixelWriter writer = image.getPixelWriter();

        int colorIndex = (coach != null) ? Math.abs(coach.getIdCoach() % 8) : 0;
        String[] colors = {"#0d6efd", "#198754", "#6f42c1", "#fd7e14",
                "#dc3545", "#20c997", "#6610f2", "#ffc107"};
        Color color = Color.web(colors[colorIndex]);

        for (int y = 0; y < intSize; y++) {
            for (int x = 0; x < intSize; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    // ================= NAVIGATION =================
    @FXML private void goToFront(ActionEvent event) { navigateTo(event, "/fxml/home.fxml", "HomePage"); }
    @FXML private void goToDomaine(ActionEvent event) { navigateTo(event, "/fxml/Domaine.fxml", "Gestion Domaines"); }
    @FXML private void goToSession(ActionEvent event) { navigateTo(event, "/fxml/Session.fxml", "Gestion Sessions"); }
    @FXML private void goToHome(ActionEvent event) { navigateTo(event, "/fxml/home.fxml", "Gestion Sessions"); }

    @FXML private void handleLogout(ActionEvent event) {
        setFeedback("Déconnexion !");
        navigateTo(event, "/fxml/login.fxml", "Connexion");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= COACH CRUD =================
    @FXML private void ajouterCoach() {
        openCoachForm("/fxml/AddCoachForm.fxml", "Ajouter Coach");
    }

    @FXML private void modifierCoach() {
        Coach selected = coachsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditCoachForm.fxml"));
                Parent root = loader.load();
                EditCoachController controller = loader.getController();
                controller.setCoach(selected);
                Stage stage = new Stage();
                stage.setTitle("Modifier Coach");
                stage.setScene(new Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadCoachs();
            } catch (Exception e) {
                setFeedback("Erreur ouverture formulaire : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            setFeedback("Veuillez sélectionner un coach à modifier.");
        }
    }

    @FXML private void supprimerCoach() {
        Coach selected = coachsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                coachService.supprimer(selected.getIdCoach());
                loadCoachs();
                setFeedback("Coach supprimé avec succès.");
            } catch (Exception e) {
                setFeedback("Erreur suppression : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            setFeedback("Veuillez sélectionner un coach à supprimer.");
        }
    }
    @FXML
    private void goToDashboard() {
        // code pour afficher le dashboard
    }

    @FXML
    private void viewSessions() {
        // code pour afficher les sessions du coach
    }

    @FXML
    private void viewParticipants() {
        // code pour afficher les participants
    }

    @FXML
    private void viewNotifications() {
        // code pour afficher les notifications
    }

    private void openCoachForm(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadCoachs();
        } catch (Exception e) {
            setFeedback("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= Méthodes sidebar utilisateur =================
    @FXML
    private void goToProfile() {
        // à implémenter plus tard
    }

    @FXML
    private void goToHome() {
        // à implémenter plus tard
    }

    @FXML
    private void viewStartups() {
        // à implémenter plus tard
    }

    @FXML
    private void viewEvenements() {
        // à implémenter plus tard
    }

    @FXML
    private void viewCandidatures() {
        // à implémenter plus tard
    }

    @FXML
    private void viewInvestments() {
        // à implémenter plus tard
    }

    @FXML
    private void viewMyStartup() {
        // à implémenter plus tard
    }
}