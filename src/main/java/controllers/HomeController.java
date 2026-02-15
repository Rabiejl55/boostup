package controllers;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
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

public class HomeController {

    @FXML
    private FlowPane domainesContainer;

    @FXML
    private TilePane planningGrid;

    @FXML
    private FlowPane coachesContainer;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMM");

    @FXML
    public void initialize() {
        chargerDomaines();
        chargerSessions();
        chargerCoaches();
    }

    // ---------------------------
    // Charger les Domaines avec images dynamiques
    // ---------------------------
    private void chargerDomaines() {
        DomaineService service = new DomaineService();
        try {
            domainesContainer.getChildren().clear();
            List<Domaine> domaines = service.afficherAll();
            for (Domaine d : domaines) {
                VBox card = createDomaineCard(d);
                domainesContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des domaines : " + e.getMessage());
        }
    }

    private VBox createDomaineCard(Domaine domaine) {
        VBox card = new VBox();
        card.getStyleClass().add("domain-card");
        card.setSpacing(0);
        card.setPadding(new Insets(0));
        card.setAlignment(Pos.CENTER);

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

        Label desc = new Label(domaine.getDescription());
        desc.getStyleClass().add("domain-description");
        desc.setWrapText(true);
        desc.setMaxWidth(240);

        content.getChildren().addAll(nom, desc);

        // Ajouter l'image et le contenu à la carte
        card.getChildren().addAll(imageView, content);

        return card;
    }

    // ---------------------------
    // Charger les Sessions
    // ---------------------------
    private void chargerSessions() {
        SessionService service = new SessionService();
        try {
            planningGrid.getChildren().clear();
            List<Session> sessions = service.afficherAll();
            for (Session s : sessions) {
                VBox dayCard = createSessionCard(s);
                planningGrid.getChildren().add(dayCard);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des sessions : " + e.getMessage());
        }
    }

    private VBox createSessionCard(Session s) {
        VBox card = new VBox();
        card.getStyleClass().add("planning-day-card");
        card.setSpacing(8);
        card.setPadding(new Insets(10));

        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(s.getDateSession().format(dateFormatter));
        dateLabel.getStyleClass().add("planning-day-name");

        Label dureeLabel = new Label(s.getDuree() + "h");
        dureeLabel.getStyleClass().add("planning-day-number");

        header.getChildren().addAll(dateLabel, dureeLabel);

        VBox content = new VBox(5);
        Label objectifLabel = new Label(s.getObjectif());
        objectifLabel.getStyleClass().add("planning-session-title");
        objectifLabel.setWrapText(true);
        objectifLabel.setMaxWidth(150);
        content.getChildren().add(objectifLabel);

        card.getChildren().addAll(header, content);
        return card;
    }

    // ---------------------------
    // Charger les Coaches avec images dynamiques
    // ---------------------------
    private void chargerCoaches() {
        CoachService service = new CoachService();
        try {
            List<Coach> coaches = service.afficherAll();
            coachesContainer.getChildren().clear();

            for (Coach c : coaches) {
                VBox card = createCoachCard(c);
                coachesContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des coaches : " + e.getMessage());
        }
    }

    private VBox createCoachCard(Coach coach) {
        VBox card = new VBox();
        card.getStyleClass().add("coach-profile-card");
        card.setSpacing(0);
        card.setPadding(new Insets(0));

        // ImageView pour l'image dynamique
        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(260);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.getStyleClass().add("coach-profile-image");

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
        content.getStyleClass().add("coach-profile-content");
        content.setSpacing(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);

        Label nomLabel = new Label(coach.getNom() + " " + coach.getPrenom());
        nomLabel.getStyleClass().add("coach-profile-name");

        Label emailLabel = new Label(coach.getEmail());
        emailLabel.getStyleClass().add("coach-profile-email");

        // Badges statiques
        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER_LEFT);

        HBox starBadge = new HBox();
        starBadge.getStyleClass().add("coach-stat-badge");
        starBadge.getChildren().add(new Label("⭐ 4.9"));

        HBox yearsBadge = new HBox();
        yearsBadge.getStyleClass().add("coach-stat-badge");
        yearsBadge.getChildren().add(new Label("🎓 12 ans"));

        HBox availabilityBadge = new HBox();
        availabilityBadge.getStyleClass().add("availability-badge");
        availabilityBadge.getChildren().add(new Label("Disponible"));

        stats.getChildren().addAll(starBadge, yearsBadge, availabilityBadge);

        content.getChildren().addAll(nomLabel, emailLabel, stats);

        // Ajouter l'imageView et le contenu à la carte
        card.getChildren().addAll(imageView, content);

        return card;
    }

    // Méthode pour définir une image par défaut pour les coaches
    private void setDefaultCoachImage(ImageView imageView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-coach.png"));
            if (defaultImage != null) {
                imageView.setImage(defaultImage);
            } else {
                imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
            }
        } catch (Exception e) {
            imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
        }
    }

    // Méthode pour définir une image par défaut pour les domaines
    private void setDefaultDomaineImage(ImageView imageView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-domaine.png"));
            if (defaultImage != null) {
                imageView.setImage(defaultImage);
            } else {
                imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
            }
        } catch (Exception e) {
            imageView.setStyle("-fx-background-color: linear-gradient(to bottom right, #43A3DB, #2D3E50);");
        }
    }
}