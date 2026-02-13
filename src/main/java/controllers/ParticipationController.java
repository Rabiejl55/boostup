package controllers;

import entities.GEvenement.EvenementOption;
import entities.GEvenement.Participation;
import entities.GEvenement.ParticipationFX;
import services.EvenementService.ParticipationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ParticipationController implements Initializable {

    @FXML private TextField tfNomStartup;
    @FXML private TextField tfNomInvestisseur;
    @FXML private CheckBox cbPresence;
    @FXML private DatePicker dpDateInscription;

    // Remplace l'ancien TextField id_evenement
    @FXML private ChoiceBox<EvenementOption> cbEvenement;

    @FXML private TableView<ParticipationFX> tableParticipations;
    @FXML private TableColumn<ParticipationFX, Integer> colId;
    @FXML private TableColumn<ParticipationFX, String> colStartup;
    @FXML private TableColumn<ParticipationFX, String> colInvestisseur;
    @FXML private TableColumn<ParticipationFX, Boolean> colPresence;
    @FXML private TableColumn<ParticipationFX, Date> colDateInscription;

    // Au lieu de l'id_evenement, on affiche le titre
    @FXML private TableColumn<ParticipationFX, String> colEvenement;

    // === Recherche + Tri ===
    @FXML private TextField tfRecherche;
    @FXML private ChoiceBox<String> cbTri;

    private final ParticipationService ps = new ParticipationService();
    private final ObservableList<ParticipationFX> participationList = FXCollections.observableArrayList();
    private final FilteredList<ParticipationFX> filteredData = new FilteredList<>(participationList, p -> true);

    private final ObservableList<EvenementOption> evenements = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStartup.setCellValueFactory(new PropertyValueFactory<>("nomStartup"));
        colInvestisseur.setCellValueFactory(new PropertyValueFactory<>("nomInvestisseur"));
        colPresence.setCellValueFactory(new PropertyValueFactory<>("presence"));
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));

        // Présence: afficher 'Tous les membres sont présents' / 'Absents' au lieu de true/false
        if (colPresence != null) {
            colPresence.setCellFactory(column -> new TableCell<ParticipationFX, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item ? "Tous les membres sont présents" : "Absents");
                    }
                }
            });
        }

        // Remplir formulaire quand on sélectionne une ligne
        tableParticipations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                tfNomStartup.setText(selected.getNomStartup());
                tfNomInvestisseur.setText(selected.getNomInvestisseur());
                cbPresence.setSelected(selected.isPresence());
                if (selected.getDateInscription() != null) {
                    dpDateInscription.setValue(selected.getDateInscription().toLocalDate());
                } else {
                    dpDateInscription.setValue(null);
                }

                // sélectionner l'évènement correspondant à l'id
                if (cbEvenement != null) {
                    EvenementOption found = evenements.stream()
                            .filter(ev -> ev.getId() == selected.getIdEvenement())
                            .findFirst().orElse(null);
                    cbEvenement.getSelectionModel().select(found);
                }
            }
        });

        // Combo événements (ChoiceBox)
        loadEvenements();

        // Charger les données
        refreshTable();

        setupSearchFilter();
        setupTriChoiceBox();
    }

    private void loadEvenements() {
        if (cbEvenement == null) return;

        try {
            evenements.clear();
            for (ParticipationService.EvenementRow row : ps.readEvenements()) {
                evenements.add(new EvenementOption(row.id, row.titre));
            }
            cbEvenement.setItems(evenements);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les événements: " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        if (tfRecherche == null || tableParticipations == null) {
            return;
        }

        tfRecherche.textProperty().addListener((obs, oldV, newV) -> {
            String q = (newV == null) ? "" : newV.trim().toLowerCase();
            filteredData.setPredicate(p -> {
                if (q.isEmpty()) return true;

                String startup = p.getNomStartup() == null ? "" : p.getNomStartup().toLowerCase();
                String invest = p.getNomInvestisseur() == null ? "" : p.getNomInvestisseur().toLowerCase();
                String evTitre = p.getEvenementTitre() == null ? "" : p.getEvenementTitre().toLowerCase();

                return startup.contains(q) || invest.contains(q) || evTitre.contains(q);
            });
        });

        SortedList<ParticipationFX> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableParticipations.comparatorProperty());
        tableParticipations.setItems(sorted);
    }

    private void setupTriChoiceBox() {
        if (cbTri == null || tableParticipations == null) {
            return;
        }

        cbTri.setItems(FXCollections.observableArrayList("Titre", "Date", "Présence"));
        cbTri.getSelectionModel().select("Titre");

        applyTri("Titre");
        cbTri.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyTri(newV));
    }

    private void applyTri(String choix) {
        if (choix == null || tableParticipations == null) return;

        tableParticipations.getSortOrder().clear();

        switch (choix.trim().toLowerCase()) {
            case "date":
                if (colDateInscription != null) {
                    colDateInscription.setSortType(TableColumn.SortType.ASCENDING);
                    tableParticipations.getSortOrder().add(colDateInscription);
                }
                break;
            case "présence":
            case "presence":
                if (colPresence != null) {
                    colPresence.setSortType(TableColumn.SortType.DESCENDING);
                    tableParticipations.getSortOrder().add(colPresence);
                }
                break;
            case "titre":
            default:
                // ici "Titre" = nomStartup
                if (colStartup != null) {
                    colStartup.setSortType(TableColumn.SortType.ASCENDING);
                    tableParticipations.getSortOrder().add(colStartup);
                }
                break;
        }

        tableParticipations.sort();
    }

    private void refreshTable() {
        try {
            participationList.clear();
            for (ParticipationService.ParticipationAvecEvenement row : ps.readAvecEvenement()) {
                ParticipationFX fx = new ParticipationFX();
                fx.setId(row.idParticipation);
                fx.setNomStartup(row.nomStartup);
                fx.setNomInvestisseur(row.nomInvestisseur);
                fx.setPresence(row.presence);
                fx.setDateInscription(row.dateInscription);
                fx.setIdEvenement(row.idEvenement);
                fx.setEvenementTitre(row.titreEvenement);
                participationList.add(fx);
            }

            // IMPORTANT: l'items est géré par setupSearchFilter() via SortedList
            if (tableParticipations.getItems() == null || tableParticipations.getItems().isEmpty()) {
                tableParticipations.setItems(participationList);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    @FXML
    void handleActualiser() {
        loadEvenements();
        refreshTable();
        clearForm();
        if (tfRecherche != null) {
            tfRecherche.clear();
        }
    }

    @FXML
    private void handleRetourEvenements() {
        try {
            URL fxml = getClass().getResource("/EvenementView.fxml");
            if (fxml == null) {
                showAlert("Erreur", "FXML introuvable: /EvenementView.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableParticipations.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("Gestion des Événements - BoostUp");
            stage.show();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de revenir aux événements: " + e.getMessage());
        }
    }

    @FXML
    private void handleOuvrirFeedback() {
        try {
            URL fxml = getClass().getResource("/FeedbackView.fxml");
            if (fxml == null) {
                showAlert("Erreur", "FXML introuvable: /FeedbackView.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableParticipations.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("Gestion des Feedbacks - BoostUp");
            stage.show();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir l'écran Feedback: " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouter() {
        if (validateForm()) {
            try {
                EvenementOption selectedEv = cbEvenement.getSelectionModel().getSelectedItem();
                int idEvenement = selectedEv.getId();

                ParticipationFX participationFX = new ParticipationFX();
                participationFX.setNomStartup(tfNomStartup.getText());
                participationFX.setNomInvestisseur(tfNomInvestisseur.getText());
                participationFX.setPresence(cbPresence.isSelected());
                participationFX.setDateInscription(Date.valueOf(dpDateInscription.getValue()));
                participationFX.setIdEvenement(idEvenement);

                Participation participation = participationFX.toParticipation();
                ps.ajouter(participation);

                refreshTable();
                clearForm();

                showAlert("Succès", "Participation ajoutée!");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleModifier() {
        ParticipationFX selected = tableParticipations.getSelectionModel().getSelectedItem();
        if (selected != null && validateForm()) {
            try {
                EvenementOption selectedEv = cbEvenement.getSelectionModel().getSelectedItem();
                int idEvenement = selectedEv.getId();

                selected.setNomStartup(tfNomStartup.getText());
                selected.setNomInvestisseur(tfNomInvestisseur.getText());
                selected.setPresence(cbPresence.isSelected());
                selected.setDateInscription(Date.valueOf(dpDateInscription.getValue()));
                selected.setIdEvenement(idEvenement);

                ps.update(selected.toParticipation());
                refreshTable();
                showAlert("Succès", "Participation modifiée!");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSupprimer() {
        ParticipationFX selected = tableParticipations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Veuillez sélectionner une participation à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer la participation");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette participation ?");

        java.util.Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // Annuler
            return;
        }

        try {
            ps.supprimer(selected.getId());
            refreshTable();
            clearForm();
            showAlert("Succès", "Participation supprimée!");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur SQL: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        String startup = tfNomStartup.getText() == null ? "" : tfNomStartup.getText().trim();
        String invest = tfNomInvestisseur.getText() == null ? "" : tfNomInvestisseur.getText().trim();

        if (startup.isEmpty()) {
            showAlert("Erreur", "Le nom de la startup est obligatoire");
            return false;
        }
        if (startup.matches("\\d+")) {
            showAlert("Erreur", "Le nom de la startup ne peut pas contenir uniquement des chiffres");
            return false;
        }

        if (invest.isEmpty()) {
            showAlert("Erreur", "Le nom de l'investisseur est obligatoire");
            return false;
        }
        if (invest.matches("\\d+")) {
            showAlert("Erreur", "Le nom de l'investisseur ne peut pas contenir uniquement des chiffres");
            return false;
        }

        if (dpDateInscription.getValue() == null) {
            showAlert("Erreur", "La date d'inscription est obligatoire");
            return false;
        }
        if (dpDateInscription.getValue().isAfter(java.time.LocalDate.now())) {
            showAlert("Erreur", "La date d'inscription doit être inférieure ou égale à la date d'aujourd'hui");
            return false;
        }

        if (cbEvenement == null || cbEvenement.getSelectionModel().getSelectedItem() == null) {
            showAlert("Erreur", "Veuillez sélectionner un événement");
            return false;
        }

        // Remettre les valeurs trimées (petit plus UX)
        tfNomStartup.setText(startup);
        tfNomInvestisseur.setText(invest);

        return true;
    }

    private void clearForm() {
        tfNomStartup.clear();
        tfNomInvestisseur.clear();
        cbPresence.setSelected(false);
        dpDateInscription.setValue(null);
        if (cbEvenement != null) {
            cbEvenement.getSelectionModel().clearSelection();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            URL fxml = getClass().getResource("/views/login.fxml");
            if (fxml == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText(null);
                alert.setContentText("FXML introuvable: /views/login.fxml");
                alert.showAndWait();
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableParticipations.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("BoostUp - Connexion");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de revenir au login: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
