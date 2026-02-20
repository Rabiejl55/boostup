package controllers;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressBar;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import entities.GAccompagnement.Coach;
import services.AccompagnementService.CoachService;

import entities.GAccompagnement.Domaine;
import entities.GAccompagnement.Session;
import services.AccompagnementService.DomaineService;
import services.AccompagnementService.SessionService;

import java.sql.SQLException;
import java.util.List;
import java.io.File;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressBar;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import java.util.Optional;

public class HomeController {

    @FXML private FlowPane domainesContainer;
    @FXML private TilePane sessionsGrid;
    @FXML private FlowPane coachesContainer;

    private final SessionService sessionService = new SessionService();
    private final CoachService coachService = new CoachService();
    private final DomaineService domaineService = new DomaineService();

    @FXML
    public void initialize() {
        try {
            chargerDomaines();
            chargerSessions();
            chargerCoaches();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================== HANDLERS STATIQUES ========================

    @FXML private void handleDashboardClick(MouseEvent event) {
        System.out.println("Dashboard cliqué (statique)");
    }

    @FXML private void handleAccompagnementsClick(MouseEvent event) {
        System.out.println("Accompagnements cliqué (statique)");
    }

    @FXML private void handleEventsClick(MouseEvent event) {
        System.out.println("Événements cliqué (statique)");
    }

    @FXML private void handleCandidaturesClick(MouseEvent event) {
        System.out.println("Candidatures cliqué (statique)");
    }

    @FXML private void handleFinancementClick(MouseEvent event) {
        System.out.println("Financement cliqué (statique)");
    }

    @FXML private void handleProfileClick(MouseEvent event) {
        System.out.println("Profil cliqué (statique)");
    }

    @FXML private void handleViewAllSessions(MouseEvent event) {
        System.out.println("Voir toutes les sessions (statique)");
    }
    @FXML
    private void handleLogout() {
        System.out.println("Déconnexion...");
        // TODO: ajouter logique de redirection vers login
    }
    @FXML private void handleViewAllCandidatures(MouseEvent event) {
        System.out.println("Voir toutes les candidatures (statique)");
    }
    @FXML private TilePane planningGrid; // ou sessionsGrid

    // ======================== DOMAINES ========================
    private void chargerDomaines() throws SQLException {
        domainesContainer.getChildren().clear();
        List<Domaine> domaines = domaineService.afficherAll();

        for (Domaine d : domaines) {
            VBox card = createDomaineCard(d);
            domainesContainer.getChildren().add(card);
        }
    }

    private VBox createDomaineCard(Domaine domaine) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                "-fx-cursor: hand;");

        // Effet de survol
        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8); " +
                        "-fx-scale-x: 1.02; " +
                        "-fx-scale-y: 1.02; " +
                        "-fx-cursor: hand;")
        );

        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                        "-fx-scale-x: 1; " +
                        "-fx-scale-y: 1; " +
                        "-fx-cursor: hand;")
        );

        // Conteneur pour l'image avec coins arrondis
        StackPane imageContainer = new StackPane();

        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(280);
        imageView.setPreserveRatio(false);

        if (domaine.getImage() != null) {
            File file = new File(domaine.getImage());
            if (file.exists()) {
                Image image = new Image(file.toURI().toString(), 280, 140, false, true);
                imageView.setImage(image);
            }
        }

        // Appliquer des coins arrondis à l'image
        Rectangle clip = new Rectangle(280, 140);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        imageContainer.getChildren().add(imageView);

        // Badge
        Label categoryBadge = new Label("Domaine d'expertise");
        categoryBadge.setStyle("-fx-background-color: #eef2ff; " +
                "-fx-text-fill: #4f46e5; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 5 12; " +
                "-fx-background-radius: 20;");
        categoryBadge.setMaxWidth(Region.USE_PREF_SIZE);

        Label nom = new Label(domaine.getNom());
        nom.setStyle("-fx-font-size: 18px; " +
                "-fx-font-weight: 700; " +
                "-fx-text-fill: #1e293b;");

        Label desc = new Label(domaine.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #64748b; " +
                "-fx-font-size: 13px;");
        desc.setMaxHeight(60);

        // Statistiques simulées
        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        Label sessionsCount = new Label("📅 Sessions disponibles");
        sessionsCount.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        statsBox.getChildren().add(sessionsCount);

        Button btn = new Button("Explorer le domaine");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #4f46e5; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: #6366f1; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand;")
        );

        btn.setOnAction(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(0.95);
            st.setToY(0.95);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            System.out.println("Explorer domaine: " + domaine.getNom());
        });

        VBox contentBox = new VBox(12);
        contentBox.getChildren().addAll(categoryBadge, nom, desc, statsBox, btn);

        card.getChildren().addAll(imageContainer, contentBox);

        return card;
    }

