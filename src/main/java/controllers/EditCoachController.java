package controllers;

import entities.GAccompagnement.Coach;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.AccompagnementService.CoachService;

import java.io.File;

public class EditCoachController {

    private final CoachService coachService = new CoachService();
    private Coach coachToEdit;
    private File selectedImageFile;

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

    @FXML
    public void setCoach(Coach coach) {
        this.coachToEdit = coach;
        nomField.setText(coach.getNom());
        prenomField.setText(coach.getPrenom());
        emailField.setText(coach.getEmail());
        telephoneField.setText(coach.getTelephone());
        if (coach.getImagecoach() != null) {
            imageLabel.setText(new File(coach.getImagecoach()).getName());
        }
    }

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

    @FXML
    private void closeForm() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void submitForm() {
        System.out.println("coach = " + coachToEdit);

        nomError.setText("");
        prenomError.setText("");
        emailError.setText("");
        telephoneError.setText("");
        errorLabel.setText("");

        if (coachToEdit == null) {
            errorLabel.setText("Aucun coach sélectionné !");
            return;
        }

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telephone = telephoneField.getText().trim();
        String imagecoach = selectedImageFile != null ? selectedImageFile.getAbsolutePath() : coachToEdit.getImagecoach();

        boolean valid = true;

        if (nom.isEmpty() || !nom.matches("[a-zA-ZÀ-ÿ ]{2,50}")) {
            nomError.setText("Nom invalide (2-50 lettres)");
            valid = false;
        }

        // ===== VALIDATION PRÉNOM =====
        if (prenom.isEmpty() || !prenom.matches("[a-zA-ZÀ-ÿ ]{2,50}")) {
            prenomError.setText("Prénom invalide (2-50 lettres)");
            valid = false;
        }

        // ===== VALIDATION EMAIL =====
        if (email.isEmpty() || !email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            emailError.setText("Email invalide");
            valid = false;
        }

        // ===== VALIDATION TÉLÉPHONE =====
        if (telephone.isEmpty() || !telephone.matches("\\d{8,12}")) {
            telephoneError.setText("Téléphone invalide (8-12 chiffres)");
            valid = false;
        }

        // ===== VALIDATION IMAGE =====
        if (imagecoach == null || imagecoach.isEmpty()) {
            errorLabel.setText("Image obligatoire");
            valid = false;
        }

        if (!valid) return;

        try {
            boolean unique = true;

            // Vérifier email uniquement si différent de l'actuel
            if (!email.equals(coachToEdit.getEmail()) && coachService.emailExiste(email)) {
                emailError.setText("Email déjà utilisé");
                unique = false;
            }

            if (!telephone.equals(coachToEdit.getTelephone()) && coachService.telephoneExiste(telephone)) {
                telephoneError.setText("Téléphone déjà utilisé");
                unique = false;
            }

            if (!imagecoach.equals(coachToEdit.getImagecoach()) && coachService.imageExiste(imagecoach)) {
                errorLabel.setText("Image déjà utilisée");
                unique = false;
            }

            if (!unique) return;

            coachToEdit.setNom(nom);
            coachToEdit.setPrenom(prenom);
            coachToEdit.setEmail(email);
            coachToEdit.setTelephone(telephone);
            coachToEdit.setImagecoach(imagecoach);

            coachService.modifier(coachToEdit);
            closeForm();

        } catch (Exception e) {
            errorLabel.setText("Erreur lors de la modification");
            e.printStackTrace();
        }
    }

}
