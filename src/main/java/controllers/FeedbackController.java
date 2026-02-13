package controllers;

import entities.GEvenement.Feedback;
import entities.GEvenement.FeedbackFX;
import entities.GEvenement.ParticipationOption;
import services.EvenementService.FeedbackService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.chart.PieChart;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class FeedbackController implements Initializable {

    @FXML private TextArea taCommentaire;
    @FXML private Slider sliderNote;
    @FXML private Label lblNoteValue;
    @FXML private DatePicker dpDateFeedback;

    // Remplace le TextField id_participation
    @FXML private ChoiceBox<ParticipationOption> cbParticipation;

    @FXML private TableView<FeedbackFX> tableFeedbacks;
    @FXML private TableColumn<FeedbackFX, Integer> colId;
    @FXML private TableColumn<FeedbackFX, String> colCommentaire;
    @FXML private TableColumn<FeedbackFX, Integer> colNote;
    @FXML private TableColumn<FeedbackFX, Date> colDateFeedback;

    // Remplace colIdParticipation (id) par un libellé lisible
    @FXML private TableColumn<FeedbackFX, String> colParticipation;

    // === Recherche + Tri ===
    @FXML private TextField tfRecherche;
    @FXML private ChoiceBox<String> cbTri;

    @FXML private Label lblStats;
    @FXML private Button btnExportCsv;

    @FXML private PieChart chartNotes;

    private FeedbackService fs = new FeedbackService();
    private final ObservableList<FeedbackFX> feedbackList = FXCollections.observableArrayList();
    private final FilteredList<FeedbackFX> filteredData = new FilteredList<>(feedbackList, f -> true);

    private final ObservableList<ParticipationOption> participations = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        colDateFeedback.setCellValueFactory(new PropertyValueFactory<>("dateFeedback"));
        colParticipation.setCellValueFactory(new PropertyValueFactory<>("participationLabel"));

        // Init label
        if (lblNoteValue != null) {
            lblNoteValue.setText(String.valueOf((int) sliderNote.getValue()));
        }

        // Configuration du slider
        sliderNote.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (lblNoteValue != null) {
                lblNoteValue.setText(String.valueOf(newValue.intValue()));
            }
        });

        // Badges de note (couleurs) au lieu d'un simple chiffre
        setupNoteBadges();

        // Charger options participation (ChoiceBox)
        loadParticipations();

        // Charger les données
        refreshTable();

        setupSearchFilter();
        setupTriChoiceBox();

        // Mini-stats: se met à jour quand la liste visible change
        hookStatsRefresh();
        refreshStats();
    }

    private void setupNoteBadges() {
        if (colNote == null) return;

        colNote.setCellFactory(column -> new TableCell<FeedbackFX, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(String.valueOf(item));
                setTextFill(javafx.scene.paint.Color.WHITE);
                setStyle("-fx-font-weight: bold; -fx-alignment: CENTER; -fx-background-radius: 10; -fx-padding: 4 8;");

                String color;
                if (item <= 2) {
                    color = "#dc3545"; // rouge
                } else if (item == 3) {
                    color = "#f39c12"; // orange
                } else {
                    color = "#198754"; // vert
                }

                // Couleur de fond sur la cellule
                setStyle(getStyle() + " -fx-background-color: " + color + ";");
            }
        });
    }

    private void hookStatsRefresh() {
        try {
            // Quand le filtre change, la table est alimentée par un SortedList qui dépend de filteredData
            // On écoute la liste filtrée directement.
            filteredData.addListener((ListChangeListener<FeedbackFX>) c -> refreshStats());
        } catch (Exception ignored) {
            // safe: si jamais l'écoute échoue, on garde un refresh manuel via refreshTable/handleActualiser
        }

        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, o, n) -> refreshStats());
        }
    }

    private void refreshStats() {
        if (lblStats != null) {
            int count = filteredData.size();
            if (count == 0) {
                lblStats.setText("Stats: 0 feedback • moyenne: -");
            } else {
                double avg = filteredData.stream().mapToInt(FeedbackFX::getNote).average().orElse(0.0);
                lblStats.setText(String.format("Stats: %d feedback%s • moyenne: %.2f/5", count, count > 1 ? "s" : "", avg));
            }
        }

        // Mettre à jour le graphique
        refreshChart();
    }

    private void refreshChart() {
        if (chartNotes == null) return;

        // Regrouper par événement (depuis participationLabel: "Event - Investisseur").
        java.util.Map<String, java.util.List<FeedbackFX>> byEvent = new java.util.HashMap<>();

        for (FeedbackFX f : filteredData) {
            String label = f.getParticipationLabel();
            String eventName;
            if (label == null || label.isBlank()) {
                eventName = "(Événement inconnu)";
            } else {
                int idx = label.indexOf(" - ");
                eventName = idx > 0 ? label.substring(0, idx).trim() : label.trim();
                if (eventName.isBlank()) {
                    eventName = "(Événement inconnu)";
                }
            }

            byEvent.computeIfAbsent(eventName, k -> new java.util.ArrayList<>()).add(f);
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        // slice = événement (valeur = nb feedbacks). Affichage = nom de l'événement uniquement.
        // Couleur basée sur la meilleure note (max) de l'événement.
        java.util.Map<PieChart.Data, Integer> maxBySlice = new java.util.HashMap<>();
        for (var entry : byEvent.entrySet()) {
            String event = entry.getKey();
            var list = entry.getValue();
            int max = list.stream().mapToInt(FeedbackFX::getNote).max().orElse(0);
            int count = list.size();

            PieChart.Data slice = new PieChart.Data(event, count);
            data.add(slice);
            maxBySlice.put(slice, max);
        }

        if (data.isEmpty()) {
            chartNotes.setData(FXCollections.observableArrayList(new PieChart.Data("Aucun feedback", 1)));
            return;
        }

        chartNotes.setData(data);

        // Appliquer les couleurs après que JavaFX a créé les nodes.
        // Règle: vert (max>=4), orange (max==3), rouge (max<=2)
        for (PieChart.Data slice : data) {
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) return;
                int max = maxBySlice.getOrDefault(slice, 0);

                String color;
                if (max >= 4) {
                    color = "#198754"; // vert
                } else if (max == 3) {
                    color = "#f39c12"; // orange
                } else {
                    color = "#dc3545"; // rouge
                }

                newNode.setStyle("-fx-pie-color: " + color + ";");
            });
        }
    }

    @FXML
    private void handleExportCsv() {
        if (tableFeedbacks == null) {
            showAlert("Erreur", "TableView introuvable");
            return;
        }

        // Exporter ce que l'utilisateur voit (items de la table)
        var items = tableFeedbacks.getItems();
        if (items == null || items.isEmpty()) {
            showAlert("Info", "Aucune donnée à exporter");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les feedbacks (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName("feedbacks.csv");

        Stage stage = (Stage) tableFeedbacks.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // En-têtes
            w.write("id_feedback;commentaire;note;date_feedback;participation\n");

            for (FeedbackFX f : items) {
                String commentaire = safeCsv(f.getCommentaire());
                String participation = safeCsv(f.getParticipationLabel());
                String date = f.getDateFeedback() == null ? "" : f.getDateFeedback().toString();

                w.write(f.getId() + ";" + commentaire + ";" + f.getNote() + ";" + date + ";" + participation + "\n");
            }

            showAlert("Succès", "Export CSV terminé: " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'exporter: " + e.getMessage());
        }
    }

    private String safeCsv(String s) {
        if (s == null) return "";
        // On garde le séparateur ;, donc on remplace ; et retours ligne
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    @FXML
    public void handleActualiser() {
        loadParticipations();
        refreshTable();
        clearForm();
        if (tfRecherche != null) {
            tfRecherche.clear();
        }
        refreshStats();
    }

    @FXML
    public void handleRetourParticipation() {
        try {
            URL fxml = getClass().getResource("/ParticipationView.fxml");
            if (fxml == null) {
                showAlert("Erreur", "FXML introuvable: /ParticipationView.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableFeedbacks.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("Gestion des Participations - BoostUp");
            stage.show();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de revenir aux participations: " + e.getMessage());
        }
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
            Stage stage = (Stage) tableFeedbacks.getScene().getWindow();
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

    @FXML
    private void handleAjouter() {
        if (validateForm()) {
            try {
                ParticipationOption selectedPart = cbParticipation.getSelectionModel().getSelectedItem();

                FeedbackFX feedbackFX = new FeedbackFX();
                feedbackFX.setCommentaire(taCommentaire.getText());
                feedbackFX.setNote((int) sliderNote.getValue());
                feedbackFX.setDateFeedback(Date.valueOf(dpDateFeedback.getValue()));
                feedbackFX.setIdParticipation(selectedPart.getIdParticipation());

                Feedback feedback = feedbackFX.toFeedback();
                fs.ajouter(feedback);

                refreshTable();
                clearForm();

                showAlert("Succès", "Feedback ajouté!");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleModifier() {
        FeedbackFX selected = tableFeedbacks.getSelectionModel().getSelectedItem();
        if (selected != null && validateForm()) {
            try {
                ParticipationOption selectedPart = cbParticipation.getSelectionModel().getSelectedItem();

                selected.setCommentaire(taCommentaire.getText());
                selected.setNote((int) sliderNote.getValue());
                selected.setDateFeedback(Date.valueOf(dpDateFeedback.getValue()));
                selected.setIdParticipation(selectedPart.getIdParticipation());

                fs.update(selected.toFeedback());
                refreshTable();
                showAlert("Succès", "Feedback modifié!");

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSupprimer() {
        FeedbackFX selected = tableFeedbacks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Veuillez sélectionner un feedback à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer le feedback");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer ce feedback ?");

        java.util.Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // Annuler la suppression
            return;
        }

        try {
            fs.supprimer(selected.getId());
            refreshTable();
            clearForm();
            showAlert("Succès", "Feedback supprimé!");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur SQL: " + e.getMessage());
        }
    }

    @FXML
    private void handleTableClick() {
        FeedbackFX selected = tableFeedbacks.getSelectionModel().getSelectedItem();
        if (selected != null) {
            taCommentaire.setText(selected.getCommentaire());
            sliderNote.setValue(selected.getNote());
            dpDateFeedback.setValue(selected.getDateFeedback().toLocalDate());

            // Sélectionner la participation correspondante
            ParticipationOption found = participations.stream()
                    .filter(p -> p.getIdParticipation() == selected.getIdParticipation())
                    .findFirst().orElse(null);
            if (cbParticipation != null) {
                cbParticipation.getSelectionModel().select(found);
            }
        }
    }

    private boolean validateForm() {
        if (taCommentaire.getText().isEmpty()) {
            showAlert("Erreur", "Le commentaire est obligatoire");
            return false;
        }
        if (dpDateFeedback.getValue() == null) {
            showAlert("Erreur", "La date du feedback est obligatoire");
            return false;
        }
        if (cbParticipation == null || cbParticipation.getSelectionModel().getSelectedItem() == null) {
            showAlert("Erreur", "Veuillez sélectionner une participation");
            return false;
        }

        return true;
    }

    private void clearForm() {
        taCommentaire.clear();
        sliderNote.setValue(3);
        if (lblNoteValue != null) {
            lblNoteValue.setText("3");
        }
        dpDateFeedback.setValue(null);
        if (cbParticipation != null) {
            cbParticipation.getSelectionModel().clearSelection();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadParticipations() {
        if (cbParticipation == null) return;
        try {
            participations.clear();
            for (FeedbackService.ParticipationRow row : fs.readParticipationsOptions()) {
                String event = (row.evenementTitre == null || row.evenementTitre.isBlank()) ? "(Événement inconnu)" : row.evenementTitre;
                String invest = (row.nomInvestisseur == null || row.nomInvestisseur.isBlank()) ? "(Investisseur inconnu)" : row.nomInvestisseur;
                participations.add(new ParticipationOption(row.idParticipation, event + " - " + invest));
            }
            cbParticipation.setItems(participations);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les participations: " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        if (tfRecherche == null || tableFeedbacks == null) {
            return;
        }

        tfRecherche.textProperty().addListener((obs, oldV, newV) -> {
            String q = (newV == null) ? "" : newV.trim().toLowerCase();
            filteredData.setPredicate(f -> {
                if (q.isEmpty()) return true;

                String comm = f.getCommentaire() == null ? "" : f.getCommentaire().toLowerCase();
                String partLabel = f.getParticipationLabel() == null ? "" : f.getParticipationLabel().toLowerCase();

                return comm.contains(q) || partLabel.contains(q);
            });
        });

        SortedList<FeedbackFX> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableFeedbacks.comparatorProperty());
        tableFeedbacks.setItems(sorted);
    }

    private void setupTriChoiceBox() {
        if (cbTri == null || tableFeedbacks == null) {
            return;
        }

        cbTri.setItems(FXCollections.observableArrayList("Note", "DateFeedback"));
        cbTri.getSelectionModel().select("Note");

        applyTri("Note");
        cbTri.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyTri(newV));
    }

    private void applyTri(String choix) {
        if (choix == null || tableFeedbacks == null) return;

        tableFeedbacks.getSortOrder().clear();

        switch (choix.trim().toLowerCase()) {
            case "datefeedback":
            case "date":
                if (colDateFeedback != null) {
                    colDateFeedback.setSortType(TableColumn.SortType.ASCENDING);
                    tableFeedbacks.getSortOrder().add(colDateFeedback);
                }
                break;
            case "note":
            default:
                if (colNote != null) {
                    colNote.setSortType(TableColumn.SortType.DESCENDING);
                    tableFeedbacks.getSortOrder().add(colNote);
                }
                break;
        }

        tableFeedbacks.sort();
    }

    private void refreshTable() {
        try {
            feedbackList.clear();
            for (FeedbackService.FeedbackAvecParticipation row : fs.readAvecParticipation()) {
                FeedbackFX fx = new FeedbackFX();
                fx.setId(row.idFeedback);
                fx.setCommentaire(row.commentaire);
                fx.setNote(row.note);
                fx.setDateFeedback(row.dateFeedback);
                fx.setIdParticipation(row.idParticipation);

                String event = (row.evenementTitre == null || row.evenementTitre.isBlank()) ? "(Événement inconnu)" : row.evenementTitre;
                String invest = (row.nomInvestisseur == null || row.nomInvestisseur.isBlank()) ? "(Investisseur inconnu)" : row.nomInvestisseur;
                fx.setParticipationLabel(event + " - " + invest);

                feedbackList.add(fx);
            }

            // IMPORTANT: l'items est géré par setupSearchFilter() via SortedList
            if (tableFeedbacks.getItems() == null || tableFeedbacks.getItems().isEmpty()) {
                tableFeedbacks.setItems(feedbackList);
            }

            refreshStats();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }
}
