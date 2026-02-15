package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import services.UtilisateurService.UserService;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UsersManagementController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;
    @FXML private TableColumn<User, java.sql.Timestamp> dateCreationColumn;
    @FXML private Label feedbackLabel;
    @FXML private Label filterSummaryLabel;  // Pour le résumé des filtres
    @FXML private Label userCountLabel;      // Pour le compteur d'utilisateurs
    @FXML
    private ImageView avatarImageView;

    private final UserService userService = new UserService();
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    // New FXML fields for search and filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<Role_enum> roleFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button clearFiltersButton;

    private FilteredList<User> filteredData;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^[+]?[0-9\\s.-]{8,20}$");
    private static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-z0-9À-ÖØ-öø-ÿ\\s'-]{2,50}$");

    @FXML
    public void initialize() {
        // Configurer les colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getRole() != null ? cell.getValue().getRole().name() : "N/A"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        // Formatter la date
        dateCreationColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(java.sql.Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                            .format(new java.util.Date(item.getTime())));
                }
            }
        });

        // Personnaliser l'affichage de la colonne active
        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText("");
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "Actif" : "Inactif");
                    badge.getStyleClass().addAll("badge", active ? "badge-success" : "badge-danger");
                    setGraphic(badge);
                }
            }
        });

        // Personnaliser l'affichage du rôle
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText("");
                    setGraphic(null);
                } else {
                    Label badge = new Label(role);
                    badge.getStyleClass().addAll("badge", "role-" + role.toLowerCase());
                    setGraphic(badge);
                }
            }
        });
        initializeFilters();
        usersTable.setItems(usersList);
        refreshUsers();
        loadAvatar();
    }

    private void loadAvatar() {
        if (avatarImageView == null) return;

        User user = SessionManager.getCurrentUser();
        String avatarUrl = user != null ? user.getAvatar() : null;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                // Clean URL
                if (avatarUrl.contains(" ")) avatarUrl = avatarUrl.replace(" ", "%20");
                if (avatarUrl.startsWith("C:/") || avatarUrl.startsWith("D:/")) {
                    avatarUrl = "file:///" + avatarUrl.replace("\\", "/");
                }

                // Load image with proper dimensions (80x80)
                Image image = new Image(avatarUrl, 80, 80, true, true, true);

                if (!image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setPreserveRatio(true);
                    avatarImageView.setSmooth(true);
                    avatarImageView.setFitWidth(80);
                    avatarImageView.setFitHeight(80);

                    // Create circular clip - radius 40 for 80px image
                    Circle clip = new Circle(40, 40, 40);
                    avatarImageView.setClip(clip);
                    avatarImageView.setVisible(true);
                    return;
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement avatar: " + e.getMessage());
            }
        }

        // Default avatar if none
        setDefaultAvatar();
    }

    private void setDefaultAvatar() {
        if (avatarImageView != null) {
            User user = SessionManager.getCurrentUser();

            // Create gradient color based on user ID or name
            Color color1, color2;
            if (user != null) {
                int hash = user.getNom().hashCode();
                double hue = Math.abs(hash % 360);
                color1 = Color.hsb(hue, 0.7, 0.9);
                color2 = Color.hsb((hue + 30) % 360, 0.8, 0.8);
            } else {
                color1 = Color.web("#0d6efd");
                color2 = Color.web("#6f42c1");
            }

            // Create gradient image
            WritableImage image = new WritableImage(80, 80);
            PixelWriter writer = image.getPixelWriter();

            for (int y = 0; y < 80; y++) {
                for (int x = 0; x < 80; x++) {
                    double ratio = (double)(x + y) / (160.0);
                    Color mixed = color1.interpolate(color2, ratio);
                    writer.setColor(x, y, mixed);
                }
            }

            avatarImageView.setImage(image);
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setFitWidth(80);
            avatarImageView.setFitHeight(80);

            Circle clip = new Circle(40, 40, 40);
            avatarImageView.setClip(clip);
            avatarImageView.setVisible(true);
        }
    }
    private void initializeFilters() {
        // Setup search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Setup filter comboboxes
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Tous les champs",
                "Nom",
                "Email",
                "Téléphone"
        ));
        filterComboBox.getSelectionModel().selectFirst();
        filterComboBox.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        // Setup role filter
        roleFilterComboBox.setItems(FXCollections.observableArrayList(Role_enum.values()));
        roleFilterComboBox.getSelectionModel().selectFirst();
        roleFilterComboBox.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        // Setup status filter
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Tous les statuts",
                "Actifs uniquement",
                "Inactifs uniquement"
        ));
        statusFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }


    private void refreshUsers() {
        try {
            usersList.clear();
            usersList.addAll(userService.read());
            // Initialize filtered list
            filteredData = new FilteredList<>(usersList, p -> true);
            usersTable.setItems(filteredData);
            showFeedback("Liste rafraîchie (" + usersList.size() + " utilisateurs)", "success");
        } catch (SQLException e) {
            showFeedback("Erreur chargement : " + e.getMessage(), "error");
        }
    }
    private void applyFilters() {
        if (filteredData == null) return;

        String searchText = searchField.getText().toLowerCase();
        String searchField = filterComboBox.getValue();
        Role_enum selectedRole = roleFilterComboBox.getValue();
        String statusFilter = statusFilterComboBox.getValue();

        filteredData.setPredicate(user -> {
            // Filtre par rôle
            if (selectedRole != null && user.getRole() != selectedRole) {
                return false;
            }

            // Filtre par statut
            if (statusFilter != null) {
                if ("Actifs uniquement".equals(statusFilter) && !user.isActive()) {
                    return false;
                }
                if ("Inactifs uniquement".equals(statusFilter) && user.isActive()) {
                    return false;
                }
            }

            // Filtre par recherche textuelle
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            // Recherche dans le champ spécifié
            switch (searchField) {
                case "Nom":
                    return user.getNom() != null && user.getNom().toLowerCase().contains(searchText);
                case "Email":
                    return user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText);
                case "Téléphone":
                    return user.getPhone() != null && user.getPhone().toLowerCase().contains(searchText);
                case "Nom complet":
                    return user.getFullname() != null && user.getFullname().toLowerCase().contains(searchText);
                case "Tous les champs":
                default:
                    return (user.getNom() != null && user.getNom().toLowerCase().contains(searchText)) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText)) ||
                            (user.getFullname() != null && user.getFullname().toLowerCase().contains(searchText)) ||
                            (user.getPhone() != null && user.getPhone().toLowerCase().contains(searchText)) ||
                            (user.getRole() != null && user.getRole().name().toLowerCase().contains(searchText));
            }
        });

        // Update feedback with filter results
        int filteredCount = filteredData.size();
        int totalCount = usersList.size();
        if (filteredCount < totalCount) {
            showFeedback(filteredCount + " utilisateur(s) trouvé(s) sur " + totalCount, "info");
        } else {
            showFeedback(totalCount + " utilisateur(s) au total", "success");
        }
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        searchField.clear();
        filterComboBox.getSelectionModel().selectFirst();
        roleFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();
        applyFilters();
        showFeedback("Filtres réinitialisés", "success");
    }


    @FXML
    private void refreshUsers(ActionEvent event) {
        refreshUsers();
    }

    @FXML
    private void showAddUserDialog(ActionEvent event) {
        Dialog<User> dialog = createUserDialog(null);
        dialog.setTitle("Ajouter un utilisateur");
        dialog.showAndWait().ifPresent(user -> {
            try {
                validateUser(user, true);
                userService.ajouter(user);
                refreshUsers();
                showFeedback("Utilisateur ajouté avec succès !", "success");
            } catch (SQLException e) {
                if (e.getMessage().contains("email")) {
                    showFeedback("Cette adresse email est déjà utilisée !", "error");
                } else {
                    showFeedback("Erreur ajout : " + e.getMessage(), "error");
                }
            } catch (IllegalArgumentException e) {
                showFeedback(e.getMessage(), "error");
            }
        });
    }


    @FXML
    private void showEditUserDialog(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Sélectionnez un utilisateur à modifier", "error");
            return;
        }

        Dialog<User> dialog = createUserDialog(selected);
        dialog.setTitle("Modifier l'utilisateur");
        dialog.showAndWait().ifPresent(user -> {
            try {
                validateUser(user, false);
                userService.update(user);
                refreshUsers();
                showFeedback("Utilisateur modifié avec succès !", "success");
            } catch (SQLException e) {
                if (e.getMessage().contains("email")) {
                    showFeedback("Cette adresse email est déjà utilisée !", "error");
                } else {
                    showFeedback("Erreur modification : " + e.getMessage(), "error");
                }
            } catch (IllegalArgumentException e) {
                showFeedback(e.getMessage(), "error");
            }
        });
    }

    @FXML
    private void deleteSelectedUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Sélectionnez un utilisateur à supprimer", "error");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer utilisateur");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer l'utilisateur :\n" +
                "• " + selected.getNom() + "\n" +
                "• " + selected.getEmail() + "\n\n" +
                "Cette action est irréversible !");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.supprimer(selected.getId());
                    refreshUsers();
                    showFeedback("Utilisateur supprimé avec succès !", "success");
                } catch (SQLException e) {
                    showFeedback("Erreur suppression : " + e.getMessage(), "error");
                }
            }
        });
    }

    private Dialog<User> createUserDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter utilisateur" : "Modifier utilisateur");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.setPrefWidth(500);
        dialogPane.setPrefHeight(650);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setStyle("-fx-background-color: white;");

        int rowIndex = 0;

        // === Nom d'utilisateur ===
        Label nomLabel = new Label("Nom d'utilisateur *:");
        nomLabel.getStyleClass().add("form-label");
        grid.add(nomLabel, 0, rowIndex);

        TextField nomField = new TextField(existing != null ? existing.getNom() : "");
        nomField.setPromptText("Nom d'utilisateur");
        nomField.getStyleClass().add("modern-input");
        nomField.setPrefWidth(300);
        grid.add(nomField, 1, rowIndex);
        rowIndex++;

        Label nomError = new Label();
        nomError.getStyleClass().add("error-label");
        nomError.setWrapText(true);
        nomError.setVisible(false);
        nomError.setManaged(false);
        grid.add(nomError, 1, rowIndex);
        rowIndex++;

        // === Nom complet ===
        Label fullnameLabel = new Label("Nom complet :");
        fullnameLabel.getStyleClass().add("form-label");
        grid.add(fullnameLabel, 0, rowIndex);

        TextField fullnameField = new TextField(existing != null ? existing.getFullname() : "");
        fullnameField.setPromptText("Nom complet");
        fullnameField.getStyleClass().add("modern-input");
        fullnameField.setPrefWidth(300);
        grid.add(fullnameField, 1, rowIndex);
        rowIndex++;

        Label fullnameError = new Label();
        fullnameError.getStyleClass().add("error-label");
        fullnameError.setWrapText(true);
        fullnameError.setVisible(false);
        fullnameError.setManaged(false);
        grid.add(fullnameError, 1, rowIndex);
        rowIndex++;

        // === Email ===
        Label emailLabel = new Label("Email *:");
        emailLabel.getStyleClass().add("form-label");
        grid.add(emailLabel, 0, rowIndex);

        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        emailField.setPromptText("email@exemple.com");
        emailField.getStyleClass().add("modern-input");
        emailField.setPrefWidth(300);
        grid.add(emailField, 1, rowIndex);
        rowIndex++;

        Label emailError = new Label();
        emailError.getStyleClass().add("error-label");
        emailError.setWrapText(true);
        emailError.setVisible(false);
        emailError.setManaged(false);
        grid.add(emailError, 1, rowIndex);
        rowIndex++;

        // === Téléphone ===
        Label phoneLabel = new Label("Téléphone :");
        phoneLabel.getStyleClass().add("form-label");
        grid.add(phoneLabel, 0, rowIndex);

        TextField phoneField = new TextField(existing != null ? existing.getPhone() : "");
        phoneField.setPromptText("+33 1 23 45 67 89 (optionnel)");
        phoneField.getStyleClass().add("modern-input");
        phoneField.setPrefWidth(300);
        grid.add(phoneField, 1, rowIndex);
        rowIndex++;

        Label phoneError = new Label();
        phoneError.getStyleClass().add("error-label");
        phoneError.setWrapText(true);
        phoneError.setVisible(false);
        phoneError.setManaged(false);
        grid.add(phoneError, 1, rowIndex);
        rowIndex++;

        // === Mot de passe ===
        Label mdpLabel = new Label(existing == null ? "Mot de passe *:" : "Nouveau mot de passe :");
        mdpLabel.getStyleClass().add("form-label");
        grid.add(mdpLabel, 0, rowIndex);

        PasswordField mdpField = new PasswordField();
        mdpField.setPromptText(existing == null ? "Mot de passe requis" : "Laisser vide pour conserver");
        mdpField.getStyleClass().add("modern-input");
        mdpField.setPrefWidth(300);
        grid.add(mdpField, 1, rowIndex);
        rowIndex++;

        Label mdpError = new Label();
        mdpError.getStyleClass().add("error-label");
        mdpError.setWrapText(true);
        mdpError.setVisible(false);
        mdpError.setManaged(false);
        grid.add(mdpError, 1, rowIndex);
        rowIndex++;

        // === Confirmation mot de passe ===
        Label mdpConfirmLabel = new Label("Confirmation :");
        mdpConfirmLabel.getStyleClass().add("form-label");
        grid.add(mdpConfirmLabel, 0, rowIndex);

        PasswordField mdpConfirmField = new PasswordField();
        mdpConfirmField.setPromptText("Confirmer le mot de passe");
        mdpConfirmField.getStyleClass().add("modern-input");
        mdpConfirmField.setPrefWidth(300);
        grid.add(mdpConfirmField, 1, rowIndex);
        rowIndex++;

        Label mdpConfirmError = new Label();
        mdpConfirmError.getStyleClass().add("error-label");
        mdpConfirmError.setWrapText(true);
        mdpConfirmError.setVisible(false);
        mdpConfirmError.setManaged(false);
        grid.add(mdpConfirmError, 1, rowIndex);
        rowIndex++;

        // === Rôle ===
        Label roleLabel = new Label("Rôle *:");
        roleLabel.getStyleClass().add("form-label");
        grid.add(roleLabel, 0, rowIndex);

        ComboBox<Role_enum> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList(Role_enum.values()));
        roleCombo.setValue(existing != null ? existing.getRole() : Role_enum.INVESTISSEUR);
        roleCombo.getStyleClass().add("modern-input");
        roleCombo.setPrefWidth(300);
        grid.add(roleCombo, 1, rowIndex);
        rowIndex++;

        Label roleError = new Label();
        roleError.getStyleClass().add("error-label");
        roleError.setWrapText(true);
        roleError.setVisible(false);
        roleError.setManaged(false);
        grid.add(roleError, 1, rowIndex);
        rowIndex++;

        // === Statut ===
        Label activeLabel = new Label("Statut :");
        activeLabel.getStyleClass().add("form-label");
        grid.add(activeLabel, 0, rowIndex);

        CheckBox activeCheck = new CheckBox("Actif");
        activeCheck.setSelected(existing == null || existing.isActive());
        grid.add(activeCheck, 1, rowIndex);
        rowIndex++;

        dialogPane.setContent(grid);

        // === VALIDATION EN TEMPS RÉEL ===
        boolean isNewUser = (existing == null);

        // Validation nom
        nomField.textProperty().addListener((obs, old, newVal) ->
                validateNom(nomField, nomError, newVal));

        // Validation email
        emailField.textProperty().addListener((obs, old, newVal) ->
                validateEmail(emailField, emailError, newVal));

        // Validation téléphone
        phoneField.textProperty().addListener((obs, old, newVal) ->
                validatePhone(phoneField, phoneError, newVal));

        // Validation mot de passe
        mdpField.textProperty().addListener((obs, old, newVal) -> {
            validatePassword(mdpField, mdpError, mdpConfirmField, mdpConfirmError, newVal, isNewUser);
            validatePasswordConfirmation(mdpField, mdpConfirmField, mdpConfirmError, isNewUser);
        });

        // Validation confirmation
        mdpConfirmField.textProperty().addListener((obs, old, newVal) ->
                validatePasswordConfirmation(mdpField, mdpConfirmField, mdpConfirmError, isNewUser));

        // Validation rôle
        roleCombo.valueProperty().addListener((obs, old, newVal) ->
                validateRole(roleCombo, roleError, newVal));

        // Désactiver le bouton OK si validation échoue
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        okButton.getStyleClass().add("action-button");

        // Vérifier périodiquement la validation
        Runnable validateForm = () -> {
            boolean isValid = true;

            isValid &= validateNom(nomField, nomError, nomField.getText());
            isValid &= validateEmail(emailField, emailError, emailField.getText());
            isValid &= validatePhone(phoneField, phoneError, phoneField.getText());
            isValid &= validateRole(roleCombo, roleError, roleCombo.getValue());

            if (isNewUser) {
                isValid &= validatePassword(mdpField, mdpError, mdpConfirmField, mdpConfirmError,
                        mdpField.getText(), true);
                isValid &= validatePasswordConfirmation(mdpField, mdpConfirmField, mdpConfirmError, true);
            } else {
                // Pour modification, valider seulement si un mot de passe est fourni
                if (!mdpField.getText().isEmpty()) {
                    isValid &= validatePassword(mdpField, mdpError, mdpConfirmField, mdpConfirmError,
                            mdpField.getText(), false);
                    isValid &= validatePasswordConfirmation(mdpField, mdpConfirmField, mdpConfirmError, false);
                }
            }

            okButton.setDisable(!isValid);
        };

        nomField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        emailField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        phoneField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        mdpField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        mdpConfirmField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        roleCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

        // Validation initiale pour les nouveaux utilisateurs
        if (isNewUser) {
            validateForm.run();
        } else {
            okButton.setDisable(false);
        }

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                User user = existing != null ? existing : new User();
                user.setNom(nomField.getText().trim());
                user.setFullname(fullnameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

                if (!mdpField.getText().isEmpty()) {
                    user.setMDP(mdpField.getText());
                } else if (isNewUser) {
                    throw new IllegalArgumentException("Le mot de passe est requis");
                }

                user.setRole(roleCombo.getValue());
                user.setActive(activeCheck.isSelected());
                return user;
            }
            return null;
        });

        return dialog;
    }

    // ==================== MÉTHODES DE VALIDATION ====================

    private boolean validateNom(TextField field, Label errorLabel, String value) {
        if (value == null || value.trim().isEmpty()) {
            showFieldError(field, errorLabel, "Le nom d'utilisateur est requis");
            return false;
        }

        String trimmed = value.trim();
        if (trimmed.length() < 3) {
            showFieldError(field, errorLabel, "Minimum 3 caractères");
            return false;
        }
        if (trimmed.length() > 50) {
            showFieldError(field, errorLabel, "Maximum 50 caractères");
            return false;
        }
        if (!NAME_REGEX.matcher(trimmed).matches()) {
            showFieldError(field, errorLabel, "Caractères invalides (lettres, chiffres, espaces, apostrophes, tirets)");
            return false;
        }

        clearFieldError(field, errorLabel);
        return true;
    }

    private boolean validateEmail(TextField field, Label errorLabel, String value) {
        if (value == null || value.trim().isEmpty()) {
            showFieldError(field, errorLabel, "L'email est requis");
            return false;
        }

        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            showFieldError(field, errorLabel, "Format email invalide");
            return false;
        }
        if (trimmed.length() > 100) {
            showFieldError(field, errorLabel, "Maximum 100 caractères");
            return false;
        }

        clearFieldError(field, errorLabel);
        return true;
    }

    private boolean validatePhone(TextField field, Label errorLabel, String value) {
        if (value == null || value.trim().isEmpty()) {
            clearFieldError(field, errorLabel);
            return true;
        }

        String trimmed = value.trim();
        if (!PHONE_REGEX.matcher(trimmed).matches()) {
            showFieldError(field, errorLabel, "Format invalide (ex: +33 6 12 34 56 78)");
            return false;
        }
        if (trimmed.length() > 20) {
            showFieldError(field, errorLabel, "Maximum 20 caractères");
            return false;
        }

        clearFieldError(field, errorLabel);
        return true;
    }

    private boolean validatePassword(PasswordField field, Label errorLabel,
                                     PasswordField confirmField, Label confirmErrorLabel,
                                     String value, boolean isNewUser) {
        if (isNewUser) {
            if (value == null || value.isEmpty()) {
                showFieldError(field, errorLabel, "Le mot de passe est requis");
                return false;
            }
        } else {
            if (value == null || value.isEmpty()) {
                clearFieldError(field, errorLabel);
                return true;
            }
        }

        if (value.length() < 6) {
            showFieldError(field, errorLabel, "Minimum 6 caractères");
            return false;
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            showFieldError(field, errorLabel, "Doit contenir majuscule, minuscule et chiffre");
            return false;
        }

        clearFieldError(field, errorLabel);

        // Vérifier la confirmation si elle existe
        if (confirmField != null && confirmErrorLabel != null) {
            validatePasswordConfirmation(field, confirmField, confirmErrorLabel, isNewUser);
        }

        return true;
    }

    private boolean validatePasswordConfirmation(PasswordField passwordField,
                                                 PasswordField confirmField,
                                                 Label errorLabel, boolean isNewUser) {
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (isNewUser) {
            if (confirm == null || confirm.isEmpty()) {
                showFieldError(confirmField, errorLabel, "La confirmation est requise");
                return false;
            }
        } else {
            if (password.isEmpty() && confirm.isEmpty()) {
                clearFieldError(confirmField, errorLabel);
                return true;
            }
            if (confirm == null || confirm.isEmpty()) {
                showFieldError(confirmField, errorLabel, "Confirmez le mot de passe");
                return false;
            }
        }

        if (!password.isEmpty() && !confirm.isEmpty() && !password.equals(confirm)) {
            showFieldError(confirmField, errorLabel, "Les mots de passe ne correspondent pas");
            return false;
        }

        clearFieldError(confirmField, errorLabel);
        return true;
    }

    private boolean validateRole(ComboBox<Role_enum> combo, Label errorLabel, Role_enum value) {
        if (value == null) {
            showFieldError(combo, errorLabel, "Veuillez sélectionner un rôle");
            return false;
        }
        clearFieldError(combo, errorLabel);
        return true;
    }

    // ==================== GESTION DES ERREURS ====================

    private void showFieldError(Control field, Label errorLabel, String message) {
        field.getStyleClass().removeAll("valid", "error");
        if (!field.getStyleClass().contains("error")) {
            field.getStyleClass().add("error");
        }

        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }

        field.setTooltip(new Tooltip(message));
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("error");
        if (!field.getStyleClass().contains("valid")) {
            field.getStyleClass().add("valid");
        }
        field.setTooltip(null);

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void validateUser(User user, boolean isNewUser) {
        // Validation nom
        if (user.getNom() == null || user.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est requis");
        }
        if (user.getNom().length() < 3) {
            throw new IllegalArgumentException("Le nom doit contenir au moins 3 caractères");
        }
        if (user.getNom().length() > 50) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser 50 caractères");
        }
        if (!NAME_REGEX.matcher(user.getNom()).matches()) {
            throw new IllegalArgumentException("Caractères invalides dans le nom");
        }

        // Validation email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis");
        }
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new IllegalArgumentException("Format d'email invalide");
        }

        // Validation mot de passe
        if (isNewUser) {
            if (user.getMDP() == null || user.getMDP().isEmpty()) {
                throw new IllegalArgumentException("Le mot de passe est requis");
            }
            if (user.getMDP().length() < 6) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
            }
            if (!PASSWORD_PATTERN.matcher(user.getMDP()).matches()) {
                throw new IllegalArgumentException("Le mot de passe doit contenir des majuscules, minuscules et chiffres");
            }
        }

        // Validation rôle
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Le rôle est requis");
        }

        // Validation téléphone (optionnel)
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (!PHONE_REGEX.matcher(user.getPhone()).matches()) {
                throw new IllegalArgumentException("Format de téléphone invalide");
            }
        }
    }

    private void showFeedback(String message, String type) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message);
            feedbackLabel.getStyleClass().removeAll("success", "error", "info");
            feedbackLabel.getStyleClass().add(type);
            feedbackLabel.setTextFill(type.equals("success") ? Color.GREEN :
                    type.equals("error") ? Color.RED : Color.BLUE);
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        if (feedbackLabel != null) {
                            feedbackLabel.setText("");
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/admin-dashboard.fxml", "Tableau de bord Admin");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/login.fxml", "Connexion");
    }

    //////////////////////////////////////////////////block unblock metier
    @FXML
    private void blockSelectedUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("❌ Sélectionnez un utilisateur à bloquer", "error");
            return;
        }

        // Vérifier si c'est un ADMIN (protection)
        if (selected.getRole() == Role_enum.ADMIN) {
            showFeedback("⚠️ Impossible de bloquer un administrateur !", "error");
            return;
        }

        // Vérifier si déjà inactif
        if (!selected.isActive()) {
            showFeedback("ℹ️ Cet utilisateur est déjà bloqué", "info");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bloquer l'utilisateur");
        confirm.setHeaderText("Confirmer le blocage");
        confirm.setContentText(String.format("""
            Êtes-vous sûr de vouloir BLOQUER l'utilisateur :
            
            👤 Nom : %s
            📧 Email : %s
            🔑 Rôle : %s
            
            ❌ Il ne pourra PLUS se connecter !
            """,
                selected.getDisplayName(),
                selected.getEmail(),
                getRoleDisplayName(selected.getRole())
        ));

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // Personnaliser les boutons
        Button blockButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        blockButton.setText("Bloquer");
        blockButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

        Button cancelButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.NO);
        cancelButton.setText("Annuler");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // Changement de statut + log métier
                    userService.changerStatut(selected.getId(), false);

                    // Log métier dans la console (ou fichier log)
                    logUserAction("BLOCK", selected);

                    refreshUsers();
                    showFeedback(String.format(
                            "✅ Utilisateur %s a été bloqué avec succès !",
                            selected.getDisplayName()
                    ), "success");

                } catch (SQLException e) {
                    showFeedback("❌ Erreur lors du blocage : " + e.getMessage(), "error");
                }
            }
        });
    }

    @FXML
    private void unblockSelectedUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("❌ Sélectionnez un utilisateur à débloquer", "error");
            return;
        }

        // Vérifier si déjà actif
        if (selected.isActive()) {
            showFeedback("ℹ️ Cet utilisateur est déjà actif", "info");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Débloquer l'utilisateur");
        confirm.setHeaderText("Confirmer le déblocage");
        confirm.setContentText(String.format("""
            Êtes-vous sûr de vouloir DÉBLOQUER l'utilisateur :
            
            👤 Nom : %s
            📧 Email : %s
            🔑 Rôle : %s
            
            ✅ Il pourra de nouveau se connecter !
            """,
                selected.getDisplayName(),
                selected.getEmail(),
                getRoleDisplayName(selected.getRole())
        ));

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Button unblockButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        unblockButton.setText("Débloquer");
        unblockButton.setStyle("-fx-background-color: #198754; -fx-text-fill: white;");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.changerStatut(selected.getId(), true);
                    logUserAction("UNBLOCK", selected);
                    refreshUsers();
                    showFeedback(String.format(
                            "✅ Utilisateur %s a été débloqué avec succès !",
                            selected.getDisplayName()
                    ), "success");
                } catch (SQLException e) {
                    showFeedback("❌ Erreur lors du déblocage : " + e.getMessage(), "error");
                }
            }
        });
    }

    // 🔐 RÈGLE MÉTIER FORTE : Vérification avant blocage
    private boolean canBlockUser(User targetUser) {
        User currentUser = SessionManager.getCurrentUser();

        // Règle 1: Un admin ne peut pas bloquer un autre admin
        if (targetUser.getRole() == Role_enum.ADMIN) {
            showFeedback("⚠️ Les administrateurs ne peuvent pas être bloqués", "error");
            return false;
        }

        // Règle 2: Un admin ne peut pas se bloquer lui-même
        if (currentUser != null && currentUser.getId() == targetUser.getId()) {
            showFeedback("⚠️ Vous ne pouvez pas vous bloquer vous-même !", "error");
            return false;
        }

        // Règle 3: Vérifier si c'est le dernier admin (si on voulait bloquer un admin)
        // À implémenter si nécessaire

        return true;
    }

    // 📝 LOG MÉTIER (sans base de données)
    private void logUserAction(String action, User user) {
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date());

        User admin = SessionManager.getCurrentUser();
        String adminName = admin != null ? admin.getDisplayName() : "SYSTEM";

        String logMessage = String.format(
                "[%s] 🔐 ACTION: %s | Admin: %s | Cible: %s (%s) | Email: %s | Rôle: %s",
                timestamp,
                action,
                adminName,
                user.getDisplayName(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        // 1. Afficher dans la console (utile pour debug)
        System.out.println(logMessage);

        // 2. Optionnel : Écrire dans un fichier log
        writeToLogFile(logMessage);

        // 3. Optionnel : Stocker dans une liste en mémoire pour l'affichage
        addToActivityLog(logMessage);
    }

    // 📁 Écriture dans fichier log (optionnel)
    private void writeToLogFile(String message) {
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get("user_actions.log");
            java.nio.file.Files.writeString(
                    logPath,
                    message + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (java.io.IOException e) {
            System.err.println("⚠️ Impossible d'écrire dans le fichier log: " + e.getMessage());
        }
    }

    // 📋 Liste en mémoire pour l'activité récente
    private ObservableList<String> activityLog = FXCollections.observableArrayList();

    private void addToActivityLog(String message) {
        activityLog.add(0, message); // Ajouter au début
        if (activityLog.size() > 50) { // Garder seulement 50 entrées
            activityLog.remove(activityLog.size() - 1);
        }
    }
    private String getRoleDisplayName(Role_enum role) {
        if (role == null) return "Non défini";

        switch (role) {
            case ADMIN:
                return "Administrateur";
            case INVESTISSEUR:
                return "Investisseur";
            case STARTUP:
                return "Porteur de projet";
            default:
                return role.name();
        }
    }
}