package controllers;

import entities.GAccompagnement.Coach;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.AccompagnementService.CoachService;

import java.io.File;

public class AddCoachController {

    private final CoachService coachService = new CoachService();

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;

    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label telephoneError;
    @FXML private Label errorLabel;
    @FXML private Label imageLabel;

    private File selectedImageFile;

    // ===== CHOISIR IMAGE =====
    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(nomField.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imageLabel.setText(file.getName());
        }
    }

    // ===== FERMER =====
    @FXML
    private void closeForm() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    // ===== AJOUTER =====
    @FXML
    private void submitForm() {

        // ===== RÉINITIALISER LES MESSAGES D'ERREUR =====
        nomError.setText("");
        prenomError.setText("");
        emailError.setText("");
        telephoneError.setText("");
        errorLabel.setText("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telephone = telephoneField.getText().trim();

        boolean valid = true;

        // ===== VALIDATION NOM =====
        if (nom.isEmpty()) {
            nomError.setText("Nom requis");
            valid = false;
        } else if (!nom.matches("[a-zA-Z ]{2,50}")) {
            nomError.setText("Lettres uniquement (2-50 caractères)");
            valid = false;
        }

        // ===== VALIDATION PRÉNOM =====
        if (prenom.isEmpty()) {
            prenomError.setText("Prénom requis");
            valid = false;
        } else if (!prenom.matches("[a-zA-Z ]{2,50}")) {
            prenomError.setText("Lettres uniquement (2-50 caractères)");
            valid = false;
        }

        // ===== VALIDATION EMAIL =====
        if (email.isEmpty()) {
            emailError.setText("Email requis");
            valid = false;
        } else if (!email.matches("[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,}")) {
            emailError.setText("Email invalide");
            valid = false;
        }

        // ===== VALIDATION TÉLÉPHONE =====
        if (telephone.isEmpty()) {
            telephoneError.setText("Téléphone requis");
            valid = false;
        } else if (!telephone.matches("\\d{8,12}")) {
            telephoneError.setText("Téléphone invalide (8-12 chiffres)");
            valid = false;
        }

        // ===== VALIDATION IMAGE =====
        if (selectedImageFile == null) {
            errorLabel.setText("Image obligatoire");
            valid = false;
        }

        if (!valid) return;

        // ===== VÉRIFICATION UNICITÉ EN BASE =====
        try {
            boolean unique = true;

            if (coachService.emailExiste(email)) {
                emailError.setText("Email déjà utilisé");
                unique = false;
            }

            if (coachService.telephoneExiste(telephone)) {
                telephoneError.setText("Téléphone déjà utilisé");
                unique = false;
            }

            if (coachService.imageExiste(selectedImageFile.getAbsolutePath())) {
                errorLabel.setText("Image déjà utilisée");
                unique = false;
            }

            if (!unique) return;

            // ===== AJOUT EN BASE =====
            Coach c = new Coach(nom, prenom, email, telephone, selectedImageFile.getAbsolutePath());
            coachService.ajouter(c);

            // ===== FERMER LE FORMULAIRE =====
            closeForm();

        } catch (Exception e) {
            errorLabel.setText("Erreur lors de l'ajout du coach");
            e.printStackTrace(); // À remplacer par un logger en production
        }
    }

}
