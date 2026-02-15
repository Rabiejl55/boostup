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


public class EditSessionController {

    private final SessionService sessionService = new SessionService();
    private Session sessionToEdit;

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


    @FXML
    public void initialize() {
        try {
            CoachService coachService = new CoachService();
            coachComboBox.setItems(FXCollections.observableArrayList(coachService.afficherAll()));
        } catch (SQLException e) {
            e.printStackTrace(); // ou Logger pour production
            if (errorLabel != null) {
                errorLabel.setText("Impossible de charger les coachs depuis la base de données.");
            }
        }
    }


    public void setSession(Session session) {
        this.sessionToEdit = session;
        if(session!=null){
            dateSessionPicker.setValue(session.getDateSession());
            dureeField.setText(String.valueOf(session.getDuree()));
            lieuField.setText(session.getLieu());
            typeField.setText(session.getTypeSession());
            objectifField.setText(session.getObjectif());
            if(session.getCoach()!=null) coachComboBox.setValue(session.getCoach());
        }
    }

    @FXML
    private void closeForm() { ((Stage) dateSessionPicker.getScene().getWindow()).close(); }

    @FXML
    private void submitForm() {
        if(sessionToEdit==null) return;

        LocalDate date = dateSessionPicker.getValue();
        int duree = Integer.parseInt(dureeField.getText().trim());
        String lieu = lieuField.getText().trim();
        String type = typeField.getText().trim();
        String objectif = objectifField.getText().trim();
        Coach coach = coachComboBox.getValue();

        sessionToEdit.setDateSession(date);
        sessionToEdit.setDuree(duree);
        sessionToEdit.setLieu(lieu);
        sessionToEdit.setTypeSession(type);
        sessionToEdit.setObjectif(objectif);
        sessionToEdit.setCoach(coach);

        try {
            sessionService.modifier(sessionToEdit);
            closeForm();
        } catch (Exception e) {
            if(errorLabel!=null) errorLabel.setText("Erreur lors de la modification");
            e.printStackTrace();
        }
    }
}
