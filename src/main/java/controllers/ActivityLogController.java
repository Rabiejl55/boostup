package controllers;

import entities.GUtilisateurs.User;
import javafx.application.Platform;
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
    @FXML private ToggleButton themeToggle;

    // ═══ Pagination ═══
    @FXML private Label pageInfoLabel;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private int currentPage = 0;
    private static final int ROWS_PER_PAGE = 15;

    private ObservableList<Map<String, Object>> allActivities = FXCollections.observableArrayList();
    private FilteredList<Map<String, Object>> filteredActivities;
    private ObservableList<Map<String, Object>> pagedData = FXCollections.observableArrayList();

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
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        activityTable.setItems(pagedData);

        // Setup ComboBox filtre (facultatif si non présent dans le FXML)
        if (filterTypeCombo != null) {
            filterTypeCombo.setItems(FXCollections.observableArrayList(
                    "Tous", "Connexions", "Échecs", "SMS", "Face ID", "Export PDF", "Déconnexions", "Inscriptions"
            ));
            filterTypeCombo.setValue("Tous");
            filterTypeCombo.setOnAction(e -> applyFilters());
        }

        // Recherche par email
        if (searchEmailField != null) {
            searchEmailField.textProperty().addListener((obs, old, val) -> applyFilters());
        }

        Platform.runLater(() -> {
            if (themeToggle != null && themeToggle.getScene() != null) {
                ThemeHelper.applyTheme(themeToggle.getScene(), themeToggle);
            }
        });

        // Charger les données
        loadActivities();
        loadStats();
    }

    private void loadActivities() {
        List<Map<String, Object>> activities = ActivityLogService.getRecentActivities(500);
        allActivities.setAll(activities);
        currentPage = 0;
        updatePagination();

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
        String typeFilter = filterTypeCombo != null ? filterTypeCombo.getValue() : "Tous";
        String emailSearch = searchEmailField != null ? searchEmailField.getText().trim().toLowerCase() : "";

        filteredActivities.setPredicate(activity -> {
            String actionType = (String) activity.get("action_type");
            String email = (String) activity.get("user_email");

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

            boolean emailMatch = emailSearch.isEmpty() ||
                    (email != null && email.toLowerCase().contains(emailSearch));

            return typeMatch && emailMatch;
        });

        currentPage = 0;
        updatePagination();

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

    // ══════════════════════════════════════════════════════════
    // PAGINATION
    // ══════════════════════════════════════════════════════════

    private void updatePagination() {
        if (filteredActivities == null) return;

        int totalItems = filteredActivities.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ROWS_PER_PAGE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int from = currentPage * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, totalItems);
        pagedData.setAll(filteredActivities.subList(from, to));

        if (pageInfoLabel != null)
            pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage == 0);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    private void goToPrevPage(ActionEvent event) {
        if (currentPage > 0) { currentPage--; updatePagination(); }
    }

    @FXML
    private void goToNextPage(ActionEvent event) {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredActivities.size() / ROWS_PER_PAGE));
        if (currentPage < totalPages - 1) { currentPage++; updatePagination(); }
    }

    // ══════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════

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

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }
}
