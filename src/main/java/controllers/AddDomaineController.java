package controllers;

import entities.GAccompagnement.Domaine;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.AccompagnementService.DomaineService;

import java.io.File;

public class AddDomaineController {

    private final DomaineService domaineService = new DomaineService();

    @FXML private TextField nomField;
    @FXML private TextArea descriptionField;
    @FXML private TextField niveauField;
    @FXML private ComboBox<String> statutBox;

    @FXML private Label nomError;
    @FXML private Label descriptionError;
    @FXML private Label niveauError;
    @FXML private Label statutError;
    @FXML private Label errorLabel;
    @FXML private Label imageLabel;

    private File selectedImageFile;

    @FXML
    public void initialize() {
        statutBox.getItems().addAll("Actif", "Inactif");
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
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

        // Réinitialiser les messages d'erreur
        nomError.setText("");
        descriptionError.setText("");
        niveauError.setText("");
        statutError.setText("");
        errorLabel.setText("");

        String nom = nomField.getText().trim();
        String description = descriptionField.getText().trim();
        String niveau = niveauField.getText().trim();
        String statut = statutBox.getValue();

        boolean valid = true;

        // NOM
        if (nom.isEmpty()) {
            nomError.setText("Nom requis");
            valid = false;
        } else if (!nom.matches("[a-zA-Z ]{2,50}")) {
            nomError.setText("Lettres uniquement (2-50 caractères)");
            valid = false;
        }

        if (description.isEmpty()) {
            descriptionError.setText("Description requise");
            valid = false;
        }

        if (niveau.isEmpty()) {
            niveauError.setText("Niveau requis");
            valid = false;
        }

        if (statut == null) {
            statutError.setText("Statut requis");
            valid = false;
        }

        if (selectedImageFile == null) {
            errorLabel.setText("Image obligatoire");
            valid = false;
        }

        if (!valid) return;

        try {
            Domaine d = new Domaine(
                    nom,
                    description,
                    niveau,
                    statut,
                    selectedImageFile.getAbsolutePath()
            );

            domaineService.ajouter(d);
            closeForm();

        } catch (Exception e) {
            errorLabel.setText("Erreur lors de l'ajout du domaine");
            e.printStackTrace();
        }
    }
}