// ======================== SESSIONS AMÉLIORÉES ========================

    private void chargerSessions() throws SQLException {
        sessionsGrid.getChildren().clear();
        List<Session> sessions = sessionService.afficherAll();

        for (Session s : sessions) {
            sessionsGrid.getChildren().add(createEnhancedSessionCard(s));
        }
    }

    private VBox createEnhancedSessionCard(Session session) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 20; " +
                "-fx-border-radius: 20; " +
                "-fx-border-width: 1; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // En-tête avec date
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Badge de date
        VBox dateBadge = new VBox(2);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.setPrefWidth(60);
        dateBadge.setPrefHeight(60);
        dateBadge.setStyle("-fx-background-color: #f8fafc; " +
                "-fx-background-radius: 15; " +
                "-fx-border-radius: 15; " +
                "-fx-border-width: 1; " +
                "-fx-border-color: #e2e8f0;");

        // Traitement de la date
        Object dateObj = session.getDateSession();
        if (dateObj != null) {
            if (dateObj instanceof LocalDate) {
                LocalDate date = (LocalDate) dateObj;
                Label month = new Label(date.format(DateTimeFormatter.ofPattern("MMM")));
                month.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-font-weight: 600;");

                Label day = new Label(String.valueOf(date.getDayOfMonth()));
                day.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");

                dateBadge.getChildren().addAll(month, day);
            }
        }

        // Infos session
        VBox sessionInfo = new VBox(4);
        sessionInfo.setAlignment(Pos.CENTER_LEFT);

        Label objectif = new Label(session.getObjectif());
        objectif.setWrapText(true);
        objectif.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-text-fill: #1e293b;");

        sessionInfo.getChildren().add(objectif);

        header.getChildren().addAll(dateBadge, sessionInfo);

        // Détails supplémentaires
        VBox details = new VBox(8);
        details.setPadding(new Insets(10, 0, 0, 0));

        Label lieu = new Label("📍 " + session.getLieu());
        lieu.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        // Tags
        FlowPane tagsPane = new FlowPane(5, 5);
        String[] tags = {"Formation", "Session"};
        for (String tag : tags) {
            Label tagLabel = new Label("#" + tag);
            tagLabel.setStyle("-fx-background-color: #f1f5f9; " +
                    "-fx-text-fill: #475569; " +
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-radius: 15;");
            tagsPane.getChildren().add(tagLabel);
        }

        details.getChildren().addAll(lieu, tagsPane);

        // Bouton d'inscription
        Button btn = new Button("S'inscrire à la session");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #3b82f6; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: #2563eb; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand;")
        );

        btn.setOnAction(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(0.95);
            st.setToY(0.95);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            System.out.println("Inscription session: " + session.getObjectif());
            showInscriptionConfirmation(session);
        });

        card.getChildren().addAll(header, details, btn);

        return card;
    }

// ======================== COACHS EN LISTE ========================

    private void chargerCoaches() throws SQLException {
        coachesContainer.getChildren().clear();
        List<Coach> coaches = coachService.afficherAll();

        // Vue en liste
        VBox coachesList = new VBox(15);
        coachesList.setPrefWidth(1200);

        for (Coach c : coaches) {
            coachesList.getChildren().add(createCoachListItem(c));
        }

        coachesContainer.getChildren().add(coachesList);
    }

    private HBox createCoachListItem(Coach coach) {
        HBox item = new HBox(20);
        item.setPadding(new Insets(20));
        item.setPrefWidth(1200);
        item.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); " +
                "-fx-border-width: 1; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 16; " +
                "-fx-cursor: hand;");

        // Effet de survol
        item.setOnMouseEntered(e ->
                item.setStyle("-fx-background-color: #f8fafc; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-color: #cbd5e1; " +
                        "-fx-border-radius: 16; " +
                        "-fx-cursor: hand;")
        );

        item.setOnMouseExited(e ->
                item.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-radius: 16; " +
                        "-fx-cursor: hand;")
        );

        // Avatar
        StackPane avatarContainer = new StackPane();

        Circle avatarCircle = new Circle(45);
        avatarCircle.setFill(Color.web("#e2e8f0"));
        avatarCircle.setStroke(Color.web("#cbd5e1"));
        avatarCircle.setStrokeWidth(2);

        ImageView imageView = new ImageView();
        imageView.setFitHeight(80);
        imageView.setFitWidth(80);
        imageView.setPreserveRatio(true);

        if (coach.getImagecoach() != null) {
            File file = new File(coach.getImagecoach());
            if (file.exists()) {
                Image image = new Image(file.toURI().toString(), 80, 80, false, true);
                imageView.setImage(image);

                // Créer un cercle pour le clip
                Circle clip = new Circle(40);
                clip.setCenterX(40);
                clip.setCenterY(40);
                imageView.setClip(clip);
            }
        }

        avatarContainer.getChildren().addAll(avatarCircle, imageView);

        // Informations du coach
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nom = new Label(coach.getNom() + " " + coach.getPrenom());
        nom.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");

        // Badges
        HBox expertiseBox = new HBox(10);
        String[] expertises = {"Coach", "Expert"};
        for (String exp : expertises) {
            Label badge = new Label(exp);
            badge.setStyle("-fx-background-color: #eef2ff; " +
                    "-fx-text-fill: #4f46e5; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-padding: 4 12; " +
                    "-fx-background-radius: 20;");
            expertiseBox.getChildren().add(badge);
        }

        Label email = new Label("✉️ " + coach.getEmail());
        email.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nom, expertiseBox, email);

        // Bouton d'action
        Button voirProfilBtn = new Button("Voir le profil");
        voirProfilBtn.setStyle("-fx-background-color: #4f46e5; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand;");

        voirProfilBtn.setOnMouseEntered(e ->
                voirProfilBtn.setStyle("-fx-background-color: #6366f1; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand;")
        );

        voirProfilBtn.setOnAction(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), voirProfilBtn);
            st.setToX(0.95);
            st.setToY(0.95);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            System.out.println("Voir profil: " + coach.getNom());
            showCoachProfile(coach);
        });

        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.getChildren().add(voirProfilBtn);

        item.getChildren().addAll(avatarContainer, infoBox, actionBox);

        return item;
    }

    // Méthodes utilitaires
    private void showInscriptionConfirmation(Session session) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation d'inscription");
        alert.setHeaderText("Inscription à la session");
        alert.setContentText("Voulez-vous vous inscrire à la session : " + session.getObjectif() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Inscription confirmée pour la session: " + session.getObjectif());
        }
    }

    private void showCoachProfile(Coach coach) {
        System.out.println("Affichage du profil détaillé de: " + coach.getNom());
    }
}
