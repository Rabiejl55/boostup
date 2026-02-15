package controllers;

import entities.GAccompagnement.Domaine;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AccompagnementService.DomaineService;

public class EditDomaineController {

    private Domaine domaine;
    private final DomaineService domaineService = new DomaineService();

    @FXML private TextField nomField;
    @FXML private TextArea descriptionField;
    @FXML private TextField niveauField;
    @FXML private ComboBox<String> statutField;

    @FXML private Label nomError;
    @FXML private Label descriptionError;
    @FXML private Label niveauError;
    @FXML private Label statutError;
    @FXML private Label errorLabel; // pour erreurs globales

    @FXML
    public void initialize() {
        statutField.getItems().addAll("Actif", "Inactif");
    }

    public void setDomaine(Domaine domaine) {
        this.domaine = domaine;
        nomField.setText(domaine.getNom());
        descriptionField.setText(domaine.getDescription());
        niveauField.setText(domaine.getNiveau());
        statutField.setValue(domaine.getStatut());
    }

    @FXML
    private void submitForm() {
        // Réinitialiser erreurs
        nomError.setText("");
        descriptionError.setText("");
        niveauError.setText("");
        statutError.setText("");
        if (errorLabel != null) errorLabel.setText("");

        boolean valid = true;

        // ===== NOM =====
        String nom = nomField.getText().trim();
        if (nom.isEmpty() || !nom.matches("[a-zA-ZÀ-ÿ ]{2,50}")) {
            nomError.setText("Nom invalide (2-50 lettres)");
            valid = false;
        }

        // ===== DESCRIPTION =====
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            descriptionError.setText("Description requise");
            valid = false;
        }

        // ===== NIVEAU =====
        String niveau = niveauField.getText().trim();
        if (niveau.isEmpty()) {
            niveauError.setText("Niveau requis");
            valid = false;
        }

        // ===== STATUT =====
        String statut = statutField.getValue();
        if (statut == null || statut.isEmpty()) {
            statutError.setText("Statut requis");
            valid = false;
        }

        if (!valid) return;

        // ===== Mise à jour =====
        domaine.setNom(nom);
        domaine.setDescription(description);
        domaine.setNiveau(niveau);
        domaine.setStatut(statut);

        try {
            domaineService.modifier(domaine);
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur lors de la modification");
            e.printStackTrace();
        }
    }

    @FXML
    private void closeForm() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}
