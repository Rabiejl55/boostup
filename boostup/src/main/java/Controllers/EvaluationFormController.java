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
    @FXML private TextField nomEvaluationField;
    @FXML private TextField noteInnovationField;
    @FXML private TextField noteViabiliteField;
    @FXML private TextField noteMarcheField;
    @FXML private TextField noteEquipeField;
    @FXML private TextField noteGlobaleField;
    @FXML private TextField decisionField;

    private Evaluation evaluation;
    private EvaluationBackofficeController parentController;
    private final EvaluationService service = new EvaluationService();
    private final CandidatureService candidatureService = new CandidatureService();
    private final ObservableList<Candidature> candidatures = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Charger toutes les candidatures dans le ComboBox
        loadCandidatures();

        // Afficher uniquement le nomCandidature dans la liste déroulante
        candidatureComboBox.setCellFactory(param -> new ListCell<Candidature>() {
            @Override
            protected void updateItem(Candidature item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNomCandidature());
            }
        });

        // Même chose pour le champ sélectionné (affichage du nom)
        candidatureComboBox.setButtonCell(new ListCell<Candidature>() {
            @Override
            protected void updateItem(Candidature item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez une candidature" : item.getNomCandidature());
            }
        });

        // Listeners pour recalcul auto de la note globale et décision
        noteInnovationField.textProperty().addListener((obs, old, newVal) -> updateAutoFields());
        noteViabiliteField.textProperty().addListener((obs, old, newVal) -> updateAutoFields());
        noteMarcheField.textProperty().addListener((obs, old, newVal) -> updateAutoFields());
        noteEquipeField.textProperty().addListener((obs, old, newVal) -> updateAutoFields());
    }

    private void loadCandidatures() {
        try {
            candidatures.clear();
            candidatures.addAll(candidatureService.getAllCandidatures(false)); // false = toutes, même cachées
            candidatureComboBox.setItems(candidatures);
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Impossible de charger les candidatures disponibles");
            e.printStackTrace();
        }
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = (evaluation != null) ? evaluation : new Evaluation();
        loadData();
    }

    public void setParentController(EvaluationBackofficeController parent) {
        this.parentController = parent;
    }

    private void loadData() {
        // Pré-sélectionner la candidature si on édite une évaluation existante
        if (evaluation.getIdCandidature() > 0) {
            for (Candidature c : candidatures) {
                if (c.getIdCandidature() == evaluation.getIdCandidature()) {
                    candidatureComboBox.setValue(c);
                    break;
                }
            }
        }
        nomEvaluationField.setText(evaluation.getNomEvaluation() != null ? evaluation.getNomEvaluation() : "");
        noteInnovationField.setText(evaluation.getNoteInnovation() != null ? String.valueOf(evaluation.getNoteInnovation()) : "");
        noteViabiliteField.setText(evaluation.getNoteViabilite() != null ? String.valueOf(evaluation.getNoteViabilite()) : "");
        noteMarcheField.setText(evaluation.getNoteMarche() != null ? String.valueOf(evaluation.getNoteMarche()) : "");
        noteEquipeField.setText(evaluation.getNoteEquipe() != null ? String.valueOf(evaluation.getNoteEquipe()) : "");
        updateAutoFields();
    }

    private void updateAutoFields() {
        try {
            evaluation.setNoteInnovation(noteInnovationField.getText().isEmpty() ? null : Integer.parseInt(noteInnovationField.getText()));
            evaluation.setNoteViabilite(noteViabiliteField.getText().isEmpty() ? null : Integer.parseInt(noteViabiliteField.getText()));
            evaluation.setNoteMarche(noteMarcheField.getText().isEmpty() ? null : Integer.parseInt(noteMarcheField.getText()));
            evaluation.setNoteEquipe(noteEquipeField.getText().isEmpty() ? null : Integer.parseInt(noteEquipeField.getText()));
            noteGlobaleField.setText(evaluation.getNoteGlobale() != null ? String.valueOf(evaluation.getNoteGlobale()) : "");
            decisionField.setText(evaluation.getDecision());
        } catch (NumberFormatException ignored) {
            // Ignorer si non numérique
        }
    }

    @FXML
    private void saveEvaluation() {
        try {
            // 1. Vérifier qu'une candidature est sélectionnée
            Candidature selected = candidatureComboBox.getValue();
            if (selected == null) {
                AlertUtils.showError("Champ obligatoire", "Veuillez sélectionner une candidature");
                return;
            }

            // 2. Vérifier le nom de l'évaluation (si tu veux le rendre obligatoire)
            String nomEval = nomEvaluationField.getText().trim();
            if (nomEval.isEmpty()) {
                AlertUtils.showError("Champ obligatoire", "Veuillez entrer un nom pour cette évaluation");
                return;
            }
            evaluation.setNomEvaluation(nomEval);

            // 3. Récupérer l'ID réel de la candidature sélectionnée
            evaluation.setIdCandidature(selected.getIdCandidature());

            // 4. Sauvegarde (ajout ou modification)
            if (evaluation.getIdEvaluation() == 0) {
                service.addEvaluation(evaluation);
                AlertUtils.showInfo("Succès", "Évaluation ajoutée avec succès");
            } else {
                service.updateEvaluation(evaluation);
                AlertUtils.showInfo("Succès", "Évaluation modifiée avec succès");
            }

            // 5. Rafraîchir la table des évaluations
            if (parentController != null) {
                parentController.refreshTable();
            }

            // 6. Fermer la fenêtre
            closeForm();

        } catch (NumberFormatException e) {
            AlertUtils.showError("Format invalide", "Veuillez vérifier les notes saisies (doivent être des nombres entiers)");
        } catch (SQLException e) {
            AlertUtils.showError("Erreur base de données", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtils.showError("Erreur inattendue", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void closeForm() {
        Stage stage = (Stage) noteInnovationField.getScene().getWindow();
        stage.close();
    }
}