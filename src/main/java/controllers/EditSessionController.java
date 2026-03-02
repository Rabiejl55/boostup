package controllers;

import entities.GAccompagnement.Coach;
import entities.GAccompagnement.Domaine;
import entities.GAccompagnement.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AccompagnementService.CoachService;
import services.AccompagnementService.DomaineService;
import services.AccompagnementService.SessionService;

import java.sql.SQLException;
import java.time.LocalDate;

public class EditSessionController {

    private final SessionService sessionService = new SessionService();
    private final DomaineService domaineService = new DomaineService();
    private Session sessionToEdit;

    @FXML private DatePicker dateSessionPicker;
    @FXML private TextField dureeField;
    @FXML private TextField lieuField;
    @FXML private TextField typeField;
    @FXML private TextField objectifField;
    @FXML private ComboBox<Coach> coachComboBox;
    @FXML private ComboBox<Domaine> domaineComboBox;

    @FXML private Label dateError;
    @FXML private Label dureeError;
    @FXML private Label lieuError;
    @FXML private Label typeError;
    @FXML private Label objectifError;
    @FXML private Label domaineError;
    @FXML private Label errorLabel;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        try {
            // Charger les coachs
            coachComboBox.setItems(FXCollections.observableArrayList(
                    new CoachService().afficherAll()
            ));

            // Charger les domaines
            domaineComboBox.setItems(FXCollections.observableArrayList(
                    domaineService.afficherAll()
            ));

            // Afficher seulement le nom du domaine
            domaineComboBox.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(Domaine item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });
            domaineComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Domaine item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });

            // Désactiver les dates passées dans le DatePicker
            dateSessionPicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Impossible de charger les coachs ou domaines.");
            }
        }
    }

    // ================= SET SESSION =================
    public void setSession(Session session) {
        this.sessionToEdit = session;
        if(session != null){
            dateSessionPicker.setValue(session.getDateSession());
            dureeField.setText(String.valueOf(session.getDuree()));
            lieuField.setText(session.getLieu());
            typeField.setText(session.getTypeSession());
            objectifField.setText(session.getObjectif());

            if(session.getCoach() != null) coachComboBox.setValue(session.getCoach());
            if(session.getDomaine() != null) domaineComboBox.setValue(session.getDomaine());
        }
    }

    // ================= FERMER =================
    @FXML
    private void closeForm() {
        ((Stage) dateSessionPicker.getScene().getWindow()).close();
    }

    // ================= SUBMIT =================
    @FXML
    private void submitForm() {
        if(sessionToEdit == null) return;

        boolean valid = true;

        LocalDate date = dateSessionPicker.getValue();
        String dureeStr = dureeField.getText().trim();
        String lieu = lieuField.getText().trim();
        String type = typeField.getText().trim();
        String objectif = objectifField.getText().trim();
        Coach coach = coachComboBox.getValue();
        Domaine domaine = domaineComboBox.getValue();

        // ===== VALIDATION =====
        dateError.setText("");
        dureeError.setText("");
        lieuError.setText("");
        typeError.setText("");
        objectifError.setText("");
        domaineError.setText("");
        if (errorLabel != null) errorLabel.setText("");

        // Date obligatoire et pas dans le passé
        if(date == null) {
            dateError.setText("Date requise");
            valid = false;
        } else if(date.isBefore(LocalDate.now())) {
            dateError.setText("La date ne peut pas être passée");
            valid = false;
        }

        // Durée
        int duree = 0;
        try {
            duree = Integer.parseInt(dureeStr);
            if(duree <= 0) { dureeError.setText("Durée doit être positive"); valid = false; }
        } catch (NumberFormatException e) {
            dureeError.setText("Nombre uniquement"); valid = false;
        }

        // Autres champs
        if(lieu.isEmpty()) { lieuError.setText("Lieu requis"); valid = false; }
        if(type.isEmpty()) { typeError.setText("Type requis"); valid = false; }
        if(objectif.isEmpty()) { objectifError.setText("Objectif requis"); valid = false; }
        if(coach == null) { if(errorLabel != null) errorLabel.setText("Sélectionner un coach"); valid = false; }
        if(domaine == null) { domaineError.setText("Sélectionner un domaine"); valid = false; }

        if(!valid) return;

        // ===== MODIFICATION =====
        sessionToEdit.setDateSession(date);
        sessionToEdit.setDuree(duree);
        sessionToEdit.setLieu(lieu);
        sessionToEdit.setTypeSession(type);
        sessionToEdit.setObjectif(objectif);
        sessionToEdit.setCoach(coach);
        sessionToEdit.setDomaine(domaine);

        try {
            sessionService.modifier(sessionToEdit);
            closeForm();
        } catch (Exception e) {
            if(errorLabel != null) errorLabel.setText("Erreur lors de la modification");
            e.printStackTrace();
        }
    }
}
