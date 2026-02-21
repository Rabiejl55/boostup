package controllers;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.BarcodeFormat;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import entities.GAccompagnement.Coach;
import entities.GAccompagnement.Domaine;
import entities.GAccompagnement.Session;
import services.AccompagnementService.CoachService;
import services.AccompagnementService.DomaineService;
import services.AccompagnementService.SessionService;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;



import entities.GAccompagnement.Session;

import java.time.LocalDate;
import java.util.List;

public class HomeController {
    // ================= SIDEBAR =================
    @FXML private ImageView sidebarAvatarView;
    @FXML private Label welcomeLabel;
    // ================= SESSIONS =================
    @FXML private ComboBox<Session> sessionComboBox;         // <-- ajouté
    @FXML private ImageView qrCodeImageViewSession;         // <-- ajouté

    // ================= COACHS =================
    @FXML private ComboBox<Coach> coachComboBox;           // <-- ajouté
    @FXML private ImageView qrCodeImageViewCoach;

    // ================= DOMAINES =================
    @FXML private TextField searchDomainesField;
    @FXML private FlowPane domainesContainer;
    @FXML private Label domainesCount;
    @FXML
    private ComboBox<Domaine> domaineComboBox;    @FXML private ImageView qrCodeImageView;

    // ================= SESSIONS =================
    @FXML private TextField searchSessionsField;
    @FXML private TilePane sessionsGrid;
    @FXML private Label sessionsCount;
    @FXML
    private void handleDashboardClick(ActionEvent event) {
        System.out.println("Dashboard clicked");
    }

    @FXML
    private void handleAccompagnementsClick(ActionEvent event) {
        System.out.println("Domaines clicked");
    }

    @FXML
    private void handleEventsClick(ActionEvent event) {
        System.out.println("Sessions clicked");
    }

    @FXML
    private void handleCandidaturesClick(ActionEvent event) {
        System.out.println("Coachs clicked");
    }

    @FXML
    private void handleFinancementClick(ActionEvent event) {
        System.out.println("Financement clicked");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Logout clicked");
    }
    // ================= COACHS =================
    @FXML private TextField searchCoachsField;
    @FXML private FlowPane coachesContainer;
    @FXML private Label coachsCount;
    @FXML

    private final SessionService sessionService = new SessionService();
    private final CoachService coachService = new CoachService();
    private final DomaineService domaineService = new DomaineService();

    @FXML
    public void initialize() {
        try {
            chargerComboBoxDomaines();
            chargerComboBoxSessions();
            chargerComboBoxCoachs();
            chargerDomaines();
            chargerSessions();
            chargerCoaches();
            chargerComboBoxDomaines();
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchDomainesField.textProperty().addListener((obs, oldValue, newValue) -> rechercherDomaines(newValue));
        searchSessionsField.textProperty().addListener((obs, oldValue, newValue) -> rechercherSessions(newValue));
        searchCoachsField.textProperty().addListener((obs, oldValue, newValue) -> rechercherCoaches(newValue));
    }

    // ======================== DOMAINES ========================
    private void chargerDomaines() throws SQLException {
        domainesContainer.getChildren().clear();
        List<Domaine> domaines = domaineService.afficherAll();
        for (Domaine d : domaines) {
            domainesContainer.getChildren().add(createDomaineCard(d));
        }
    }

    private VBox createDomaineCard(Domaine domaine) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15,0,0,5); -fx-cursor: hand;");

        // Image
        StackPane imageContainer = new StackPane();
        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(280);
        imageView.setPreserveRatio(false);
        if (domaine.getImage() != null) {
            File file = new File(domaine.getImage());
            if (file.exists()) imageView.setImage(new Image(file.toURI().toString(), 280, 140, false, true));
        }
        Rectangle clip = new Rectangle(280, 140); clip.setArcWidth(20); clip.setArcHeight(20);
        imageView.setClip(clip);
        imageContainer.getChildren().add(imageView);

        // Contenu
        Label categoryBadge = new Label("Domaine d'expertise");
        categoryBadge.setStyle("-fx-background-color: #eef2ff; -fx-text-fill: #4f46e5; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 20;");

        Label nom = new Label(domaine.getNom());
        nom.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");

