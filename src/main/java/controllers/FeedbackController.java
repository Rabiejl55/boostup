package controllers;

import entities.GEvenement.Feedback;
import entities.GEvenement.FeedbackFX;
import entities.GEvenement.ParticipationOption;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import services.AccompagnementService.SuggestionAPI;
import services.AccompagnementService.ProfanityDetectorAPI;
import services.EvenementService.FeedbackService;
import services.SentimentAnalysisService;
import services.SentimentAnalysisService.SentimentResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FeedbackController implements Initializable {

    // ═══════════════════════════════════════════════════
    //  CHAMPS ADMIN (FeedbackView.fxml)
    // ═══════════════════════════════════════════════════
    @FXML private TextArea taCommentaire;
    @FXML private Slider sliderNote;
    @FXML private Label lblNoteValue;
    @FXML private DatePicker dpDateFeedback;
    @FXML private Label lblSentimentIA;
    @FXML private ChoiceBox<ParticipationOption> cbParticipation;
    @FXML private TableView<FeedbackFX> tableFeedbacks;
    @FXML private TableColumn<FeedbackFX, Integer> colId;
    @FXML private TableColumn<FeedbackFX, String> colCommentaire;
    @FXML private TableColumn<FeedbackFX, Integer> colNote;
    @FXML private TableColumn<FeedbackFX, Date> colDateFeedback;
    @FXML private TableColumn<FeedbackFX, String> colParticipation;
    @FXML private TextField tfRecherche;
    @FXML private ChoiceBox<String> cbTri;
    @FXML private Label lblStats;
    @FXML private Button btnExportCsv;
    @FXML private PieChart chartNotes;

    // ═══════════════════════════════════════════════════
    //  CHAMPS FRONT / USER (Feedback.fxml)
    // ═══════════════════════════════════════════════════
    @FXML private HBox starBox;
    @FXML private TextArea commentTextArea;
    @FXML private Button sendFeedbackButton;
    @FXML private VBox commentsContainer;

    private int rating = 0;

    // ═══════════════════════════════════════════════════
    //  SERVICES & DATA
    // ═══════════════════════════════════════════════════
    private FeedbackService fs = new FeedbackService();
    private final ObservableList<FeedbackFX> feedbackList = FXCollections.observableArrayList();
    private final FilteredList<FeedbackFX> filteredData = new FilteredList<>(feedbackList, f -> true);
    private final ObservableList<ParticipationOption> participations = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ─── FRONT / USER view (Feedback.fxml — star rating) ───
        if (starBox != null) {
            setupStars();
        }

        // ─── ADMIN / CRUD view (FeedbackView.fxml — table) ───
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
            colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
            colDateFeedback.setCellValueFactory(new PropertyValueFactory<>("dateFeedback"));
            colParticipation.setCellValueFactory(new PropertyValueFactory<>("participationLabel"));
        }

        if (lblNoteValue != null && sliderNote != null) {
            lblNoteValue.setText(String.valueOf((int) sliderNote.getValue()));
        }

        if (sliderNote != null) {
            sliderNote.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (lblNoteValue != null) {
                    lblNoteValue.setText(String.valueOf(newValue.intValue()));
                }
            });
        }

        // 🧠 ANALYSE DE SENTIMENT IA EN TEMPS RÉEL
        setupSentimentAnalysis();

        // Badges de note (couleurs) au lieu d'un simple chiffre
        setupNoteBadges();

        // Charger options participation (ChoiceBox)
        loadParticipations();

        // Charger les données (admin table only)
        if (tableFeedbacks != null) {
            refreshTable();
            setupSearchFilter();
            setupTriChoiceBox();
            hookStatsRefresh();
            refreshStats();
            setupInlineEditing();
        }
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

    // ════════════════════════════════════════════════════════
    // 🧠 ANALYSE DE SENTIMENT IA EN TEMPS RÉEL
    // ════════════════════════════════════════════════════════

    /**
     * Configure l'analyse de sentiment automatique lors de la saisie du commentaire
     */
    private void setupSentimentAnalysis() {
        if (taCommentaire == null || lblSentimentIA == null) {
            System.err.println("⚠️ Commentaire ou label sentiment IA manquant dans FXML");
            return;
        }

        // Style initial du label
        lblSentimentIA.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8 12; " +
                               "-fx-background-radius: 8; -fx-font-weight: bold; " +
                               "-fx-font-size: 13px;");
        lblSentimentIA.setText("💬 Tapez un commentaire pour analyse IA...");

        // Analyse en temps réel quand l'utilisateur tape
        taCommentaire.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                // Reset quand vide
                lblSentimentIA.setText("💬 Tapez un commentaire pour analyse IA...");
                lblSentimentIA.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8 12; " +
                                       "-fx-background-radius: 8; -fx-font-weight: bold; " +
                                       "-fx-font-size: 13px; -fx-text-fill: #6c757d;");
                return;
            }

            // Analyse asynchrone pour ne pas bloquer l'UI
            new Thread(() -> {
                try {
                    SentimentResult result = SentimentAnalysisService.analyserSentiment(newValue);

                    // Mise à jour UI sur le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        lblSentimentIA.setText(result.getEmoji() + " " + result.getLabel() +
                                              " (" + result.getConfidencePercent() + ")");
                        lblSentimentIA.setStyle("-fx-background-color: " + result.getColor() + "20; " +
                                               "-fx-padding: 8 12; -fx-background-radius: 8; " +
                                               "-fx-font-weight: bold; -fx-font-size: 13px; " +
                                               "-fx-text-fill: " + result.getColor() + "; " +
                                               "-fx-border-color: " + result.getColor() + "; " +
                                               "-fx-border-width: 2; -fx-border-radius: 8;");
                    });

                } catch (Exception e) {
                    System.err.println("❌ Erreur analyse sentiment : " + e.getMessage());
                }
            }).start();
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
        // Option A: édition inline
        showAlert("Modification", "La modification se fait directement dans le tableau (double-clic sur une cellule). ");
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

    private void setupInlineEditing() {
        if (tableFeedbacks == null) return;
        tableFeedbacks.setEditable(true);

        // Commentaire
        if (colCommentaire != null) {
            colCommentaire.setEditable(true);
            colCommentaire.setCellFactory(TextFieldTableCell.forTableColumn());
            colCommentaire.setOnEditCommit(ev -> {
                FeedbackFX row = ev.getRowValue();
                if (row == null) return;
                String newV = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
                if (newV.isEmpty()) {
                    showAlert("Validation", "Le commentaire est obligatoire");
                    tableFeedbacks.refresh();
                    return;
                }
                String old = row.getCommentaire();
                row.setCommentaire(newV);
                try {
                    fs.update(row.toFeedback());
                } catch (SQLException ex) {
                    row.setCommentaire(old);
                    tableFeedbacks.refresh();
                    showAlert("Erreur", "Erreur SQL: " + ex.getMessage());
                }
            });
        }

        // Note
        if (colNote != null) {
            colNote.setEditable(true);
            IntegerStringConverter intConv = new IntegerStringConverter();
            colNote.setCellFactory(TextFieldTableCell.forTableColumn(intConv));
            colNote.setOnEditCommit(ev -> {
                FeedbackFX row = ev.getRowValue();
                if (row == null) return;
                Integer newV = ev.getNewValue();
                if (newV == null || newV < 1 || newV > 5) {
                    showAlert("Validation", "La note doit être entre 1 et 5");
                    tableFeedbacks.refresh();
                    return;
                }
                int old = row.getNote();
                row.setNote(newV);
                try {
                    fs.update(row.toFeedback());
                    refreshStats();
                } catch (SQLException ex) {
                    row.setNote(old);
                    tableFeedbacks.refresh();
                    showAlert("Erreur", "Erreur SQL: " + ex.getMessage());
                }
            });
        }

        // Date feedback (DatePicker)
        if (colDateFeedback != null) {
            colDateFeedback.setEditable(true);
            colDateFeedback.setCellFactory(column -> new DateEditingCell());
        }

        // Participation label: non editable
        if (colParticipation != null) {
            colParticipation.setEditable(false);
        }
    }

    /** Cellule DatePicker pour colDateFeedback : commit + update DB */
    private class DateEditingCell extends TableCell<FeedbackFX, Date> {
        private final DatePicker picker = new DatePicker();

        DateEditingCell() {
            picker.setOnAction(e -> commitFromPicker());
            picker.focusedProperty().addListener((obs, old, foc) -> {
                if (!foc && isEditing()) commitFromPicker();
            });
        }

        private void commitFromPicker() {
            java.time.LocalDate ld = picker.getValue();
            if (ld == null) {
                cancelEdit();
                return;
            }
            commitEdit(Date.valueOf(ld));
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (!isEmpty()) {
                Date d = getItem();
                picker.setValue(d == null ? null : d.toLocalDate());
                setGraphic(picker);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        public void commitEdit(Date newValue) {
            FeedbackFX row = getTableRow() == null ? null : getTableRow().getItem();
            if (row == null || newValue == null) {
                super.commitEdit(newValue);
                return;
            }

            Date old = row.getDateFeedback();
            row.setDateFeedback(newValue);
            try {
                fs.update(row.toFeedback());
                super.commitEdit(newValue);
            } catch (SQLException ex) {
                row.setDateFeedback(old);
                tableFeedbacks.refresh();
                showAlert("Erreur", "Erreur SQL: " + ex.getMessage());
                super.cancelEdit();
            }

            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        protected void updateItem(Date item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                return;
            }
            setText(item == null ? "" : item.toLocalDate().toString());
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FRONT / USER — Star Rating (Feedback.fxml)
    // ═══════════════════════════════════════════════════════

    private void setupStars() {
        if (starBox == null) return;
        starBox.getChildren().clear();

        Image starFilled = null;
        Image starEmpty = null;
        try {
            starFilled = new Image(getClass().getResourceAsStream("/images/star_filled.png"));
            starEmpty = new Image(getClass().getResourceAsStream("/images/star_empty.png"));
        } catch (Exception e) {
            System.err.println("Star images not found, using text fallback");
        }

        for (int i = 1; i <= 5; i++) {
            final int starValue = i;

            if (starFilled != null && starEmpty != null) {
                // Image-based stars
                ImageView star = new ImageView(starEmpty);
                star.setFitWidth(30);
                star.setFitHeight(30);

                final Image filled = starFilled;
                final Image empty = starEmpty;

                star.setOnMouseEntered(e -> highlightStars(starValue, filled, empty));
                star.setOnMouseExited(e -> highlightStars(rating, filled, empty));
                star.setOnMouseClicked(e -> {
                    rating = starValue;
                    highlightStars(rating, filled, empty);
                });

                starBox.getChildren().add(star);
            } else {
                // Text fallback if images are missing
                Label star = new Label("☆");
                star.setStyle("-fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: #ccc;");

                star.setOnMouseEntered(e -> highlightStarsText(starValue));
                star.setOnMouseExited(e -> highlightStarsText(rating));
                star.setOnMouseClicked(e -> {
                    rating = starValue;
                    highlightStarsText(rating);
                });

                starBox.getChildren().add(star);
            }
        }
    }

    private void highlightStars(int upTo, Image filled, Image empty) {
        if (starBox == null) return;
        for (int i = 0; i < starBox.getChildren().size(); i++) {
            if (starBox.getChildren().get(i) instanceof ImageView) {
                ImageView star = (ImageView) starBox.getChildren().get(i);
                star.setImage(i < upTo ? filled : empty);
            }
        }
    }

    private void highlightStarsText(int upTo) {
        if (starBox == null) return;
        for (int i = 0; i < starBox.getChildren().size(); i++) {
            if (starBox.getChildren().get(i) instanceof Label) {
                Label star = (Label) starBox.getChildren().get(i);
                if (i < upTo) {
                    star.setText("★");
                    star.setStyle("-fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: #f5a623;");
                } else {
                    star.setText("☆");
                    star.setStyle("-fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: #ccc;");
                }
            }
        }
    }

    @FXML
    private void handleSendFeedback() {
        String commentText = (commentTextArea != null) ? commentTextArea.getText().trim() : "";

        if (rating == 0 && commentText.isEmpty()) {
            showAlert("Attention", "Veuillez mettre une note ou écrire un commentaire !");
            return;
        }

        // Vérification langage inapproprié dans un thread séparé
        new Thread(() -> {
            boolean containsBadWords = false;
            try {
                containsBadWords = ProfanityDetectorAPI.hasBadWords(commentText);
            } catch (Exception e) {
                System.err.println("ProfanityDetector unavailable: " + e.getMessage());
            }

            final boolean badWords = containsBadWords;
            Platform.runLater(() -> {
                if (badWords) {
                    showAlert("Refusé", "Commentaire refusé : langage inapproprié");
                } else {
                    // Créer la boîte du commentaire
                    VBox commentBox = new VBox();
                    commentBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                            "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
                    commentBox.setSpacing(5);

                    Label ratingLabel = new Label("⭐".repeat(rating));
                    ratingLabel.setStyle("-fx-font-size: 14px;");

                    Label commentLabel = new Label(commentText);
                    commentLabel.setWrapText(true);
                    commentLabel.setStyle("-fx-font-size: 13px;");

                    commentBox.getChildren().addAll(ratingLabel, commentLabel);

                    // Suggestion automatique (si disponible)
                    try {
                        String correctedText = SuggestionAPI.getCorrectedText(commentText);
                        if (correctedText != null && !correctedText.equals(commentText)) {
                            Label suggestionLabel = new Label("Phrase corrigée :\n" + correctedText);
                            suggestionLabel.setWrapText(true);
                            suggestionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
                            commentBox.getChildren().add(suggestionLabel);
                        }
                    } catch (Exception e) {
                        System.err.println("SuggestionAPI unavailable: " + e.getMessage());
                    }

                    if (commentsContainer != null) {
                        commentsContainer.getChildren().add(0, commentBox);
                    }

                    // Réinitialiser
                    rating = 0;
                    setupStars();
                    if (commentTextArea != null) commentTextArea.clear();

                    saveFeedback(commentText);
                }
            });
        }).start();
    }

    private void saveFeedback(String feedback) {
        // Sauvegarde (à compléter avec logique BD si nécessaire)
        System.out.println("Feedback sauvegardé : ⭐ " + rating + " — " + feedback);
    }
}
