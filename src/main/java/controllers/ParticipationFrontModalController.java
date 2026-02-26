package controllers;

import entities.GEvenement.Evenement;
import entities.GEvenement.EvenementFX;
import entities.GEvenement.Participation;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import services.EvenementService.ParticipationService;
import services.EmailService;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Modal d'inscription (front) -> insertion dans table participation.
 */
public class ParticipationFrontModalController implements Initializable {

    @FXML private Label lblEventTitle;
    @FXML private TextField tfStartup;
    @FXML private TextField tfInvestisseur;
    @FXML private ChoiceBox<String> cbPresence;
    @FXML private Label lblError;

    private final ParticipationService ps = new ParticipationService();

    private Runnable onClose;
    private EvenementFX evenement;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setEvenement(EvenementFX evenement) {
        this.evenement = evenement;
        if (lblEventTitle != null && evenement != null) {
            lblEventTitle.setText("Événement: " + (evenement.getTitre() == null ? "" : evenement.getTitre()));
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (cbPresence != null) {
            cbPresence.getItems().setAll("Oui, je veux assister", "Peut-être");
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        // 🎮 CHALLENGE POUR ÉVÉNEMENT COMPLET (capacité 250)
        if (evenement != null && evenement.getCapaciteMax() >= 250) {
            System.out.println("🎮 Événement complet détecté ! Lancement du challenge Snake...");

            // Lancer le jeu Snake
            utils.SnakeGameChallenge challenge = new utils.SnakeGameChallenge();
            javafx.stage.Stage ownerStage = (javafx.stage.Stage) tfStartup.getScene().getWindow();

            boolean won = challenge.showChallenge(ownerStage);

            if (!won) {
                // L'utilisateur a perdu ou annulé
                System.out.println("❌ Challenge échoué ou annulé");
                return;
            }

            // L'utilisateur a gagné ! On continue l'inscription
            System.out.println("🏆 Challenge réussi ! Inscription autorisée malgré l'événement complet");
        }

        boolean presence = "Oui, je veux assister".equals(cbPresence.getValue());
        Date today = new Date(System.currentTimeMillis());

        Participation p = new Participation(
                tfStartup.getText().trim(),
                tfInvestisseur.getText().trim(),
                presence,
                today,
                evenement.getId()
        );

        try {
            ps.ajouter(p);

            // 📧 ENVOI AUTOMATIQUE DE L'EMAIL DE BIENVENUE
            System.out.println("✅ Participation ajoutée, envoi de l'email de bienvenue...");

            // Conversion EvenementFX -> Evenement pour EmailService
            Evenement eventForEmail = new Evenement();
            eventForEmail.setTitre(evenement.getTitre());
            eventForEmail.setType(evenement.getType());
            eventForEmail.setDateEvenement(evenement.getDateEvenement());
            eventForEmail.setLieu(evenement.getLieu());
            eventForEmail.setDescription(evenement.getDescription());
            eventForEmail.setCapaciteMax(evenement.getCapaciteMax());

            // Nom utilisateur = nom startup + investisseur
            String nomUtilisateur = tfStartup.getText().trim() + " / " + tfInvestisseur.getText().trim();

            // Envoi email (thread asynchrone, ne bloque pas l'UI)
            EmailService.envoyerEmailBienvenue(eventForEmail, nomUtilisateur, null);

            // Message de félicitations (alerte système)
            String dateEv = evenement.getDateEvenement() == null ? "-" : evenement.getDateEvenement().toString();
            String lieuEv = (evenement.getLieu() == null || evenement.getLieu().trim().isEmpty()) ? "(lieu non précisé)" : evenement.getLieu().trim();

            showSystemInfo(
                    "🎉 Félicitations !\n\nOn se voit le " + dateEv + " au " + lieuEv + " inchallah 🙂👋\n\n" +
                    "📧 Un email de confirmation a été envoyé à rayen.amri@esprit.tn"
            );

            // Fermer après 600ms (laisse le temps au système de rendre l'UI). L'alerte est modale.
            PauseTransition pt = new PauseTransition(Duration.millis(600));
            pt.setOnFinished(ev -> close());
            pt.play();

        } catch (SQLException e) {
            showError("Erreur SQL: " + e.getMessage());
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    private boolean validate() {
        clearError();

        if (evenement == null) {
            showError("Événement introuvable.");
            return false;
        }

        String s1 = tfStartup == null ? "" : tfStartup.getText().trim();
        String s2 = tfInvestisseur == null ? "" : tfInvestisseur.getText().trim();

        if (s1.isEmpty() || s2.isEmpty()) {
            showError("Tous les champs doivent être remplis.");
            return false;
        }

        // règles simples: longueur min 3
        if (s1.length() < 3) {
            showError("Le nom de la startup doit contenir au moins 3 caractères.");
            return false;
        }
        if (s2.length() < 3) {
            showError("Le nom de l’investisseur doit contenir au moins 3 caractères.");
            return false;
        }

        if (cbPresence == null || cbPresence.getValue() == null) {
            showError("Veuillez sélectionner une option (Oui / Peut-être). ");
            return false;
        }

        return true;
    }

    private void showError(String msg) {
        if (lblError != null) {
            lblError.setStyle("-fx-text-fill: #ffb4b4; -fx-font-weight: 900;");
            lblError.setText(msg);
        }
    }

    private void clearError() {
        if (lblError != null) lblError.setText("");
    }

    private void close() {
        if (onClose != null) onClose.run();
    }

    private void showSystemInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Inscription confirmée");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
