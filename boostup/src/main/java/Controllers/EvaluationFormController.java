package Controllers;

import entities.GCandidature.Candidature;
import entities.GCandidature.Evaluation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.EvaluationService;
import utils.AlertUtils;

import java.sql.SQLException;

public class EvaluationFormController {

    @FXML private ComboBox<Candidature> candidatureComboBox;
    @FXML private TextField             nomEvaluationField;
    @FXML private TextField             noteInnovationField;
    @FXML private TextField             noteViabiliteField;
    @FXML private TextField             noteMarcheField;
    @FXML private TextField             noteEquipeField;
    @FXML private TextField             noteGlobaleField;   // lecture seule
    @FXML private TextField             decisionField;      // lecture seule

    // Indicateur coloré global (fx:id à ajouter dans le FXML)
    @FXML private Label lblIndicateur;

    // Labels d'erreur sous chaque note (fx:id à ajouter dans le FXML)
    @FXML private Label lblErrInnovation;
    @FXML private Label lblErrViabilite;
    @FXML private Label lblErrMarche;
    @FXML private Label lblErrEquipe;

    private Evaluation evaluation;
    private EvaluationBackofficeController parentController;

    private final EvaluationService  service           = new EvaluationService();
    private final CandidatureService candidatureService = new CandidatureService();
    private final ObservableList<Candidature> candidatures = FXCollections.observableArrayList();

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

        // Note globale et décision en lecture seule
        noteGlobaleField.setEditable(false);
        decisionField.setEditable(false);

        // Setup des champs de notes : float entre 0 et 10
        setupNoteField(noteInnovationField, lblErrInnovation);
        setupNoteField(noteViabiliteField,  lblErrViabilite);
        setupNoteField(noteMarcheField,     lblErrMarche);
        setupNoteField(noteEquipeField,     lblErrEquipe);

        // Recalcul automatique à chaque saisie
        noteInnovationField.textProperty().addListener((o, a, b) -> updateAutoFields());
        noteViabiliteField .textProperty().addListener((o, a, b) -> updateAutoFields());
        noteMarcheField    .textProperty().addListener((o, a, b) -> updateAutoFields());
        noteEquipeField    .textProperty().addListener((o, a, b) -> updateAutoFields());

        updateIndicateur(null);
    }

    // ── Setup note : accepte float 0.0 → 10.0, bloque les lettres ────────────
    private void setupNoteField(TextField tf, Label errLbl) {
        tf.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                tf.setStyle("");
                setErrLabel(errLbl, "");
                return;
            }
            // Autoriser uniquement chiffres, point/virgule (saisie en cours)
            if (!val.matches("[0-9]*[.,]?[0-9]*")) {
                tf.setText(old);
                return;
            }
            // Remplacer virgule par point pour le parsing
            String normalized = val.replace(",", ".");
            try {
                double note = Double.parseDouble(normalized);
                if (note < 0 || note > 10) {
                    tf.setText(old);
                    setErrLabel(errLbl, "⚠ Entre 0 et 10");
                } else {
                    // Bordure verte ≥ 7.5, rouge < 7.5
                    if (note >= 7.5)
                        tf.setStyle("-fx-border-color:#10b981; -fx-border-width:2; -fx-border-radius:8;");
                    else
                        tf.setStyle("-fx-border-color:#e63946; -fx-border-width:2; -fx-border-radius:8;");
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

    // ── Recalcul note globale + décision + indicateur ─────────────────────────
    private void updateAutoFields() {
        Double inn  = parseFloat(noteInnovationField.getText());
        Double viab = parseFloat(noteViabiliteField.getText());
        Double mar  = parseFloat(noteMarcheField.getText());
        Double eq   = parseFloat(noteEquipeField.getText());

        // Mettre à jour l'entité Evaluation
        evaluation.setNoteInnovation(inn  != null ? inn.intValue()  : null);
        evaluation.setNoteViabilite(viab  != null ? viab.intValue() : null);
        evaluation.setNoteMarche(mar      != null ? mar.intValue()  : null);
        evaluation.setNoteEquipe(eq       != null ? eq.intValue()   : null);

        // Calcul manuel de la moyenne avec les valeurs float réelles
        double sum = 0; int count = 0;
        if (inn  != null) { sum += inn;  count++; }
        if (viab != null) { sum += viab; count++; }
        if (mar  != null) { sum += mar;  count++; }
        if (eq   != null) { sum += eq;   count++; }

        if (count > 0) {
            double globale = sum / count;
            evaluation.setNoteGlobale(globale);
            String decision = globale >= 7.5 ? "ACCEPTEE" : "REFUSEE";
            evaluation.setDecision(decision);
            noteGlobaleField.setText(String.format("%.2f", globale));
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

    // ── Indicateur coloré global ───────────────────────────────────────────────
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

    // ── Parse float robuste (accepte virgule ou point) ────────────────────────
    private Double parseFloat(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.parseDouble(s.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

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
                    candidatureComboBox.setValue(c); break;
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

    @FXML
    private void saveEvaluation() {
        // ── 1. Tous les champs obligatoires ──────────────────────────────────
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
                || noteMarcheField.getText().isEmpty()     || noteEquipeField.getText().isEmpty()) {
            AlertUtils.showError("Champs obligatoires", "Toutes les notes doivent être renseignées.");
            return;
        }

        // ── 2. Validité des notes (0-10) ──────────────────────────────────────
        if (!isNoteValid(noteInnovationField) || !isNoteValid(noteViabiliteField)
                || !isNoteValid(noteMarcheField)     || !isNoteValid(noteEquipeField)) {
            AlertUtils.showError("Notes invalides", "Toutes les notes doivent être des nombres entre 0 et 10.");
            return;
        }

        // ── 3. Unicité nomEvaluation ──────────────────────────────────────────
        try {
            if (service.existsNomEvaluation(nomEval, evaluation.getIdEvaluation())) {
                AlertUtils.showError("Nom déjà utilisé", "Une évaluation avec ce nom existe déjà.");
                return;
            }
        } catch (SQLException ex) {
            AlertUtils.showError("Erreur", "Impossible de vérifier l'unicité : " + ex.getMessage());
            return;
        }

        // ── 4. Sauvegarde ─────────────────────────────────────────────────────
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

    @FXML private void closeForm() {
        ((Stage) noteInnovationField.getScene().getWindow()).close();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}