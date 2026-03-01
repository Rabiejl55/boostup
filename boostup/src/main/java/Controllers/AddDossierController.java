package Controllers;

import entities.GCandidature.Candidature;
import entities.GCandidature.DossierCandidature;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.DossierCandidatureService;

import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AddDossierController implements Initializable {

    // ── Champs FXML existants ──────────────────────────────────────
    @FXML private TextField             tfNomDossier;
    @FXML private ComboBox<Candidature> cbCandidature;
    @FXML private TextArea              taDescription;
    @FXML private TextField             tfBusinessPlan;
    @FXML private Button                btnParcourir;
    @FXML private DatePicker            dpDateCreation;

    // ── Widgets de progression (optionnels — présents dans le FXML ou non) ──
    @FXML private ProgressBar pbCompletude;
    @FXML private Label       lblCompletude;
    @FXML private Label       lblCountNom;
    @FXML private Label       lblCountDescription;
    @FXML private Label       lblCountBp;

    // ── Constantes de validation ───────────────────────────────────
    private static final int MAX_NOM  = 100;
    private static final int MIN_NOM  = 3;
    private static final int MIN_DESC = 20;
    private static final int MAX_DESC = 3000;
    private static final int MAX_BP   = 255;


    // ── Services ──────────────────────────────────────────────────
    private final DossierCandidatureService service     = new DossierCandidatureService();
    private final CandidatureService        candService = new CandidatureService();
    private boolean saved = false;

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDateCreation.setValue(LocalDate.now());

        // Charger toutes les candidatures (false = sans filtre visible)
        try {
            List<Candidature> cands = candService.getAllCandidatures(false);
            cbCandidature.setItems(FXCollections.observableArrayList(cands));
        } catch (SQLException ex) {
            err("Erreur", "Impossible de charger les candidatures : " + ex.getMessage());
        }

        // Affichage des candidatures dans la ComboBox
        cbCandidature.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Candidature c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "" : c.getNomCandidature() + "  —  " + c.getNomStartup());
            }
        });
        cbCandidature.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Candidature c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Choisissez la candidature…" :
                        c.getNomCandidature() + "  —  " + c.getNomStartup());
            }
        });

        // Compteurs de caractères
        setupCounter(tfNomDossier,   lblCountNom,         MAX_NOM);
        setupCounter(taDescription,  lblCountDescription, MAX_DESC);
        setupCounter(tfBusinessPlan, lblCountBp,          MAX_BP);

        // Listeners validation live + progression
        tfNomDossier.textProperty().addListener((o, old, val) -> {
            if (val != null && val.length() > MAX_NOM) { tfNomDossier.setText(old); return; }
            validateNomLive();
            updateProgress();
        });
        taDescription.textProperty().addListener((o, old, val) -> {
            if (val != null && val.length() > MAX_DESC) { taDescription.setText(old); return; }
            updateProgress();
        });
        tfBusinessPlan.textProperty().addListener((o, old, val) -> {
            if (val != null && val.length() > MAX_BP) { tfBusinessPlan.setText(old); return; }
            validateBpLive();
            updateProgress();
        });
        cbCandidature.valueProperty().addListener((o, a, b)  -> updateProgress());
        dpDateCreation.valueProperty().addListener((o, a, b) -> updateProgress());

        updateProgress();
    }

    // ══════════════════════════════════════════════════════════════
    //  PARCOURIR (inchangé)
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier Business Plan");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers PDF",     "*.pdf"),
                new FileChooser.ExtensionFilter("Documents Word",   "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("Présentations",    "*.pptx"),
                new FileChooser.ExtensionFilter("Tous les fichiers","*.*")
        );
        Stage stage = (Stage) tfBusinessPlan.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            tfBusinessPlan.setText(file.getAbsolutePath());
            tfBusinessPlan.setStyle(
                    "-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROGRESSION (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void updateProgress() {
        int score = 0;
        if (tfNomDossier.getText()    != null && tfNomDossier.getText().trim().length() >= MIN_NOM) score++;
        if (cbCandidature.getValue()  != null)                                                      score++;
        if (tfBusinessPlan.getText()  != null && !tfBusinessPlan.getText().trim().isEmpty())        score++;
        if (dpDateCreation.getValue() != null)                                                      score++;

        double ratio = score / 4.0;
        if (pbCompletude != null) {
            pbCompletude.setProgress(ratio);
            pbCompletude.setStyle(ratio < 0.4 ? "-fx-accent:#e63946;" :
                    ratio < 0.8 ? "-fx-accent:#f59e0b;" : "-fx-accent:#10b981;");
        }
        if (lblCompletude != null) {
            lblCompletude.setText("Complétude : " + (int)(ratio * 100) + "%");
            lblCompletude.setStyle(ratio < 0.4
                    ? "-fx-text-fill:#e63946; -fx-font-weight:bold;"
                    : ratio < 0.8
                    ? "-fx-text-fill:#f59e0b; -fx-font-weight:bold;"
                    : "-fx-text-fill:#10b981; -fx-font-weight:bold;");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  VALIDATION LIVE (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void validateNomLive() {
        String val = tfNomDossier.getText();
        if (val == null || val.trim().isEmpty())
            tfNomDossier.setStyle("");
        else if (val.trim().length() < MIN_NOM)
            tfNomDossier.setStyle(
                    "-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
        else
            tfNomDossier.setStyle(
                    "-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
    }

    private void validateBpLive() {
        String val = tfBusinessPlan.getText();
        if (val == null || val.trim().isEmpty()) { tfBusinessPlan.setStyle(""); return; }
        boolean ok = new File(val).exists()
                || val.startsWith("http://") || val.startsWith("https://")
                || val.matches("(?i).*\\.(pdf|docx|doc|pptx|xlsx)$");
        tfBusinessPlan.setStyle(ok
                ? "-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;"
                : "-fx-border-color:#f59e0b; -fx-border-width:2; -fx-border-radius:8;");
    }

    // ══════════════════════════════════════════════════════════════
    //  COMPTEURS (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void setupCounter(TextField tf, Label lbl, int max) {
        if (lbl == null) return;
        lbl.setText("0/" + max);
        tf.textProperty().addListener((o, a, b) -> {
            int len = b == null ? 0 : b.length();
            lbl.setText(len + "/" + max);
            lbl.setStyle(len == 0       ? "-fx-text-fill:#9ca3af;" :
                    len < max / 2  ? "-fx-text-fill:#f59e0b;" :
                            "-fx-text-fill:#10b981;");
        });
    }

    private void setupCounter(TextArea ta, Label lbl, int max) {
        if (lbl == null) return;
        lbl.setText("0/" + max);
        ta.textProperty().addListener((o, a, b) -> {
            int len = b == null ? 0 : b.length();
            lbl.setText(len + "/" + max);
            lbl.setStyle(len == 0       ? "-fx-text-fill:#9ca3af;" :
                    len < max / 2  ? "-fx-text-fill:#f59e0b;" :
                            "-fx-text-fill:#10b981;");
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  SAUVEGARDE (inchangé)
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void save() {
        String nom  = tfNomDossier.getText()   == null ? "" : tfNomDossier.getText().trim();
        String desc = taDescription.getText()  == null ? "" : taDescription.getText().trim();
        String bp   = tfBusinessPlan.getText() == null ? "" : tfBusinessPlan.getText().trim();

        // Champs obligatoires
        if (nom.isEmpty())                    { highlight(tfNomDossier);   err("Champ obligatoire", "Le nom du dossier est requis."); return; }
        if (cbCandidature.getValue() == null) {                             err("Champ obligatoire", "Veuillez sélectionner une candidature."); return; }
        if (bp.isEmpty())                     { highlight(tfBusinessPlan); err("Champ obligatoire", "Le Business Plan est requis (fichier PDF ou URL)."); return; }
        if (dpDateCreation.getValue() == null){                             err("Champ obligatoire", "La date de création est requise."); return; }

        // Longueur minimale nom
        if (nom.length() < MIN_NOM) {
            highlight(tfNomDossier);
            err("Champ invalide", "Le nom doit contenir au moins " + MIN_NOM + " caractères.");
            return;
        }

        // Validation Business Plan
        boolean isFile = new File(bp).exists();
        boolean isUrl  = bp.startsWith("http://") || bp.startsWith("https://");
        boolean isExt  = bp.matches("(?i).*\\.(pdf|docx|doc|pptx|xlsx)$");
        if (!isFile && !isUrl && !isExt) {
            highlight(tfBusinessPlan);
            err("Business Plan invalide",
                    "Choisissez un fichier via 'Parcourir' ou entrez une URL valide (http/https).");
            return;
        }

        // Unicité du nom
        try {
            if (service.existsNomDossier(nom, 0)) {
                highlight(tfNomDossier);
                err("Nom déjà utilisé", "Un dossier avec ce nom existe déjà.");
                return;
            }
        } catch (SQLException ex) {
            err("Erreur", "Impossible de vérifier l'unicité : " + ex.getMessage());
            return;
        }

        // Construction de l'entité
        DossierCandidature d = new DossierCandidature();
        d.setNomDossier(nom);
        d.setIdCandidature(cbCandidature.getValue().getIdCandidature());
        d.setDescriptionProjet(desc);
        d.setBusinessPlan(bp);
        d.setDateCreation(Date.valueOf(dpDateCreation.getValue()));
        d.setEtat("INCOMPLET");
        d.setVisible(true);

        try {
            service.addDossier(d);
            saved = true;
            ((Stage) tfNomDossier.getScene().getWindow()).close();
        } catch (SQLException ex) {
            err("Erreur sauvegarde", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void close() {
        ((Stage) tfNomDossier.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void highlight(TextField tf) {
        tf.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
        tf.textProperty().addListener((o, a, b) -> tf.setStyle(""));
    }

    private void err(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}