        Label desc = new Label(domaine.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;"); desc.setMaxHeight(60);

        Button btn = new Button("Explorer le domaine");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12; -fx-background-radius: 12; -fx-cursor: hand;");
        btn.setOnAction(e -> System.out.println("Explorer domaine: " + domaine.getNom()));

        VBox contentBox = new VBox(12); contentBox.getChildren().addAll(categoryBadge, nom, desc, btn);
        card.getChildren().addAll(imageContainer, contentBox);
        return card;
    }

    private void chargerComboBoxDomaines() throws SQLException {
        domaineComboBox.getItems().clear();
        List<Domaine> domaines = domaineService.afficherAll();
        for (Domaine d : domaines)
            domaineComboBox.getItems().add(d);
    }

    private void rechercherDomaines(String motCle) {
        try {
            domainesContainer.getChildren().clear();
            List<Domaine> domaines = domaineService.rechercherParNom(motCle);
            for (Domaine d : domaines) domainesContainer.getChildren().add(createDomaineCard(d));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ======================== SESSIONS ========================
    private void chargerSessions() throws SQLException {
        sessionsGrid.getChildren().clear();
        List<Session> sessions = sessionService.afficherAll();
        for (Session s : sessions) sessionsGrid.getChildren().add(createEnhancedSessionCard(s));
    }

    private VBox createEnhancedSessionCard(Session session) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-width: 1; -fx-border-color: #e2e8f0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05),10,0,0,2);");

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        VBox dateBadge = new VBox(2); dateBadge.setAlignment(Pos.CENTER); dateBadge.setPrefWidth(60); dateBadge.setPrefHeight(60);
        dateBadge.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-width:1; -fx-border-color:#e2e8f0;");

        LocalDate date = session.getDateSession();
        if (date != null) {
            Label month = new Label(date.format(DateTimeFormatter.ofPattern("MMM")));
            month.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-font-weight: 600;");
            Label day = new Label(String.valueOf(date.getDayOfMonth()));
            day.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
            dateBadge.getChildren().addAll(month, day);
        }

        VBox sessionInfo = new VBox(4); sessionInfo.setAlignment(Pos.CENTER_LEFT);
        Label objectif = new Label(session.getObjectif()); objectif.setWrapText(true);
        objectif.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-text-fill: #1e293b;");
        sessionInfo.getChildren().add(objectif);

        header.getChildren().addAll(dateBadge, sessionInfo);
        card.getChildren().add(header);

        return card;
    }

    private void rechercherSessions(String motCle) {
        try {
            sessionsGrid.getChildren().clear();
            List<Session> sessions = sessionService.rechercherParNom(motCle);
            for (Session s : sessions) sessionsGrid.getChildren().add(createEnhancedSessionCard(s));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ======================== COACHS ========================
    private void chargerCoaches() throws SQLException {
        coachesContainer.getChildren().clear();
        List<Coach> coaches = coachService.afficherAll();
        VBox coachesList = new VBox(15); coachesList.setPrefWidth(1200);
        for (Coach c : coaches) coachesList.getChildren().add(createCoachListItem(c));
        coachesContainer.getChildren().add(coachesList);
    }

    private HBox createCoachListItem(Coach coach) {
        HBox item = new HBox(20); item.setPadding(new Insets(20)); item.setPrefWidth(1200);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10,0,0,2); -fx-border-width: 1; -fx-border-color: #e2e8f0; -fx-border-radius: 16;");

        // Avatar
        StackPane avatarContainer = new StackPane();
        Circle avatarCircle = new Circle(45); avatarCircle.setFill(Color.web("#e2e8f0")); avatarCircle.setStroke(Color.web("#cbd5e1")); avatarCircle.setStrokeWidth(2);
        ImageView imageView = new ImageView(); imageView.setFitHeight(80); imageView.setFitWidth(80); imageView.setPreserveRatio(true);
        if (coach.getImagecoach() != null) { File file = new File(coach.getImagecoach()); if (file.exists()) imageView.setImage(new Image(file.toURI().toString(), 80, 80, false, true)); }
        avatarContainer.getChildren().addAll(avatarCircle, imageView);

        VBox infoBox = new VBox(8); infoBox.setAlignment(Pos.CENTER_LEFT);
        Label nom = new Label(coach.getNom() + " " + coach.getPrenom()); nom.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        infoBox.getChildren().add(nom);

        Button voirProfilBtn = new Button("Voir le profil"); voirProfilBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill:white;");
        voirProfilBtn.setOnAction(e -> System.out.println("Voir profil: " + coach.getNom()));

        VBox actionBox = new VBox(10); actionBox.setAlignment(Pos.CENTER); actionBox.getChildren().add(voirProfilBtn);
        item.getChildren().addAll(avatarContainer, infoBox, actionBox);
        return item;
    }

    private void rechercherCoaches(String motCle) {
        try {
            coachesContainer.getChildren().clear();
            List<Coach> coaches = coachService.rechercherParNom(motCle);
            VBox coachesList = new VBox(15); coachesList.setPrefWidth(1200);
            for (Coach c : coaches) coachesList.getChildren().add(createCoachListItem(c));
            coachesContainer.getChildren().add(coachesList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ======================== QR CODE ========================
    @FXML
    private void handleGenerateQRCode() {
        Domaine domaine = domaineComboBox.getValue();
        if (domaine == null) return;

        String qrContent = String.format(
                "Domaine : %s\n" +
                        "Description : %s\n" +
                        "Niveau : %s\n" +
                        "Statut : %s",
                domaine.getNom(),
                domaine.getDescription(),
                domaine.getNiveau(),
                domaine.getStatut()
        );
        try {
            int width = 300;
            int height = 300;

            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrContent,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    width, height,
                    hints);

            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.getPixelWriter().setColor(x, y, bitMatrix.get(x, y) ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
                }
            }

            qrCodeImageView.setImage(image);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleGenerateQRCodeCoach() {
        Coach coach = coachComboBox.getValue();
        if (coach == null) return;

        // Contenu QR Code lisible et professionnel
        String qrContent = String.format(
                "%s %s\n" +
                        "Email : %s\n" +
                        "Téléphone : %s\n" +
                coach.getNom(),
                coach.getPrenom(),
                coach.getEmail(),
                coach.getTelephone()
        );

        try {
            int width = 300;
            int height = 300;

            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrContent,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    width, height,
                    hints);

            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.getPixelWriter().setColor(x, y, bitMatrix.get(x, y) ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
                }
            }

            qrCodeImageViewCoach.setImage(image); // ImageView spécifique pour Coachs

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleGenerateQRCodeSession() {
        Session session = sessionComboBox.getValue();
        if (session == null) return;

        // Contenu QR Code lisible et professionnel
        String qrContent = String.format(
                "Session : %s\n" +
                        "Date : %s\n" +
                        "Durée : %d h\n" +
                        "Lieu : %s\n" +
                        "Objectif : %s\n" +
                        "Coach : %s %s\n" +
                        "Domaine : %s",
                session.getTypeSession(),
                session.getDateSession(),
                session.getDuree(),
                session.getLieu(),
                session.getObjectif(),
                session.getCoach().getNom(),
                session.getCoach().getPrenom(),
                session.getDomaine().getNom()
        );

        try {
            int width = 300;
            int height = 300;

            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrContent,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    width, height,
                    hints);

            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.getPixelWriter().setColor(x, y, bitMatrix.get(x, y) ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
                }
            }

            qrCodeImageViewSession.setImage(image); // ImageView spécifique pour Sessions

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void chargerComboBoxSessions() throws SQLException {
        sessionComboBox.getItems().clear();
        List<Session> sessions = sessionService.afficherAll();
        for (Session s : sessions) {
            sessionComboBox.getItems().add(s);
        }
    }

    private void chargerComboBoxCoachs() throws SQLException {
        coachComboBox.getItems().clear();
        List<Coach> coaches = coachService.afficherAll();
        for (Coach c : coaches) {
            coachComboBox.getItems().add(c);
        }
    }
    @FXML
    private void handleOpenMap() {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MapView.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("🗺 Carte des Sessions");
            stage.setScene(new Scene(root, 1000, 650));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleCalendarClick(ActionEvent event) {
        System.out.println("Calendrier cliqué !");
        // Ici tu peux ouvrir une nouvelle fenêtre, afficher un calendrier, etc.
    }
}