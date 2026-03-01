package Controllers;

import entities.GCandidature.Candidature;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AddCandidatureController implements Initializable {

    @FXML private TextField  tfNomCandidature;
    @FXML private TextField  tfNomStartup;
    @FXML private TextField  tfEmail;          // ✅ NOUVEAU
    @FXML private DatePicker dpDate;
    @FXML private TextArea   taComment;

    @FXML private Label lblCountNom;
    @FXML private Label lblCountStartup;
    @FXML private Label lblCountComment;

    private static final int MAX_NOM     = 50;
    private static final int MAX_STARTUP = 50;
    private static final int MAX_COMMENT = 3000;
    private static final int MIN_NOM     = 3;
    private static final int MIN_STARTUP = 2;
    private static final LocalDate MIN_DATE = LocalDate.of(2026, 2, 23);

    private final CandidatureService service = new CandidatureService();
    private boolean saved = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Date par défaut
        dpDate.setValue(MIN_DATE);
        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(MIN_DATE));
                if (date.isBefore(MIN_DATE))
                    setStyle("-fx-background-color:#f5f5f5; -fx-text-fill:#cccccc;");
            }
        });
        dpDate.getEditor().focusedProperty().addListener((obs, was, is) -> {
            if (!is) validateDate();
        });

        // Compteurs live
        setupCounter(tfNomCandidature, lblCountNom,     MAX_NOM);
        setupCounter(tfNomStartup,     lblCountStartup, MAX_STARTUP);
        setupCounter(taComment,        lblCountComment, MAX_COMMENT);

        // Validation live nom candidature
        tfNomCandidature.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > MAX_NOM) { tfNomCandidature.setText(old); return; }
            validateNomLive(tfNomCandidature, val, MIN_NOM);
        });

        // Validation live nom startup
        tfNomStartup.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > MAX_STARTUP) { tfNomStartup.setText(old); return; }
            validateNomLive(tfNomStartup, val, MIN_STARTUP);
        });

        // ✅ Validation live email
        tfEmail.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                tfEmail.setStyle("");
            } else if (val.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
                tfEmail.setStyle("-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
            } else {
                tfEmail.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
            }
        });

        // Limite commentaire
        taComment.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > MAX_COMMENT) taComment.setText(old);
        });
    }

    private void setupCounter(TextField tf, Label lbl, int max) {
        if (lbl == null) return;
        lbl.setText("0/" + max + " caractères");
        tf.textProperty().addListener((obs, old, val) -> {
            int len = val == null ? 0 : val.length();
            lbl.setText(len + "/" + max + " caractères");
            lbl.setStyle(len == 0      ? "-fx-text-fill:#9ca3af;" :
                    len < max / 2 ? "-fx-text-fill:#f59e0b;" :
                            "-fx-text-fill:#10b981;");
        });
    }

    private void setupCounter(TextArea ta, Label lbl, int max) {
        if (lbl == null) return;
        lbl.setText("0/" + max + " caractères");
        ta.textProperty().addListener((obs, old, val) -> {
            int len = val == null ? 0 : val.length();
            lbl.setText(len + "/" + max + " caractères");
            lbl.setStyle(len == 0      ? "-fx-text-fill:#9ca3af;" :
                    len < max / 2 ? "-fx-text-fill:#f59e0b;" :
                            "-fx-text-fill:#10b981;");
        });
    }

    private void validateNomLive(TextField tf, String val, int min) {
        if (val == null || val.trim().isEmpty())
            tf.setStyle("");
        else if (val.trim().length() < min || val.trim().matches("[0-9]+"))
            tf.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
        else
            tf.setStyle("-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
    }

    private void validateDate() {
        try {
            LocalDate parsed = LocalDate.parse(
                    dpDate.getEditor().getText(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            if (parsed.isBefore(MIN_DATE)) {
                dpDate.setValue(MIN_DATE);
                showWarn("Date invalide", "La date ne peut pas être antérieure au 23/02/2026.");
            } else {
                dpDate.setValue(parsed);
            }
        } catch (Exception ignored) {
            dpDate.setValue(MIN_DATE);
        }
    }

    @FXML
    private void save() {
        String nom     = tfNomCandidature.getText() == null ? "" : tfNomCandidature.getText().trim();
        String startup = tfNomStartup.getText()     == null ? "" : tfNomStartup.getText().trim();
        String email   = tfEmail.getText()          == null ? "" : tfEmail.getText().trim(); // ✅
        String comment = taComment.getText()        == null ? "" : taComment.getText().trim();

        // ── Champs obligatoires ───────────────────────────────────
        if (nom.isEmpty()) {
            highlight(tfNomCandidature);
            showWarn("Champ obligatoire", "Le nom de la candidature est requis."); return;
        }
        if (startup.isEmpty()) {
            highlight(tfNomStartup);
            showWarn("Champ obligatoire", "Le nom de la startup est requis."); return;
        }

        // ✅ Email obligatoire + validation format
        if (email.isEmpty()) {
            highlight(tfEmail);
            showWarn("Champ obligatoire", "L'email de contact est requis."); return;
        }
        if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
            highlight(tfEmail);
            showWarn("Email invalide", "Veuillez saisir une adresse email valide.\nEx : contact@startup.com"); return;
        }

        if (comment.isEmpty()) {
            showWarn("Champ obligatoire", "Le commentaire est requis."); return;
        }
        if (dpDate.getValue() == null) {
            showWarn("Champ obligatoire", "La date de dépôt est requise."); return;
        }

        // ── Longueurs minimales ───────────────────────────────────
        if (nom.length() < MIN_NOM) {
            highlight(tfNomCandidature);
            showWarn("Champ invalide", "Le nom doit contenir au moins " + MIN_NOM + " caractères."); return;
        }
        if (nom.matches("[0-9]+")) {
            highlight(tfNomCandidature);
            showWarn("Champ invalide", "Le nom ne peut pas être uniquement des chiffres."); return;
        }
        if (startup.length() < MIN_STARTUP) {
            highlight(tfNomStartup);
            showWarn("Champ invalide", "Le nom de la startup doit contenir au moins " + MIN_STARTUP + " caractères."); return;
        }
        if (dpDate.getValue().isBefore(MIN_DATE)) {
            showWarn("Date invalide", "La date ne peut pas être antérieure au 23/02/2026."); return;
        }

        // ── Unicité nomCandidature ────────────────────────────────
        try {
            if (service.existsNomCandidature(nom, 0)) {
                highlight(tfNomCandidature);
                showWarn("Nom déjà utilisé",
                        "Une candidature avec ce nom existe déjà. Veuillez en choisir un autre."); return;
            }
        } catch (SQLException ex) {
            showWarn("Erreur", "Impossible de vérifier l'unicité : " + ex.getMessage()); return;
        }

        // ── Sauvegarde ────────────────────────────────────────────
        Candidature c = new Candidature();
        c.setNomCandidature(nom);
        c.setNomStartup(startup);
        c.setEmailContact(email);   // ✅ NOUVEAU
        c.setDateDepot(Date.valueOf(dpDate.getValue()));
        c.setStatut("EN_ATTENTE");
        c.setScore(null);
        c.setVisible(true);
        c.setIdStartup(1);
        c.setCommentaire(comment);

        try {
            service.addCandidature(c);
            saved = true;
            ((Stage) tfNomCandidature.getScene().getWindow()).close();
        } catch (Exception ex) {
            showWarn("Erreur de sauvegarde", "Impossible d'enregistrer : " + ex.getMessage());
        }
    }

    @FXML private void close() {
        ((Stage) tfNomCandidature.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void highlight(TextField tf) {
        tf.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
        tf.textProperty().addListener((o, a, b) -> tf.setStyle(""));
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}