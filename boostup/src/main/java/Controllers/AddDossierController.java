package Controllers;

import entities.GCandidature.Candidature;
import entities.GCandidature.DossierCandidature;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.DossierCandidatureService;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AddDossierController implements Initializable {

    @FXML private TextField              tfNomDossier;
    @FXML private ComboBox<Candidature>  cbCandidature;
    @FXML private TextArea               taDescription;
    @FXML private TextField              tfBusinessPlan;
    @FXML private DatePicker             dpDateCreation;

    private final DossierCandidatureService service     = new DossierCandidatureService();
    private final CandidatureService        candService = new CandidatureService();
    private boolean saved = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDateCreation.setValue(LocalDate.now());

        try {
            List<Candidature> cands = candService.getAllCandidatures(true);
            cbCandidature.setItems(FXCollections.observableArrayList(cands));
        } catch (SQLException ex) {
            err("Erreur", "Impossible de charger les candidatures : " + ex.getMessage());
        }

        cbCandidature.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Candidature c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "" : c.getNomCandidature() + "  —  " + c.getNomStartup());
            }
        });
        cbCandidature.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Candidature c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Choisissez la candidature…" :
                        c.getNomCandidature() + "  —  " + c.getNomStartup());
            }
        });
    }

    @FXML private void save() {
        String nom  = tfNomDossier.getText()  == null ? "" : tfNomDossier.getText().trim();
        String desc = taDescription.getText() == null ? "" : taDescription.getText().trim();

        if (nom.isEmpty())  { highlight(tfNomDossier);  err("Requis", "Nom du dossier obligatoire."); return; }
        if (cbCandidature.getValue() == null) { err("Requis", "Sélectionnez une candidature."); return; }
        if (desc.isEmpty()) { err("Requis", "Description obligatoire."); return; }
        if (dpDateCreation.getValue() == null) { err("Requis", "Date obligatoire."); return; }

        DossierCandidature d = new DossierCandidature();
        d.setNomDossier(nom);
        d.setIdCandidature(cbCandidature.getValue().getIdCandidature());
        d.setDescriptionProjet(desc);
        d.setBusinessPlan(tfBusinessPlan.getText() == null ? "" : tfBusinessPlan.getText().trim());
        d.setDateCreation(Date.valueOf(dpDateCreation.getValue()));
        d.setEtat("INCOMPLET");
        d.setVisible(true);

        try {
            service.addDossier(d);
            saved = true;
            ((Stage) tfNomDossier.getScene().getWindow()).close();
        } catch (SQLException ex) {
            err("Erreur sauvegarde", ex.getMessage());
        }
    }

    @FXML private void close() { ((Stage) tfNomDossier.getScene().getWindow()).close(); }

    public boolean isSaved() { return saved; }

    private void highlight(TextField tf) {
        tf.setStyle("-fx-border-color:#e63946;-fx-border-width:2;-fx-border-radius:8;");
        tf.textProperty().addListener((o, a, b) -> tf.setStyle(""));
    }
    private void err(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}