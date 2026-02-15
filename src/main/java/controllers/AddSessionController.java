package controllers;

import entities.GAccompagnement.Coach;
import entities.GAccompagnement.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AccompagnementService.CoachService;
import services.AccompagnementService.SessionService;

import java.time.LocalDate;
import java.sql.SQLException;

public class AddSessionController {

    private final SessionService sessionService = new SessionService();

    @FXML private DatePicker dateSessionPicker;
    @FXML private TextField dureeField;
    @FXML private TextField lieuField;
    @FXML private TextField typeField;
    @FXML private TextField objectifField;
    @FXML private ComboBox<Coach> coachComboBox;

    @FXML private Label dateError;
    @FXML private Label dureeError;
    @FXML private Label lieuError;
    @FXML private Label typeError;
    @FXML private Label objectifError;
    @FXML private Label errorLabel;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        try {
            coachComboBox.setItems(FXCollections.observableArrayList(
                    new CoachService().afficherAll()
            ));
        } catch (SQLException e) {
            e.printStackTrace();
            if (errorLabel != null) errorLabel.setText("Impossible de charger les coachs");
        }
    }


    // ================= FERMER =================
    @FXML
    private void closeForm() {
        ((Stage) dateSessionPicker.getScene().getWindow()).close();
    }

    // ================= AJOUTER =================
    @FXML
    private void submitForm() {

        // Réinitialiser les messages
        dateError.setText("");
        dureeError.setText("");
        lieuError.setText("");
        typeError.setText("");
        objectifError.setText("");
        if (errorLabel != null) errorLabel.setText("");

        LocalDate date = dateSessionPicker.getValue();
        String dureeStr = dureeField.getText().trim();
        String lieu = lieuField.getText().trim();
        String type = typeField.getText().trim();
        String objectif = objectifField.getText().trim();
        Coach coach = coachComboBox.getValue();

        boolean valid = true;

        // ===== VALIDATION =====
        if (date == null) { dateError.setText("Date requise"); valid = false; }

        int duree = 0;
        try {
            duree = Integer.parseInt(dureeStr);
            if (duree <= 0) { dureeError.setText("Durée doit être positive"); valid = false; }
        } catch (NumberFormatException e) {
            dureeError.setText("Nombre uniquement"); valid = false;
        }

        if (lieu.isEmpty()) { lieuError.setText("Lieu requis"); valid = false; }
        if (type.isEmpty()) { typeError.setText("Type requis"); valid = false; }
        if (objectif.isEmpty()) { objectifError.setText("Objectif requis"); valid = false; }
        if (coach == null) { if (errorLabel != null) errorLabel.setText("Sélectionner un coach"); valid = false; }

        if (!valid) return;

        // ===== AJOUT =====
        try {
            Session s = new Session(date, duree, lieu, type, objectif, coach);
            sessionService.ajouter(s);
            closeForm();
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("Erreur lors de l'ajout");
            e.printStackTrace();
        }
    }
}
