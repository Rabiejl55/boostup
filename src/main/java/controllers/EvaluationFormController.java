package controllers;

import entities.GCandidature.Candidature;
import entities.GCandidature.Evaluation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.DossierCandidatureService;
import services.CandidatureService.EvaluationService;
import utils.AlertUtils;

import java.sql.SQLException;

public class EvaluationFormController {

    // ── Champs FXML existants ──────────────────────────────────────
    @FXML private ComboBox<Candidature> candidatureComboBox;
    @FXML private TextField             nomEvaluationField;
    @FXML private TextField             noteInnovationField;
    @FXML private TextField             noteViabiliteField;
    @FXML private TextField             noteMarcheField;
    @FXML private TextField             noteEquipeField;
    @FXML private TextField             noteGlobaleField;   // lecture seule
    @FXML private TextField             decisionField;      // lecture seule
    @FXML private Label                 lblIndicateur;

    // Labels d'erreur sous chaque note
    @FXML private Label lblErrInnovation;
    @FXML private Label lblErrViabilite;
    @FXML private Label lblErrMarche;
    @FXML private Label lblErrEquipe;

    // ✨ Bouton IA (nouveau)
    @FXML private Button btnAnalyseIA;

    // ── Services ──────────────────────────────────────────────────
    private Evaluation evaluation;
    private EvaluationBackofficeController parentController;

    private final EvaluationService          service           = new EvaluationService();
    private final CandidatureService         candidatureService = new CandidatureService();
    private final DossierCandidatureService  dossierService    = new DossierCandidatureService();
    private final ObservableList<Candidature> candidatures     = FXCollections.observableArrayList();

    public EvaluationFormController() throws SQLException {
    }

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        loadCandidatures();

