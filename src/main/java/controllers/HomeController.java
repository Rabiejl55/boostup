package controllers;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.File;
import java.io.IOException;

public class HomeController {

    @FXML
    private FlowPane domainesContainer;

    @FXML
    private TilePane planningGrid;

    @FXML
    private FlowPane coachesContainer;

    // SUPPRIMEZ CES LIGNES SI ELLES EXISTENT
    // @FXML private Label statsProgrammes;
    // @FXML private Label statsParticipants;
    // @FXML private Label statsCoachs;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMM");
    private final SessionService sessionService = new SessionService();
    private final CoachService coachService = new CoachService();
    private final DomaineService domaineService = new DomaineService();

    @FXML
    public void initialize() {
        System.out.println("=== DÉBOGAGE: Initialisation de HomeController ===");

        try {
            System.out.println("Chargement des domaines...");
            chargerDomaines();
            System.out.println("✓ Domaines chargés avec succès");
        } catch (Exception e) {
            System.err.println("✗ Erreur chargement domaines: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("Chargement des sessions...");
            chargerSessions();
            System.out.println("✓ Sessions chargées avec succès");
        } catch (Exception e) {
            System.err.println("✗ Erreur chargement sessions: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("Chargement des coaches...");
            chargerCoaches();
            System.out.println("✓ Coaches chargés avec succès");
        } catch (Exception e) {
            System.err.println("✗ Erreur chargement coaches: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Initialisation terminée ===");
    }

    // SUPPRIMEZ COMPLÈTEMENT LA MÉTHODE updateStatistics()
    // private void updateStatistics() { ... }

    // ======================== NAVIGATION ========================
    @FXML
    private void handleTableauDeBordClick(MouseEvent event) {
        System.out.println("=== Navigation vers tableau de bord ===");

        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            // Charger le fichier FXML d'accompagnement
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/accompagnement.fxml"));
            Parent root = loader.load();

            // Changer la scène
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println("✓ Navigation réussie");

        } catch (IOException e) {
            System.err.println("✗ Erreur lors de la navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ======================== DOMAINES ========================
    private void chargerDomaines() {
        try {
            domainesContainer.getChildren().clear();
            List<Domaine> domaines = domaineService.afficherAll();
            System.out.println("  - " + domaines.size() + " domaines trouvés");

            for (Domaine d : domaines) {
                VBox card = createDomaineCard(d);
                domainesContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL domaines: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createDomaineCard(Domaine domaine) {
        VBox card = new VBox();
        card.getStyleClass().add("domain-card");
        card.setSpacing(0);
        card.setPadding(new Insets(0));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);
        card.setPrefHeight(280);

        // ImageView pour l'image du domaine
        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(280);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.getStyleClass().add("domaine-image");

        // Charger l'image du domaine
        if (domaine.getImage() != null && !domaine.getImage().isEmpty()) {
            try {
                File file = new File(domaine.getImage());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                } else {
                    String imagePath = "file:" + domaine.getImage();
                    Image image = new Image(imagePath, true);
                    imageView.setImage(image);
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement image domaine: " + e.getMessage());
                setDefaultDomaineImage(imageView);
            }
        } else {
            setDefaultDomaineImage(imageView);
        }

        // Contenu texte
        VBox content = new VBox();
        content.getStyleClass().add("domain-content");
        content.setSpacing(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(20));

        Label nom = new Label(domaine.getNom());
        nom.getStyleClass().add("domain-name");
        nom.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label desc = new Label(domaine.getDescription());
        desc.getStyleClass().add("domain-description");
        desc.setWrapText(true);
        desc.setMaxWidth(240);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        Button explorerBtn = new Button("Explorer");
        explorerBtn.getStyleClass().add("explorer-button");
        explorerBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 20;");
        explorerBtn.setMaxWidth(120);
        explorerBtn.setOnAction(e -> System.out.println("Explorer domaine: " + domaine.getNom()));

        content.getChildren().addAll(nom, desc, explorerBtn);
        card.getChildren().addAll(imageView, content);

        return card;
    }

    // ======================== SESSIONS ========================
    private void chargerSessions() {
        try {
            planningGrid.getChildren().clear();
            List<Session> sessions = sessionService.afficherAll();
            System.out.println("  - " + sessions.size() + " sessions trouvées");

            // Limiter à 6 sessions pour l'affichage
            int count = 0;
            for (Session s : sessions) {
                if (count >= 6) break;
                VBox sessionCard = createSessionCard(s);
                planningGrid.getChildren().add(sessionCard);
                count++;
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL sessions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createSessionCard(Session session) {
        VBox card = new VBox();
        card.getStyleClass().add("session-card");
        card.setSpacing(12);
        card.setPadding(new Insets(18));
        card.setPrefWidth(320);
        card.setPrefHeight(350);
        card.setAlignment(Pos.TOP_LEFT);

        // Badge de type de session
        HBox typeBadge = new HBox();
        typeBadge.getStyleClass().add("session-type-badge");
        typeBadge.setPadding(new Insets(4, 12, 4, 12));
        typeBadge.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 20;");

        Label typeLabel = new Label(session.getTypeSession());
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        typeBadge.getChildren().add(typeLabel);

        // Date et durée
        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 14px;");

        Label dateLabel = new Label(formatDate(session.getDateSession().toString()));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label dureeLabel = new Label("(" + session.getDuree() + "h)");
        dureeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        dateBox.getChildren().addAll(dateIcon, dateLabel, dureeLabel);

        // Objectif
        Label objectifLabel = new Label(session.getObjectif());
        objectifLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        objectifLabel.setWrapText(true);
        objectifLabel.setMaxWidth(280);

        // Lieu
        HBox lieuBox = new HBox(8);
        lieuBox.setAlignment(Pos.CENTER_LEFT);

        Label lieuIcon = new Label("📍");
        lieuIcon.setStyle("-fx-font-size: 14px;");

        Label lieuText = new Label(session.getLieu());
        lieuText.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        lieuBox.getChildren().addAll(lieuIcon, lieuText);

        // SECTION COACH
        VBox coachSection = new VBox(8);
        coachSection.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");

        Label coachTitle = new Label("👨‍🏫 Coach de la session");
        coachTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        if (session.getCoach() != null) {
            Coach coach = session.getCoach();

            HBox coachInfo = new HBox(12);
            coachInfo.setAlignment(Pos.CENTER_LEFT);

            // Avatar du coach
            Label avatarPlaceholder = new Label("👤");
            avatarPlaceholder.setStyle("-fx-font-size: 30px; -fx-background-color: #e2e8f0; -fx-padding: 5; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-alignment: center;");

            // Informations du coach
            VBox coachDetails = new VBox(4);

            // NOM ET PRÉNOM DU COACH
            Label coachName = new Label(coach.getNom() + " " + coach.getPrenom());
            coachName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            // Email du coach
            Label coachEmail = new Label(coach.getEmail());
            coachEmail.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563eb;");

            // Téléphone si disponible
            if (coach.getTelephone() != null && !coach.getTelephone().isEmpty()) {
                Label coachPhone = new Label("📞 " + coach.getTelephone());
                coachPhone.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                coachDetails.getChildren().addAll(coachName, coachEmail, coachPhone);
            } else {
                coachDetails.getChildren().addAll(coachName, coachEmail);
            }

            coachInfo.getChildren().addAll(avatarPlaceholder, coachDetails);
            coachSection.getChildren().addAll(coachTitle, coachInfo);
        } else {
            // Cas où aucun coach n'est assigné
            Label noCoach = new Label("Aucun coach assigné à cette session");
            noCoach.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444; -fx-font-style: italic; -fx-padding: 8;");
            coachSection.getChildren().addAll(coachTitle, noCoach);
        }

        // Bouton d'inscription
        Button inscriptionButton = new Button("S'inscrire à cette session");
        inscriptionButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand;");
        inscriptionButton.setMaxWidth(Double.MAX_VALUE);
        inscriptionButton.setOnAction(event -> {
            System.out.println("Inscription à la session : " + session.getObjectif());
        });

        // Assemblage de la carte
        card.getChildren().addAll(
                typeBadge,
                dateBox,
                objectifLabel,
                lieuBox,
                coachSection,
                inscriptionButton
        );

        return card;
    }

    private String formatDate(String date) {
        try {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
                int month = Integer.parseInt(parts[1]) - 1;
                return parts[2] + " " + months[month] + " " + parts[0];
            }
        } catch (Exception e) {
            // Ignorer et retourner la date originale
        }
        return date;
    }

    // ======================== COACHS ========================
    private void chargerCoaches() {
        try {
            coachesContainer.getChildren().clear();
            List<Coach> coaches = coachService.afficherAll();
            System.out.println("  - " + coaches.size() + " coaches trouvés");

            for (Coach c : coaches) {
                VBox card = createCoachCard(c);
                coachesContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL coaches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createCoachCard(Coach coach) {
        VBox card = new VBox();
        card.getStyleClass().add("coach-profile-card");
        card.setSpacing(0);
        card.setPadding(new Insets(0));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // ImageView pour l'image dynamique
        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(260);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setStyle("-fx-background-radius: 15 15 0 0;");

        // Charger l'image depuis le chemin stocké en base
        if (coach.getImagecoach() != null && !coach.getImagecoach().isEmpty()) {
            try {
                File file = new File(coach.getImagecoach());
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                } else {
                    String imagePath = "file:" + coach.getImagecoach();
                    Image image = new Image(imagePath, true);
                    imageView.setImage(image);
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement image coach: " + e.getMessage());
                setDefaultCoachImage(imageView);
            }
        } else {
            setDefaultCoachImage(imageView);
        }

        // Contenu texte dynamique
        VBox content = new VBox();
        content.setStyle("-fx-padding: 20; -fx-spacing: 12;");
        content.setAlignment(Pos.CENTER_LEFT);

        Label nomLabel = new Label(coach.getNom() + " " + coach.getPrenom());
        nomLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label emailLabel = new Label(coach.getEmail());
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2563eb;");

        // Badges
        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER_LEFT);

        HBox starBadge = new HBox();
        starBadge.setStyle("-fx-background-color: #fef9c3; -fx-padding: 5 10; -fx-background-radius: 20;");
        starBadge.getChildren().add(new Label("⭐ 4.9"));

        HBox availabilityBadge = new HBox();
        availabilityBadge.setStyle("-fx-background-color: #dcfce7; -fx-padding: 5 10; -fx-background-radius: 20;");
        availabilityBadge.getChildren().add(new Label("✓ Disponible"));

        stats.getChildren().addAll(starBadge, availabilityBadge);

        // Bouton Voir profil
        Button voirProfilBtn = new Button("Voir le profil");
        voirProfilBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 13px; -fx-font-weight: bold;");
        voirProfilBtn.setMaxWidth(Double.MAX_VALUE);
        voirProfilBtn.setOnAction(e -> System.out.println("Voir profil de: " + coach.getNom()));

        content.getChildren().addAll(nomLabel, emailLabel, stats, voirProfilBtn);
        card.getChildren().addAll(imageView, content);

        return card;
    }

    // ======================== IMAGES PAR DÉFAUT ========================
    private void setDefaultCoachImage(ImageView imageView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-coach.png"));
            if (defaultImage != null && !defaultImage.isError()) {
                imageView.setImage(defaultImage);
            } else {
                imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
            }
        } catch (Exception e) {
            imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
        }
    }

    private void setDefaultDomaineImage(ImageView imageView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-domaine.png"));
            if (defaultImage != null && !defaultImage.isError()) {
                imageView.setImage(defaultImage);
            } else {
                imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
            }
        } catch (Exception e) {
            imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
        }
    }
}