package Controllers;

import entities.GCandidature.Candidature;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AddCandidatureController implements Initializable {

    @FXML private TextField  tfNomCandidature;
    @FXML private TextField  tfNomStartup;
    @FXML private DatePicker dpDate;
    @FXML private TextArea   taComment;

    private final CandidatureService service  = new CandidatureService();
    private final LocalDate          MIN_DATE = LocalDate.of(2026, 2, 23);
    private boolean saved = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDate.setValue(MIN_DATE);
        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(MIN_DATE));
                if (date.isBefore(MIN_DATE))
                    setStyle("-fx-background-color:#f5f5f5;-fx-text-fill:#cccccc;");
            }
        });
        // Validation saisie manuelle de date
        dpDate.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateDate();
        });
    }

    private void validateDate() {
        try {
            LocalDate parsed = LocalDate.parse(
                    dpDate.getEditor().getText(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            if (parsed.isBefore(MIN_DATE)) {
                dpDate.setValue(MIN_DATE);
                showError("Date invalide", "La date ne peut pas être antérieure au 23/02/2026.");
            } else {
                dpDate.setValue(parsed);
            }
        } catch (Exception ignored) {
            dpDate.setValue(MIN_DATE);
        }
    }

    @FXML
    private void save() {
        String nom     = tfNomCandidature.getText() == null ? "" : tfNomCandidature.getText().trim();
        String startup = tfNomStartup.getText()     == null ? "" : tfNomStartup.getText().trim();

        if (nom.isEmpty()) {
            tfNomCandidature.setStyle("-fx-border-color:#e63946;-fx-border-width:2px;-fx-border-radius:8px;");
            showError("Champ obligatoire", "Le nom du candidat est requis.");
            return;
        }
        if (startup.isEmpty()) {
            tfNomStartup.setStyle("-fx-border-color:#e63946;-fx-border-width:2px;-fx-border-radius:8px;");
            showError("Champ obligatoire", "Le nom de la startup est requis.");
            return;
        }
        if (dpDate.getValue() == null || dpDate.getValue().isBefore(MIN_DATE)) {
            showError("Date invalide", "La date de dépôt ne peut pas être antérieure au 23/02/2026.");
            return;
        }

        Candidature c = new Candidature();
        c.setNomCandidature(nom);
        c.setNomStartup(startup);
        c.setDateDepot(Date.valueOf(dpDate.getValue()));
        c.setStatut("EN_ATTENTE");           // toujours EN_ATTENTE à la création
        c.setScore(null);                    // score = null, sera rempli après évaluation
        c.setVisible(true);
        c.setIdStartup(1);                   // à remplacer par vrai sélecteur si besoin
        c.setCommentaire(taComment.getText() == null ? "" : taComment.getText().trim());

        try {
            service.addCandidature(c);
            saved = true;
            ((Stage) tfNomCandidature.getScene().getWindow()).close();
        } catch (Exception ex) {
            showError("Erreur de sauvegarde", "Impossible d'enregistrer : " + ex.getMessage());
        }
    }

    @FXML
    private void close() {
        ((Stage) tfNomCandidature.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}