        candidatureComboBox.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Candidature item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNomCandidature());
            }
        });
        candidatureComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Candidature item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez une candidature" : item.getNomCandidature());
            }
        });

        noteGlobaleField.setEditable(false);
        decisionField.setEditable(false);

        setupNoteField(noteInnovationField, lblErrInnovation);
        setupNoteField(noteViabiliteField,  lblErrViabilite);
        setupNoteField(noteMarcheField,     lblErrMarche);
        setupNoteField(noteEquipeField,     lblErrEquipe);

        noteInnovationField.textProperty().addListener((o, a, b) -> updateAutoFields());
        noteViabiliteField .textProperty().addListener((o, a, b) -> updateAutoFields());
        noteMarcheField    .textProperty().addListener((o, a, b) -> updateAutoFields());
        noteEquipeField    .textProperty().addListener((o, a, b) -> updateAutoFields());

        updateIndicateur(null);
    }

    // ══════════════════════════════════════════════════════════════
    //  🤖 INTÉGRATION IA — NOUVEAU
    // ══════════════════════════════════════════════════════════════

    /**
     * Ouvre le panneau AnalyseIA en modal.
     * Quand l'utilisateur clique "Appliquer les notes", les 4 champs
     * de notes sont pré-remplis automatiquement.
     */
    @FXML
    private void ouvrirAnalyseIA() {
        // Une candidature doit être sélectionnée pour contextualiser l'analyse
        if (candidatureComboBox.getValue() == null) {
            AlertUtils.showError("Candidature requise",
                    "Veuillez d'abord sélectionner une candidature pour lancer l'analyse IA.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AnalyseIAPanel.fxml"));
            Parent root = loader.load();
            AnalyseIAPanelController iaCtrl = loader.getController();

            Candidature cand = candidatureComboBox.getValue();

            // ── Récupérer la description du dossier lié à cette candidature ──
            String description  = null;
            String businessPlan = null;
            try {
                var dossiers = dossierService.getAllDossiers(false);
                for (var d : dossiers) {
                    if (d.getIdCandidature() == cand.getIdCandidature()) {
                        description  = d.getDescriptionProjet();
                        businessPlan = d.getBusinessPlan();
                        break; // on prend le premier dossier trouvé
                    }
                }
            } catch (SQLException ex) {
                // Pas bloquant — l'IA analysera avec les infos disponibles
            }

            // ── Pré-remplir le contexte IA ──
            iaCtrl.setContexte(
                    cand.getNomStartup(),   // nom de la startup
                    description,            // description du projet (depuis le dossier)
                    businessPlan            // business plan (depuis le dossier)
            );

            // ── Callback : les notes suggérées sont reportées dans les champs ──
            iaCtrl.setOnNotesApplied(notes -> {
                // notes = [innovation, viabilité, marché, équipe]
                if (!notes[0].equals("—")) {
                    noteInnovationField.setText(notes[0]);
                }
                if (!notes[1].equals("—")) {
                    noteViabiliteField.setText(notes[1]);
                }
                if (!notes[2].equals("—")) {
                    noteMarcheField.setText(notes[2]);
                }
                if (!notes[3].equals("—")) {
                    noteEquipeField.setText(notes[3]);
                }
                // Feedback visuel sur le bouton IA
                btnAnalyseIA.setText("✅  Notes appliquées");
                btnAnalyseIA.setDisable(true);
            });

            // ── Ouvrir en modal ──
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(nomEvaluationField.getScene().getWindow());
            stage.setTitle("🤖 Analyse IA  —  " + cand.getNomStartup());
            stage.setScene(new Scene(root));
            stage.setMinWidth(660);
            stage.setMinHeight(520);
            stage.show();

        } catch (Exception e) {
            AlertUtils.showError("Erreur IA",
                    "Impossible d'ouvrir le panneau IA :\n" + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  VALIDATION LIVE (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void setupNoteField(TextField tf, Label errLbl) {
        tf.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                tf.setStyle("");
                setErrLabel(errLbl, "");
                return;
            }
            if (!val.matches("[0-9]*[.,]?[0-9]*")) {
                tf.setText(old);
                return;
            }
            String normalized = val.replace(",", ".");
            try {
                double note = Double.parseDouble(normalized);
                if (note < 0 || note > 10) {
                    tf.setText(old);
                    setErrLabel(errLbl, "⚠ Entre 0 et 10");
                } else {
                    tf.setStyle(note >= 7.5
                            ? "-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;"
                            : "-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
                    setErrLabel(errLbl, "");
                }
            } catch (NumberFormatException ignored) {
                // Saisie en cours (ex: "7.") → on laisse passer
            }
        });
    }

    private void setErrLabel(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setStyle(msg.isEmpty() ? "" : "-fx-text-fill:#e63946; -fx-font-size:11px;");
    }

    // ══════════════════════════════════════════════════════════════
    //  CALCUL AUTO NOTE GLOBALE (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void updateAutoFields() {
        Double inn  = parseFloat(noteInnovationField.getText());
        Double viab = parseFloat(noteViabiliteField.getText());
        Double mar  = parseFloat(noteMarcheField.getText());
        Double eq   = parseFloat(noteEquipeField.getText());

        if (evaluation != null) {
            evaluation.setNoteInnovation(inn  != null ? inn.intValue()  : null);
            evaluation.setNoteViabilite(viab  != null ? viab.intValue() : null);
            evaluation.setNoteMarche(mar      != null ? mar.intValue()  : null);
            evaluation.setNoteEquipe(eq       != null ? eq.intValue()   : null);
        }

        double sum = 0; int count = 0;
        if (inn  != null) { sum += inn;  count++; }
        if (viab != null) { sum += viab; count++; }
        if (mar  != null) { sum += mar;  count++; }
        if (eq   != null) { sum += eq;   count++; }

        if (count > 0) {
            double globale = sum / count;
            if (evaluation != null) {
                evaluation.setNoteGlobale(globale);
                evaluation.setDecision(globale >= 7.5 ? "ACCEPTEE" : "REFUSEE");
            }
            noteGlobaleField.setText(String.format("%.2f", globale));
            String decision = globale >= 7.5 ? "ACCEPTEE" : "REFUSEE";
            decisionField.setText(decision);
            styleDecision(decision);
            updateIndicateur(globale);
        } else {
            noteGlobaleField.setText("");
            decisionField.setText("");
            decisionField.setStyle("");
            updateIndicateur(null);
        }
    }

    private void updateIndicateur(Double note) {
        if (lblIndicateur == null) return;
        if (note == null) {
            lblIndicateur.setText("⬜  Saisissez les notes pour voir l'évaluation");
            lblIndicateur.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:13px; -fx-font-weight:bold;");
        } else if (note >= 9) {
            lblIndicateur.setText("🟢  Excellente candidature !");
            lblIndicateur.setStyle("-fx-text-fill:#10b981; -fx-font-size:13px; -fx-font-weight:bold;");
        } else if (note >= 7.5) {
            lblIndicateur.setText("🟢  Bonne candidature — ACCEPTÉE");
            lblIndicateur.setStyle("-fx-text-fill:#10b981; -fx-font-size:13px; -fx-font-weight:bold;");
        } else {
            lblIndicateur.setText("🔴  Candidature insuffisante — REFUSÉE");
            lblIndicateur.setStyle("-fx-text-fill:#e63946; -fx-font-size:13px; -fx-font-weight:bold;");
        }
    }

    private void styleDecision(String decision) {
        if (decisionField == null) return;
        decisionField.setStyle("ACCEPTEE".equals(decision)
                ? "-fx-text-fill:#10b981; -fx-font-weight:bold;"
                : "-fx-text-fill:#e63946; -fx-font-weight:bold;");
    }

    private Double parseFloat(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.parseDouble(s.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    // ══════════════════════════════════════════════════════════════
    //  CHARGEMENT DONNÉES (inchangé)
    // ══════════════════════════════════════════════════════════════

    private void loadCandidatures() {
        try {
            candidatures.clear();
            candidatures.addAll(candidatureService.getAllCandidatures(false));
            candidatureComboBox.setItems(candidatures);
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Impossible de charger les candidatures.");
        }
    }

    public void setEvaluation(Evaluation ev) {
        this.evaluation = (ev != null) ? ev : new Evaluation();
        loadData();
    }

    public void setParentController(EvaluationBackofficeController parent) {
        this.parentController = parent;
    }

    private void loadData() {
        if (evaluation.getIdCandidature() > 0) {
            for (Candidature c : candidatures) {
                if (c.getIdCandidature() == evaluation.getIdCandidature()) {
                    candidatureComboBox.setValue(c);
                    break;
                }
            }
        }
        nomEvaluationField .setText(nvl(evaluation.getNomEvaluation()));
        noteInnovationField.setText(evaluation.getNoteInnovation() != null ? String.valueOf(evaluation.getNoteInnovation()) : "");
        noteViabiliteField .setText(evaluation.getNoteViabilite()  != null ? String.valueOf(evaluation.getNoteViabilite())  : "");
        noteMarcheField    .setText(evaluation.getNoteMarche()     != null ? String.valueOf(evaluation.getNoteMarche())     : "");
        noteEquipeField    .setText(evaluation.getNoteEquipe()     != null ? String.valueOf(evaluation.getNoteEquipe())     : "");
        updateAutoFields();
    }

    // ══════════════════════════════════════════════════════════════
    //  SAUVEGARDE (inchangé)
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void saveEvaluation() {
        if (candidatureComboBox.getValue() == null) {
            AlertUtils.showError("Champ obligatoire", "Veuillez sélectionner une candidature.");
            return;
        }
        String nomEval = nomEvaluationField.getText() == null ? "" : nomEvaluationField.getText().trim();
        if (nomEval.isEmpty()) {
            AlertUtils.showError("Champ obligatoire", "Le nom de l'évaluation est requis.");
            return;
        }
        if (noteInnovationField.getText().isEmpty() || noteViabiliteField.getText().isEmpty()
                || noteMarcheField.getText().isEmpty()  || noteEquipeField.getText().isEmpty()) {
            AlertUtils.showError("Champs obligatoires", "Toutes les notes doivent être renseignées.");
            return;
        }
        if (!isNoteValid(noteInnovationField) || !isNoteValid(noteViabiliteField)
                || !isNoteValid(noteMarcheField)     || !isNoteValid(noteEquipeField)) {
            AlertUtils.showError("Notes invalides", "Toutes les notes doivent être des nombres entre 0 et 10.");
            return;
        }

        try {
            if (service.existsNomEvaluation(nomEval, evaluation.getIdEvaluation())) {
                AlertUtils.showError("Nom déjà utilisé", "Une évaluation avec ce nom existe déjà.");
                return;
            }
        } catch (SQLException ex) {
            AlertUtils.showError("Erreur", "Impossible de vérifier l'unicité : " + ex.getMessage());
            return;
        }

        evaluation.setNomEvaluation(nomEval);
        evaluation.setIdCandidature(candidatureComboBox.getValue().getIdCandidature());

        try {
            if (evaluation.getIdEvaluation() == 0) {
                service.addEvaluation(evaluation);
                AlertUtils.showInfo("Succès", "Évaluation ajoutée avec succès.");
            } else {
                service.updateEvaluation(evaluation);
                AlertUtils.showInfo("Succès", "Évaluation modifiée avec succès.");
            }
            if (parentController != null) parentController.refreshTable();
            closeForm();
        } catch (SQLException e) {
            AlertUtils.showError("Erreur base de données", e.getMessage());
        }
    }

    private boolean isNoteValid(TextField tf) {
        Double val = parseFloat(tf.getText());
        return val != null && val >= 0 && val <= 10;
    }

    @FXML
    private void closeForm() {
        ((Stage) noteInnovationField.getScene().getWindow()).close();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}