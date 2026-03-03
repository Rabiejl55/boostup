package controllers;

import entities.GEvenement.EvenementFX;
import entities.GEvenement.Feedback;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import services.EvenementService.FeedbackService;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * Modal Feedback (Front): ajoute un feedback en BD pour l'événement courant.
 * Stratégie: crée une participation "anonyme" (startup/investisseur = 'Front User') si nécessaire,
 * car la table feedback référence id_participation.
 */
public class FeedbackFrontModalController implements Initializable {

    @FXML private Label lblEventTitle;
    @FXML private TextArea taCommentaire;
    @FXML private ChoiceBox<Integer> cbNote;
    @FXML private Label lblError;

    private final FeedbackService fs = new FeedbackService();

    private Runnable onClose;
    private EvenementFX evenement;

    public FeedbackFrontModalController() throws SQLException {
    }

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
        if (cbNote != null) {
            cbNote.getItems().setAll(1, 2, 3, 4, 5);
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        try {
            int idParticipation = ensureParticipationForEvent(evenement.getId());

            Feedback f = new Feedback(
                    taCommentaire.getText().trim(),
                    cbNote.getValue(),
                    new Date(System.currentTimeMillis()),
                    idParticipation
            );

            fs.ajouter(f);

            showSystemInfo("Merci ! Votre feedback a été envoyé avec succès.");

            PauseTransition pt = new PauseTransition(Duration.millis(450));
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

        String c = taCommentaire == null ? "" : taCommentaire.getText().trim();
        if (c.isEmpty()) {
            showError("Veuillez écrire un commentaire.");
            return false;
        }
        if (c.length() < 3) {
            showError("Le commentaire doit contenir au moins 3 caractères.");
            return false;
        }

        if (cbNote == null || cbNote.getValue() == null) {
            showError("Veuillez sélectionner une note (1 à 5).");
            return false;
        }

        int note = cbNote.getValue();
        if (note < 1 || note > 5) {
            showError("Note invalide. Choisissez entre 1 et 5.");
            return false;
        }

        return true;
    }

    /**
     * Trouve une participation existante pour cet événement, sinon en crée une "Front User".
     */
    private int ensureParticipationForEvent(int idEvenement) throws SQLException {
        Connection conn = utils.MyDatabase.getInstance().getConnection();

        // 1) prendre une participation existante
        String find = "SELECT id_participation FROM participation WHERE id_evenement = ? ORDER BY id_participation DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // 2) créer une participation
        String insert = "INSERT INTO participation (nom_startup, nom_investisseur, presence, date_inscription, id_evenement) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Front User");
            ps.setString(2, "Front User");
            ps.setBoolean(3, true);
            ps.setDate(4, new Date(System.currentTimeMillis()));
            ps.setInt(5, idEvenement);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        // fallback: relire
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        throw new SQLException("Impossible de créer une participation pour ce feedback.");
    }

    private void showError(String msg) {
        if (lblError != null) {
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
        alert.setTitle("Feedback");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

