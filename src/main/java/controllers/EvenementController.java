package controllers;

import entities.GEvenement.Evenement;
import entities.GEvenement.EvenementFX;
import services.EvenementService.EvenementService;
import services.SmsService;
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
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LocalDateStringConverter;

public class EvenementController implements Initializable {

    // === CHAMPS DU FORMULAIRE ===
    @FXML private TextField tfTitre;
    @FXML private TextField tfType;
    @FXML private DatePicker dpDate;
    @FXML private TextField tfLieu;
    @FXML private TextArea taDescription;
    @FXML private TextField tfCapacite;
    @FXML private TextField tfImage;
    @FXML private TextField tfRecherche;

    // === TRI ===
    @FXML private ChoiceBox<String> cbTri;

    @FXML private Label statusLabel;

    // === TABLE VIEW ===
    @FXML private TableView<EvenementFX> tableEvenements;
    @FXML private TableColumn<EvenementFX, Integer> colId;
    @FXML private TableColumn<EvenementFX, String> colTitre;
    @FXML private TableColumn<EvenementFX, String> colType;
    @FXML private TableColumn<EvenementFX, Date> colDate;
    @FXML private TableColumn<EvenementFX, String> colLieu;
    @FXML private TableColumn<EvenementFX, Integer> colCapacite;
    @FXML private TableColumn<EvenementFX, String> colImage;

    // === BOUTONS ===
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnActualiser;
    @FXML private Button btnParticipation;
    @FXML private Button btnExportCsv;

