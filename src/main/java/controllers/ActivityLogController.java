package controllers;

import entities.GUtilisateurs.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UtilisateurService.ActivityLogService;

import java.util.List;
import java.util.Map;

public class ActivityLogController {

    @FXML private TableView<Map<String, Object>> activityTable;
    @FXML private TableColumn<Map<String, Object>, String> colIcon;
    @FXML private TableColumn<Map<String, Object>, String> colAction;
    @FXML private TableColumn<Map<String, Object>, String> colEmail;
    @FXML private TableColumn<Map<String, Object>, String> colDescription;
    @FXML private TableColumn<Map<String, Object>, String> colDate;

    @FXML private Label subtitleLabel;
    @FXML private Label statLoginsLabel;
    @FXML private Label statFailedLabel;
    @FXML private Label statTotalLabel;
    @FXML private Label statFaceIdLabel;

    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField searchEmailField;

    private ObservableList<Map<String, Object>> allActivities = FXCollections.observableArrayList();
    private FilteredList<Map<String, Object>> filteredActivities;

    @FXML
    public void initialize() {
        // Setup colonnes
        colIcon.setCellValueFactory(data -> {
            String type = (String) data.getValue().get("action_type");
            return new SimpleStringProperty(ActivityLogService.getActionEmoji(type));
        });
        colIcon.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px;");

        colAction.setCellValueFactory(data -> {
            String type = (String) data.getValue().get("action_type");
            return new SimpleStringProperty(ActivityLogService.formatActionDescription(type));
        });

        colEmail.setCellValueFactory(data -> {
            String email = (String) data.getValue().get("user_email");
            return new SimpleStringProperty(email != null ? email : "Anonyme");
        });

        colDescription.setCellValueFactory(data -> {
            String desc = (String) data.getValue().get("description");
            return new SimpleStringProperty(desc != null ? desc : "—");
        });

        colDate.setCellValueFactory(data -> {
            Object val = data.getValue().get("created_at");
            return new SimpleStringProperty(val != null ? val.toString() : "—");
        });

        // Style des cellules avec couleur selon le type d'action
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

                    Map<String, Object> row = getTableView().getItems().get(getIndex());
                    String type = (String) row.get("action_type");
                    if (type != null) {
                        switch (type) {
                            case "LOGIN":
                            case "FACE_AUTH":
                                setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #2ecc71;");
                                break;
                            case "LOGIN_FAILED":
                            case "FACE_AUTH_FAILED":
                                setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
                                break;
                            case "EXPORT_PDF":
                                setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #0d6efd;");
                                break;
                            case "SMS_SENT":
                                setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #f59e0b;");
                                break;
                            default:
                                setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #6c757d;");
                        }
                    }
                }
            }
        });

        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");
                }
            }
        });

        // Filtrage
        filteredActivities = new FilteredList<>(allActivities, p -> true);
        activityTable.setItems(filteredActivities);

        // Setup ComboBox filtre
        filterTypeCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Connexions", "Échecs", "SMS", "Face ID", "Export PDF", "Déconnexions", "Inscriptions"
        ));
        filterTypeCombo.setValue("Tous");
        filterTypeCombo.setOnAction(e -> applyFilters());

        // Recherche par email
        if (searchEmailField != null) {
            searchEmailField.textProperty().addListener((obs, old, val) -> applyFilters());
        }

        // Charger les données
        loadActivities();
        loadStats();
    }

    private void loadActivities() {
        List<Map<String, Object>> activities = ActivityLogService.getRecentActivities(100);
        allActivities.setAll(activities);

        if (subtitleLabel != null) {
            subtitleLabel.setText(activities.size() + " activités enregistrées");
        }
    }

    private void loadStats() {
        Map<String, Integer> stats = ActivityLogService.getActivityStats();

        int logins = stats.getOrDefault("LOGIN", 0);
        int failed = stats.getOrDefault("LOGIN_FAILED", 0);
        int faceAuth = stats.getOrDefault("FACE_AUTH", 0) + stats.getOrDefault("FACE_AUTH_FAILED", 0);
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();

        if (statLoginsLabel != null) statLoginsLabel.setText(String.valueOf(logins));
        if (statFailedLabel != null) statFailedLabel.setText(String.valueOf(failed));
        if (statTotalLabel != null) statTotalLabel.setText(String.valueOf(total));
        if (statFaceIdLabel != null) statFaceIdLabel.setText(String.valueOf(faceAuth));
    }

    private void applyFilters() {
        String typeFilter = filterTypeCombo.getValue();
        String emailSearch = searchEmailField != null ? searchEmailField.getText().trim().toLowerCase() : "";

        filteredActivities.setPredicate(activity -> {
            String actionType = (String) activity.get("action_type");
            String email = (String) activity.get("user_email");

            // Filtre par type
            boolean typeMatch = true;
            if (typeFilter != null && !"Tous".equals(typeFilter)) {
                switch (typeFilter) {
                    case "Connexions": typeMatch = "LOGIN".equals(actionType); break;
                    case "Échecs": typeMatch = actionType != null && actionType.contains("FAILED"); break;
                    case "SMS": typeMatch = "SMS_SENT".equals(actionType); break;
                    case "Face ID": typeMatch = actionType != null && actionType.contains("FACE"); break;
                    case "Export PDF": typeMatch = "EXPORT_PDF".equals(actionType); break;
                    case "Déconnexions": typeMatch = "LOGOUT".equals(actionType); break;
                    case "Inscriptions": typeMatch = "SIGNUP".equals(actionType); break;
                }
            }

            // Filtre par email
            boolean emailMatch = emailSearch.isEmpty() ||
                    (email != null && email.toLowerCase().contains(emailSearch));

            return typeMatch && emailMatch;
        });

        if (subtitleLabel != null) {
            subtitleLabel.setText(filteredActivities.size() + " activités affichées");
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadActivities();
        loadStats();
    }

    @FXML
    private void handlePurgeLogs(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Purger les logs");
        confirm.setHeaderText("Supprimer les logs de plus de 30 jours ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int deleted = ActivityLogService.purgeOldLogs(30);
                loadActivities();
                loadStats();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Purge terminée");
                info.setHeaderText(null);
                info.setContentText(deleted + " anciens logs supprimés.");
                info.showAndWait();
            }
        });
    }

    @FXML
    private void goBack(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        String path = (user != null && user.getRole() == entities.Role_enum.ADMIN)
                ? "/fxml/admin-dashboard.fxml" : "/fxml/homepage.fxml";
        String title = (user != null && user.getRole() == entities.Role_enum.ADMIN)
                ? "Dashboard Admin" : "Accueil";

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, path, title);
    }
}