    // === SERVICES & DONNÉES ===
    private EvenementService es = new EvenementService();
    private ObservableList<EvenementFX> evenementList = FXCollections.observableArrayList();
    private FilteredList<EvenementFX> filteredData = new FilteredList<>(evenementList, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ Initialisation du contrôleur...");

        if (statusLabel != null) {
            statusLabel.setText("Initialisation...");
        }

        // Contrôles de saisie (UI)
        setupInputConstraints();

        // Configuration des colonnes
        configureColumns();

        // NEW: édition directe (double-clic) dans la TableView
        setupInlineEditing();

        // Charger les données
        refreshTable();

        // Configuration de la recherche
        setupSearchFilter();

        // Configuration du tri (ChoiceBox)
        setupTriChoiceBox();

        // Désactiver boutons modification/suppression initialement
        // Option A: on désactive "Modifier" car l’édition se fait directement dans la TableView.
        if (btnModifier != null) {
            btnModifier.setDisable(true);
            btnModifier.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION,
                    "Modification",
                    "La modification se fait directement dans le tableau (double-clic sur une cellule)."));
        }
        btnSupprimer.setDisable(true);

        // Écouteur de sélection dans la table
        tableEvenements.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean itemSelected = (newValue != null);

                    // Option A: ne jamais réactiver le bouton Modifier
                    if (btnModifier != null) {
                        btnModifier.setDisable(true);
                    }
                    btnSupprimer.setDisable(!itemSelected);

                    if (itemSelected) {
                        fillFormWithEvenement(newValue);
                        if (statusLabel != null) {
                            statusLabel.setText("Sélection: " + newValue.getTitre() + " (modifiez via le tableau)");
                        }
                    }
                }
        );

        // Export CSV (connecté en code pour éviter les soucis de résolution FXML)
        if (btnExportCsv != null) {
            btnExportCsv.setOnAction(e -> handleExportCsv());
        }

        if (statusLabel != null) {
            statusLabel.setText("Prêt");
        }
        System.out.println("✅ Contrôleur initialisé avec succès");
    }

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEvenement"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        if (colImage != null) {
            colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        }
    }

    private void setupTriChoiceBox() {
        // Le user a ajouté le ChoiceBox dans le FXML ; on le rend fonctionnel sans toucher le reste de l’UI.
        if (cbTri == null) {
            System.err.println("⚠️ cbTri est null - vérifiez fx:id=\"cbTri\" dans le FXML");
            return;
        }

        // 3 choix demandés
        cbTri.setItems(FXCollections.observableArrayList("Titre", "Capacité", "Date"));
        cbTri.getSelectionModel().select("Titre");

        // Appliquer le tri immédiatement (au démarrage)
        applyTri("Titre");

        // Tri instantané au clic/changement
        cbTri.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyTri(newV));
    }

    private void applyTri(String choix) {
        if (choix == null || tableEvenements == null) {
            return;
        }

        // JavaFX: pas de TableView#setComparator().
        // On applique le tri via l’API de tri: sortOrder + sortType + table.sort().
        // Titre: A -> Z
        // Capacité: décroissant (max -> min)
        // Date: croissant (plus proche -> plus loin)

        tableEvenements.getSortOrder().clear();

        switch (choix.trim().toLowerCase()) {
            case "capacité":
            case "capacite":
                if (colCapacite != null) {
                    colCapacite.setSortType(TableColumn.SortType.DESCENDING);
                    tableEvenements.getSortOrder().add(colCapacite);
                }
                break;
            case "date":
                if (colDate != null) {
                    colDate.setSortType(TableColumn.SortType.ASCENDING);
                    tableEvenements.getSortOrder().add(colDate);
                }
                break;
            case "titre":
            default:
                if (colTitre != null) {
                    colTitre.setSortType(TableColumn.SortType.ASCENDING);
                    tableEvenements.getSortOrder().add(colTitre);
                }
                break;
        }

        // Déclenche le tri.
        tableEvenements.sort();

        if (statusLabel != null) {
            statusLabel.setText("Tri: " + choix);
        }
    }

    private void setupSearchFilter() {
        // VÉRIFICATION CRITIQUE : Vérifiez que tfRecherche n'est pas null
        if (tfRecherche == null) {
            System.err.println("⚠️ tfRecherche est null - le champ n'est pas lié au FXML");
            System.err.println("Vérifiez que dans votre FXML vous avez:");
            System.err.println("<TextField fx:id=\"tfRecherche\" ... />");
            return; // Sortir de la méthode si le champ n'existe pas
        }

        // Maintenant on peut l'utiliser en toute sécurité
        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(evenement -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (evenement.getTitre().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (evenement.getType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (evenement.getLieu().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        SortedList<EvenementFX> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableEvenements.comparatorProperty());
        tableEvenements.setItems(sortedData);

        System.out.println("✅ Filtre de recherche configuré");
    }

    // === HANDLERS DES BOUTONS ===

    @FXML
    private void handleAjouter() {
        System.out.println("➕ Bouton Ajouter cliqué");
        if (validerEtMarquerStatut()) {
            try {
                EvenementFX nouveauEvenementFX = createEvenementFXFromForm();
                Evenement nouvelEvenement = nouveauEvenementFX.toEvenement();

                es.ajouter(nouvelEvenement);
                refreshTable();
                clearForm();

                if (statusLabel != null) {
                    statusLabel.setText("✓ Événement ajouté");
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Événement ajouté avec succès!");

            } catch (SQLException e) {
                if (statusLabel != null) {
                    statusLabel.setText("✗ Erreur ajout");
                }
                showAlert(Alert.AlertType.ERROR, "Erreur SQL",
                        "Erreur lors de l'ajout: " + e.getMessage());
            } catch (NumberFormatException e) {
                if (statusLabel != null) {
                    statusLabel.setText("✗ Capacité invalide");
                }
                showAlert(Alert.AlertType.ERROR, "Erreur de format",
                        "La capacité doit être un nombre entier!");
            } catch (IllegalArgumentException e) {
                if (statusLabel != null) {
                    statusLabel.setText("✗ Validation");
                }
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private boolean validerEtMarquerStatut() {
        boolean ok = validateForm();
        if (!ok && statusLabel != null) {
            statusLabel.setText("✗ Formulaire invalide");
        }
        return ok;
    }

    @FXML
    private void handleModifier() {
        // Option A: la modification se fait inline dans la TableView.
        showAlert(Alert.AlertType.INFORMATION,
                "Modification",
                "La modification se fait directement dans le tableau (double-clic sur une cellule).\n\n" +
                        "Astuce: double-cliquez sur Titre/Type/Lieu/Capacité/Image/Date puis validez.");
    }

    @FXML
    private void handleSupprimer() {
        System.out.println("🗑️ Bouton Supprimer cliqué");
        EvenementFX selected = tableEvenements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Veuillez sélectionner un événement à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer l'événement");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer l'événement : " + selected.getTitre() + " ?");

        java.util.Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            // Annuler
            return;
        }

        try {
            // 🗑️ Suppression en base de données
            es.supprimer(selected.getId());

            // 📱 ENVOI SMS D'ANNULATION (si Twilio configuré)
            if (SmsService.estConfigurer()) {
                System.out.println("📱 Envoi du SMS d'annulation...");
                // Créer objet Evenement depuis EvenementFX pour passer au service SMS
                Evenement evt = new Evenement(
                    selected.getId(),
                    selected.getTitre(),
                    selected.getType(),
                    selected.getDateEvenement(), // Méthode correcte
                    selected.getLieu(),
                    selected.getDescription(),
                    selected.getCapaciteMax(), // Méthode correcte
                    selected.getImage()
                );
                SmsService.envoyerSmsAnnulation(evt);
            } else {
                System.out.println("⚠️ SMS non configuré - configure Twilio dans SmsService.java");
            }

            refreshTable();
            clearForm();

            if (statusLabel != null) {
                statusLabel.setText("✓ Événement supprimé");
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Événement supprimé avec succès!");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL",
                    "Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @FXML
    private void handleActualiser() {
        System.out.println("🔄 Bouton Actualiser cliqué");
        refreshTable();
        clearForm();
        if (tfRecherche != null) {
            tfRecherche.clear();
        }
        showAlert(Alert.AlertType.INFORMATION, "Actualisation",
                "Liste actualisée avec succès!");
    }

    @FXML
    private void handleOuvrirParticipation() {
        try {
            URL fxml = getClass().getResource("/ParticipationView.fxml");
            if (fxml == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "FXML introuvable: /ParticipationView.fxml (vérifie qu'il est bien dans src/main/resources)");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("Gestion des Participations - BoostUp");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir l'écran Participation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            URL fxml = getClass().getResource("/views/login.fxml");
            if (fxml == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "FXML introuvable: /views/login.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("BoostUp - Connexion");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir au login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoDashboard() {
        try {
            URL fxml = getClass().getResource("/views/AdminDashboardView.fxml");
            if (fxml == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "FXML introuvable: /views/AdminDashboardView.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("BoostUp Admin - Dashboard");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportCsv() {
        if (tableEvenements == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "TableView introuvable");
            return;
        }

        var items = tableEvenements.getItems();
        if (items == null || items.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Aucune donnée à exporter");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les événements (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName("evenements.csv");

        Stage stage = (Stage) tableEvenements.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("id_evenement;titre;type;date;lieu;capacite;image;description\n");
            for (EvenementFX e : items) {
                String date = e.getDateEvenement() == null ? "" : e.getDateEvenement().toString();
                w.write(e.getId() + ";" + safeCsv(e.getTitre()) + ";" + safeCsv(e.getType()) + ";" + date + ";" +
                        safeCsv(e.getLieu()) + ";" + e.getCapaciteMax() + ";" + safeCsv(e.getImage()) + ";" + safeCsv(e.getDescription()) + "\n");
            }
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Export CSV terminé: " + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter: " + ex.getMessage());
        }
    }

    private String safeCsv(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    // === MÉTHODES UTILITAIRES ===

    private void refreshTable() {
        try {
            evenementList.clear();

            for (Evenement e : es.readActifs()) {
                evenementList.add(new EvenementFX(e));
            }

            // IMPORTANT: s'assurer que la TableView pointe bien sur les données filtrées/triées actuelles
            // (évite l'impression "ça se modifie puis ça revient comme avant")
            if (tableEvenements != null) {
                if (tableEvenements.getItems() == null || tableEvenements.getItems().isEmpty()) {
                    // si setupSearchFilter n'a pas encore injecté le SortedList, on met la liste brute
                    tableEvenements.setItems(evenementList);
                } else {
                    // forcer un refresh visuel si on est déjà sur SortedList
                    tableEvenements.refresh();
                }
            }

            if (evenementList.isEmpty()) {
                System.out.println("ℹ️ Aucun événement trouvé dans la base");
                if (statusLabel != null) {
                    statusLabel.setText("Aucun événement trouvé");
                }
                showAlert(Alert.AlertType.INFORMATION, "Information",
                        "Aucun événement trouvé dans la base de données.");
            } else {
                System.out.println("✅ " + evenementList.size() + " événements chargés");
                if (statusLabel != null) {
                    statusLabel.setText("✓ " + evenementList.size() + " événement(s) chargé(s)");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors du chargement: " + e.getMessage());
            if (statusLabel != null) {
                statusLabel.setText("✗ Erreur chargement");
            }
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement: " + e.getMessage());
        }
    }

    private EvenementFX createEvenementFXFromForm() {
        String titre = tfTitre.getText();
        String type = tfType.getText();
        LocalDate ld = dpDate.getValue();
        String lieu = tfLieu.getText();
        String description = taDescription.getText();
        int capacite = Integer.parseInt(tfCapacite.getText());
        String image = tfImage == null ? null : tfImage.getText();

        Date date = (ld == null) ? null : Date.valueOf(ld);
        return new EvenementFX(titre, type, date, lieu, description, capacite, image);
    }

    private void fillFormWithEvenement(EvenementFX evenement) {
        if (evenement == null) return;
        tfTitre.setText(evenement.getTitre());
        tfType.setText(evenement.getType());
        if (evenement.getDateEvenement() != null) {
            dpDate.setValue(evenement.getDateEvenement().toLocalDate());
        } else {
            dpDate.setValue(null);
        }
        tfLieu.setText(evenement.getLieu());
        taDescription.setText(evenement.getDescription());
        tfCapacite.setText(String.valueOf(evenement.getCapaciteMax()));
        if (tfImage != null) {
            tfImage.setText(evenement.getImage() == null ? "" : evenement.getImage());
        }
    }

    private void updateEvenementFXFromForm(EvenementFX evenement) {
        evenement.setTitre(tfTitre.getText());
        evenement.setType(tfType.getText());

        LocalDate date = dpDate.getValue();
        if (date != null) {
            evenement.setDateEvenement(Date.valueOf(date));
        }

        evenement.setLieu(tfLieu.getText());
        evenement.setDescription(taDescription.getText());
        evenement.setCapaciteMax(Integer.parseInt(tfCapacite.getText()));
        if (tfImage != null) {
            evenement.setImage(tfImage.getText());
        }
    }

    private void setupInputConstraints() {
        // Capacité: n'autoriser que des chiffres, max 3 caractères (0-250 géré en validation)
        if (tfCapacite != null) {
            UnaryOperator<TextFormatter.Change> digitsOnly = change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty()) return change;
                if (!newText.matches("\\d{0,3}")) return null;
                return change;
            };
            tfCapacite.setTextFormatter(new TextFormatter<>(digitsOnly));
        }

        // Titre/Type/Lieu: on laisse libre (lettres + chiffres possibles), mais on nettoie les espaces multiples
        // La règle "pas que des nombres" est appliquée dans validateForm().
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        String type = tfType.getText() == null ? "" : tfType.getText().trim();
        String lieu = tfLieu.getText() == null ? "" : tfLieu.getText().trim();
        String capaciteText = tfCapacite.getText() == null ? "" : tfCapacite.getText().trim();

        if (titre.isEmpty()) {
            errors.append("- Le titre est obligatoire\n");
        } else if (titre.matches("\\d+")) {
            errors.append("- Le titre ne peut pas contenir uniquement des chiffres\n");
        }

        if (type.isEmpty()) {
            errors.append("- Le type est obligatoire\n");
        } else if (type.matches("\\d+")) {
            errors.append("- Le type ne peut pas contenir uniquement des chiffres\n");
        }

        if (dpDate.getValue() == null) {
            errors.append("- La date est obligatoire\n");
        } else {
            // Date de l'événement doit être future
            if (!dpDate.getValue().isAfter(java.time.LocalDate.now())) {
                errors.append("- La date de l'événement doit être supérieure à la date d'aujourd'hui\n");
            }
        }

        if (lieu.isEmpty()) {
            errors.append("- Le lieu est obligatoire\n");
        } else if (lieu.length() < 3) {
            errors.append("- Le lieu doit contenir au minimum 3 caractères\n");
        }

        if (capaciteText.isEmpty()) {
            errors.append("- La capacité est obligatoire\n");
        } else {
            try {
                int cap = Integer.parseInt(capaciteText);
                if (cap <= 0) {
                    errors.append("- La capacité doit être supérieure à 0\n");
                }
                if (cap > 250) {
                    errors.append("- La capacité maximale doit être inférieure ou égale à 250\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- La capacité doit être un nombre entier\n");
            }
        }

        // Description: aucune contrainte demandée

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Validation du formulaire",
                    "Veuillez corriger les erreurs suivantes:\n\n" + errors);
            return false;
        }

        // Remettre les textes trimés (petit plus UX)
        tfTitre.setText(titre);
        tfType.setText(type);
        tfLieu.setText(lieu);

        return true;
    }

    private void clearForm() {
        if (tfTitre != null) tfTitre.clear();
        if (tfType != null) tfType.clear();
        if (dpDate != null) dpDate.setValue(null);
        if (tfLieu != null) tfLieu.clear();
        if (taDescription != null) taDescription.clear();
        if (tfCapacite != null) tfCapacite.clear();
        if (tfImage != null) tfImage.clear();
        tableEvenements.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupInlineEditing() {
        if (tableEvenements == null) return;
        tableEvenements.setEditable(true);

        // Titre
        if (colTitre != null) {
            colTitre.setEditable(true);
            colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
            colTitre.setOnEditCommit(ev -> {
                EvenementFX row = ev.getRowValue();
                if (row == null) return;
                String newV = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
                if (newV.isEmpty() || newV.matches("\\d+")) {
                    showAlert(Alert.AlertType.ERROR, "Validation", "Titre invalide (obligatoire, pas uniquement des chiffres). ");
                    tableEvenements.refresh();
                    return;
                }
                String old = row.getTitre();
                row.setTitre(newV);
                try {
                    es.update(row.toEvenement());
                    setStatusOk("✓ Modifié: Titre");
                } catch (SQLException ex) {
                    row.setTitre(old);
                    tableEvenements.refresh();
                    showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier le titre: " + ex.getMessage());
                }
            });
        }

        // Type
        if (colType != null) {
            colType.setEditable(true);
            colType.setCellFactory(TextFieldTableCell.forTableColumn());
            colType.setOnEditCommit(ev -> {
                EvenementFX row = ev.getRowValue();
                if (row == null) return;
                String newV = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
                if (newV.isEmpty() || newV.matches("\\d+")) {
                    showAlert(Alert.AlertType.ERROR, "Validation", "Type invalide (obligatoire, pas uniquement des chiffres). ");
                    tableEvenements.refresh();
                    return;
                }
                String old = row.getType();
                row.setType(newV);
                try {
                    es.update(row.toEvenement());
                    setStatusOk("✓ Modifié: Type");
                } catch (SQLException ex) {
                    row.setType(old);
                    tableEvenements.refresh();
                    showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier le type: " + ex.getMessage());
                }
            });
        }

        // Lieu
        if (colLieu != null) {
            colLieu.setEditable(true);
            colLieu.setCellFactory(TextFieldTableCell.forTableColumn());
            colLieu.setOnEditCommit(ev -> {
                EvenementFX row = ev.getRowValue();
                if (row == null) return;
                String newV = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
                if (newV.length() < 3) {
                    showAlert(Alert.AlertType.ERROR, "Validation", "Lieu invalide (minimum 3 caractères). ");
                    tableEvenements.refresh();
                    return;
                }
                String old = row.getLieu();
                row.setLieu(newV);
                try {
                    es.update(row.toEvenement());
                    setStatusOk("✓ Modifié: Lieu");
                } catch (SQLException ex) {
                    row.setLieu(old);
                    tableEvenements.refresh();
                    showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier le lieu: " + ex.getMessage());
                }
            });
        }

        // Capacité (int, <= 250)
        if (colCapacite != null) {
            colCapacite.setEditable(true);
            IntegerStringConverter intConv = new IntegerStringConverter();
            colCapacite.setCellFactory(TextFieldTableCell.forTableColumn(intConv));
            colCapacite.setOnEditCommit(ev -> {
                EvenementFX row = ev.getRowValue();
                if (row == null) return;
                Integer newV = ev.getNewValue();
                if (newV == null) {
                    showAlert(Alert.AlertType.ERROR, "Validation", "Capacité invalide.");
                    tableEvenements.refresh();
                    return;
                }
                if (newV < 1 || newV > 250) {
                    showAlert(Alert.AlertType.ERROR, "Validation", "Capacité invalide (1..250). ");
                    tableEvenements.refresh();
                    return;
                }
                int old = row.getCapaciteMax();
                row.setCapaciteMax(newV);
                try {
                    es.update(row.toEvenement());
                    setStatusOk("✓ Modifié: Capacité");
                } catch (SQLException ex) {
                    row.setCapaciteMax(old);
                    tableEvenements.refresh();
                    showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier la capacité: " + ex.getMessage());
                }
            });
        }

        // Image (URL ou /images/...) - pas de validation stricte ici
        if (colImage != null) {
            colImage.setEditable(true);
            colImage.setCellFactory(TextFieldTableCell.forTableColumn());
            colImage.setOnEditCommit(ev -> {
                EvenementFX row = ev.getRowValue();
                if (row == null) return;
                String newV = ev.getNewValue();
                String old = row.getImage();
                row.setImage(newV == null ? null : newV.trim());
                try {
                    es.update(row.toEvenement());
                    setStatusOk("✓ Modifié: Image");
                } catch (SQLException ex) {
                    row.setImage(old);
                    tableEvenements.refresh();
                    showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier l'image: " + ex.getMessage());
                }
            });
        }

        // Date (via DatePicker dans la cellule)
        if (colDate != null) {
            colDate.setEditable(true);
            colDate.setCellFactory(column -> new DateEditingCell());
        }
    }

    /**
     * Cellule editable pour DatePicker (colonne Date): commit direct en DB.
     */
    private class DateEditingCell extends TableCell<EvenementFX, Date> {
        private final DatePicker picker = new DatePicker();

        DateEditingCell() {
            picker.setOnAction(e -> commitFromPicker());
            picker.focusedProperty().addListener((obs, old, foc) -> {
                if (!foc && isEditing()) {
                    commitFromPicker();
                }
            });
        }

        private void commitFromPicker() {
            LocalDate ld = picker.getValue();
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
            EvenementFX row = getTableRow() == null ? null : getTableRow().getItem();
            if (row == null || newValue == null) {
                super.commitEdit(newValue);
                return;
            }

            Date old = row.getDateEvenement();
            row.setDateEvenement(newValue);
            try {
                es.update(row.toEvenement());
                setStatusOk("✓ Modifié: Date");
                super.commitEdit(newValue);
            } catch (SQLException ex) {
                row.setDateEvenement(old);
                tableEvenements.refresh();
                showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible de modifier la date: " + ex.getMessage());
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

    private void setStatusOk(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
        }
    }
}
