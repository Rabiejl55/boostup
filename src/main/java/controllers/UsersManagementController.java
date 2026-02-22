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
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
    @FXML private Label filterSummaryLabel;
    @FXML private Label userCountLabel;
    @FXML private Label welcomeLabel;
    @FXML private ImageView avatarImageView;

    private final UserService userService = new UserService();
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<Role_enum> roleFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;

    private FilteredList<User> filteredData;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^[+]?[0-9\\s.-]{8,20}$");
    private static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-z0-9À-ÖØ-öø-ÿ\\s'-]{2,50}$");

    private ObservableList<String> activityLog = FXCollections.observableArrayList();

    // ══════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && welcomeLabel != null) {
            String displayName = currentUser.getFullname() != null && !currentUser.getFullname().isEmpty()
                    ? currentUser.getFullname() : currentUser.getNom();
            welcomeLabel.setText(displayName);
        }

        // ID — Caché
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setVisible(false);

        // NOM — Éditable double-clic
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomColumn.setCellFactory(col -> new EditableTextFieldCell("nom"));
        nomColumn.setOnEditCommit(event -> {
            User user = event.getRowValue();
            String newValue = event.getNewValue().trim();
            String oldValue = event.getOldValue();
            if (validateAndSaveField(user, "nom", newValue)) {
                user.setNom(newValue);
                logUserAction("EDIT_NOM", user);
                showFeedback("✅ Nom modifié : " + oldValue + " → " + newValue, "success");
            } else {
                event.getTableView().refresh();
            }
        });

        // EMAIL — Éditable double-clic
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setCellFactory(col -> new EditableTextFieldCell("email"));
        emailColumn.setOnEditCommit(event -> {
            User user = event.getRowValue();
            String newValue = event.getNewValue().trim();
            String oldValue = event.getOldValue();
            if (validateAndSaveField(user, "email", newValue)) {
                user.setEmail(newValue);
                logUserAction("EDIT_EMAIL", user);
                showFeedback("✅ Email modifié : " + oldValue + " → " + newValue, "success");
            } else {
                event.getTableView().refresh();
            }
        });

        // RÔLE — Clic badge
        roleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getRole() != null ? cell.getValue().getRole().name() : "N/A"));
        roleColumn.setCellFactory(col -> new EditableRoleCell());

        // STATUT — Clic badge toggle
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeColumn.setCellFactory(col -> new ToggleActiveCell());

        // DATE — Lecture seule
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        dateCreationColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(java.sql.Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                            .format(new java.util.Date(item.getTime())));
                    setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
                }
            }
        });

        // Activer édition
        usersTable.setEditable(true);
        usersTable.getSelectionModel().setCellSelectionEnabled(false);

        initializeFilters();
        usersTable.setItems(usersList);
        refreshUsers();
        loadAvatar();
    }

    // ══════════════════════════════════════════════════════════
    // COMPTEUR
    // ══════════════════════════════════════════════════════════

    private void updateUserCount() {
        if (userCountLabel != null) {
            int total = usersList.size();
            int filtered = filteredData != null ? filteredData.size() : total;
            if (filtered < total) {
                userCountLabel.setText("📊 " + filtered + " / " + total);
            } else {
                userCountLabel.setText("📊 " + total + " utilisateur" + (total > 1 ? "s" : ""));
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // AVATAR
    // ══════════════════════════════════════════════════════════

    private void loadAvatar() {
        if (avatarImageView == null) return;
        User user = SessionManager.getCurrentUser();
        String avatarUrl = user != null ? user.getAvatar() : null;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                if (avatarUrl.contains(" ")) avatarUrl = avatarUrl.replace(" ", "%20");
                if (avatarUrl.startsWith("C:/") || avatarUrl.startsWith("D:/"))
                    avatarUrl = "file:///" + avatarUrl.replace("\\", "/");

                Image image = new Image(avatarUrl, 80, 80, true, true, true);
                if (!image.isError()) {
                    applyAvatarImage(image);
                    return;
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement avatar: " + e.getMessage());
            }
        }
        setDefaultAvatar();
    }

    private void applyAvatarImage(Image image) {
        avatarImageView.setImage(image);
        avatarImageView.setPreserveRatio(true);
        avatarImageView.setSmooth(true);
        avatarImageView.setFitWidth(80);
        avatarImageView.setFitHeight(80);
        avatarImageView.setClip(new Circle(40, 40, 40));
        avatarImageView.setVisible(true);
    }

    private void setDefaultAvatar() {
        if (avatarImageView == null) return;
        User user = SessionManager.getCurrentUser();
        Color c1, c2;
        if (user != null) {
            double hue = Math.abs(user.getNom().hashCode() % 360);
            c1 = Color.hsb(hue, 0.7, 0.9);
            c2 = Color.hsb((hue + 30) % 360, 0.8, 0.8);
        } else {
            c1 = Color.web("#0d6efd");
            c2 = Color.web("#6f42c1");
        }

        WritableImage img = new WritableImage(80, 80);
        PixelWriter w = img.getPixelWriter();
        for (int y = 0; y < 80; y++) {
            for (int x = 0; x < 80; x++) {
                w.setColor(x, y, c1.interpolate(c2, (x + y) / 160.0));
            }
        }
        applyAvatarImage(img);
    }

    // ══════════════════════════════════════════════════════════
    // FILTRES
    // ══════════════════════════════════════════════════════════

    private void initializeFilters() {
        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());

        filterComboBox.setItems(FXCollections.observableArrayList("Tous les champs", "Nom", "Email", "Téléphone"));
        filterComboBox.getSelectionModel().selectFirst();
        filterComboBox.valueProperty().addListener((o, ov, nv) -> applyFilters());

        roleFilterComboBox.setItems(FXCollections.observableArrayList(Role_enum.values()));
        roleFilterComboBox.getSelectionModel().selectFirst();
        roleFilterComboBox.valueProperty().addListener((o, ov, nv) -> applyFilters());

        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Actifs uniquement", "Inactifs uniquement"));
        statusFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.valueProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void refreshUsers() {
        try {
            usersList.clear();
            usersList.addAll(userService.read());
            filteredData = new FilteredList<>(usersList, p -> true);
            usersTable.setItems(filteredData);
            updateUserCount();
            showFeedback("✅ " + usersList.size() + " utilisateurs chargés", "success");
        } catch (SQLException e) {
            showFeedback("❌ Erreur chargement : " + e.getMessage(), "error");
        }
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String text = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String field = filterComboBox.getValue();
        Role_enum role = roleFilterComboBox.getValue();
        String status = statusFilterComboBox.getValue();

        filteredData.setPredicate(user -> {
            if (role != null && user.getRole() != role) return false;
            if ("Actifs uniquement".equals(status) && !user.isActive()) return false;
            if ("Inactifs uniquement".equals(status) && user.isActive()) return false;
            if (text.isEmpty()) return true;

            switch (field != null ? field : "Tous les champs") {
                case "Nom":
                    return contains(user.getNom(), text);
                case "Email":
                    return contains(user.getEmail(), text);
                case "Téléphone":
                    return contains(user.getPhone(), text);
                default:
                    return contains(user.getNom(), text) || contains(user.getEmail(), text) ||
                            contains(user.getFullname(), text) || contains(user.getPhone(), text) ||
                            (user.getRole() != null && user.getRole().name().toLowerCase().contains(text));
            }
        });
        updateUserCount();
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        searchField.clear();
        filterComboBox.getSelectionModel().selectFirst();
        roleFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();
        applyFilters();
        showFeedback("🔄 Filtres réinitialisés", "success");
    }

    @FXML
    private void refreshUsers(ActionEvent event) {
        refreshUsers();
    }

    // ══════════════════════════════════════════════════════════
    // AJOUTER UTILISATEUR
    // ══════════════════════════════════════════════════════════

    @FXML
    private void showAddUserDialog(ActionEvent event) {
        Dialog<User> dialog = createAddDialog();
        dialog.showAndWait().ifPresent(user -> {
            try {
                validateUser(user);
                userService.ajouter(user);
                logUserAction("ADD", user);
                refreshUsers();
                showFeedback("✅ " + user.getDisplayName() + " ajouté avec succès !", "success");
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("email")) {
                    showFeedback("❌ Cet email est déjà utilisé !", "error");
                } else {
                    showFeedback("❌ Erreur : " + e.getMessage(), "error");
                }
            } catch (IllegalArgumentException e) {
                showFeedback("❌ " + e.getMessage(), "error");
            }
        });
    }

    private Dialog<User> createAddDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("➕ Nouvel utilisateur");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dp.setPrefWidth(550);
        dp.setPrefHeight(650);

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");

        VBox main = new VBox(16);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #f8f9fa;");

        // Section infos
        VBox infoBox = createSection("👤 Informations");
        GridPane g1 = new GridPane();
        g1.setHgap(12);
        g1.setVgap(8);
        int r = 0;

        TextField nomF = addField(g1, r, "Nom d'utilisateur *", "Ex: jean.dupont");
        r += 2;
        Label nomErr = addErrorLabel(g1, r++);

        TextField fullnameF = addField(g1, r, "Nom complet", "Ex: Jean Dupont");
        r += 2;

        TextField emailF = addField(g1, r, "Email *", "Ex: jean@exemple.com");
        r += 2;
        Label emailErr = addErrorLabel(g1, r++);

        TextField phoneF = addField(g1, r, "Téléphone", "+216 12 345 678 (optionnel)");
        r += 2;
        Label phoneErr = addErrorLabel(g1, r++);

        infoBox.getChildren().add(g1);
        main.getChildren().add(infoBox);

        // Section sécurité
        VBox secBox = createSection("🔒 Sécurité");
        GridPane g2 = new GridPane();
        g2.setHgap(12);
        g2.setVgap(8);
        int s = 0;

        PasswordField mdpF = addPwdField(g2, s, "Mot de passe *", "Minimum 6 caractères");
        s += 2;
        Label mdpErr = addErrorLabel(g2, s++);

        PasswordField mdpCF = addPwdField(g2, s, "Confirmation *", "Confirmer le mot de passe");
        s += 2;
        Label mdpCErr = addErrorLabel(g2, s++);

        secBox.getChildren().add(g2);
        main.getChildren().add(secBox);

        // Section paramètres
        VBox setBox = createSection("⚙️ Paramètres");
        GridPane g3 = new GridPane();
        g3.setHgap(12);
        g3.setVgap(8);

        Label roleLbl = new Label("Rôle *");
        roleLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #344054; -fx-font-size: 13px;");
        g3.add(roleLbl, 0, 0, 2, 1);

        ComboBox<Role_enum> roleCombo = new ComboBox<>(FXCollections.observableArrayList(Role_enum.values()));
        roleCombo.setValue(Role_enum.INVESTISSEUR);
        roleCombo.getStyleClass().add("modern-input");
        roleCombo.setPrefWidth(400);
        g3.add(roleCombo, 0, 1, 2, 1);

        CheckBox activeChk = new CheckBox("Compte actif");
        activeChk.setSelected(true);
        activeChk.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10 0 0 0;");
        g3.add(activeChk, 0, 2, 2, 1);

        setBox.getChildren().add(g3);
        main.getChildren().add(setBox);

        sp.setContent(main);
        dp.setContent(sp);

        // Style boutons
        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        okBtn.setText("💾 Enregistrer");
        okBtn.setStyle("-fx-background-color: linear-gradient(to right, #1b2a4a, #2d1b4e); " +
                "-fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 12 28; -fx-background-radius: 8px;");
        okBtn.setDisable(true);

        Button cancelBtn = (Button) dp.lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d; -fx-font-weight: 600; " +
                "-fx-padding: 12 28; -fx-background-radius: 8px; -fx-border-color: #dee2e6; -fx-border-radius: 8px;");

        // Validation temps réel
        Runnable validate = () -> {
            boolean ok = true;
            ok &= vNom(nomF, nomErr);
            ok &= vEmail(emailF, emailErr);
            ok &= vPhone(phoneF, phoneErr);
            ok &= vPwd(mdpF, mdpErr);
            ok &= vPwdConfirm(mdpF, mdpCF, mdpCErr);
            ok &= roleCombo.getValue() != null;
            okBtn.setDisable(!ok);
        };

        nomF.textProperty().addListener((o, a, b) -> validate.run());
        emailF.textProperty().addListener((o, a, b) -> validate.run());
        phoneF.textProperty().addListener((o, a, b) -> validate.run());
        mdpF.textProperty().addListener((o, a, b) -> validate.run());
        mdpCF.textProperty().addListener((o, a, b) -> validate.run());
        roleCombo.valueProperty().addListener((o, a, b) -> validate.run());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                User u = new User();
                u.setNom(nomF.getText().trim());
                u.setFullname(fullnameF.getText().trim());
                u.setEmail(emailF.getText().trim());
                u.setPhone(phoneF.getText().trim().isEmpty() ? null : phoneF.getText().trim());
                u.setMDP(mdpF.getText());
                u.setRole(roleCombo.getValue());
                u.setActive(activeChk.isSelected());
                return u;
            }
            return null;
        });

        return dialog;
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS DIALOGUE
    // ══════════════════════════════════════════════════════════

    private VBox createSection(String title) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1b2a4a;");
        box.getChildren().add(lbl);
        return box;
    }

    private TextField addField(GridPane grid, int row, String label, String prompt) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #344054; -fx-font-size: 13px;");
        grid.add(lbl, 0, row, 2, 1);
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("modern-input");
        tf.setPrefWidth(400);
        grid.add(tf, 0, row + 1, 2, 1);
        return tf;
    }

    private PasswordField addPwdField(GridPane grid, int row, String label, String prompt) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #344054; -fx-font-size: 13px;");
        grid.add(lbl, 0, row, 2, 1);
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("modern-input");
        pf.setPrefWidth(400);
        grid.add(pf, 0, row + 1, 2, 1);
        return pf;
    }

    private Label addErrorLabel(GridPane grid, int row) {
        Label err = new Label();
        err.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
        err.setWrapText(true);
        err.setVisible(false);
        err.setManaged(false);
        grid.add(err, 0, row, 2, 1);
        return err;
    }

    // ══════════════════════════════════════════════════════════
    // SUPPRIMER
    // ══════════════════════════════════════════════════════════

    @FXML
    private void deleteSelectedUser(ActionEvent event) {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showFeedback("⚠️ Sélectionnez un utilisateur", "error");
            return;
        }

        User me = SessionManager.getCurrentUser();
        if (me != null && me.getId() == sel.getId()) {
            showFeedback("⚠️ Impossible de supprimer votre propre compte", "error");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Supprimer " + sel.getDisplayName() + " (" + sel.getEmail() + ") ?\n\n⚠️ Action irréversible !");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Button yesBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        yesBtn.setText("Supprimer");
        yesBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.supprimer(sel.getId());
                    logUserAction("DELETE", sel);
                    refreshUsers();
                    showFeedback("✅ " + sel.getDisplayName() + " supprimé", "success");
                } catch (SQLException e) {
                    showFeedback("❌ " + e.getMessage(), "error");
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════��══════
    // BLOQUER / DÉBLOQUER
    // ══════════════════════════════════════════════════════════

    @FXML
    private void blockSelectedUser(ActionEvent event) {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showFeedback("⚠️ Sélectionnez un utilisateur", "error");
            return;
        }
        if (sel.getRole() == Role_enum.ADMIN) {
            showFeedback("⚠️ Impossible de bloquer un admin", "error");
            return;
        }
        if (!sel.isActive()) {
            showFeedback("ℹ️ Déjà bloqué", "info");
            return;
        }
        User me = SessionManager.getCurrentUser();
        if (me != null && me.getId() == sel.getId()) {
            showFeedback("⚠️ Impossible de vous bloquer", "error");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bloquer");
        confirm.setHeaderText("Confirmer le blocage");
        confirm.setContentText("Bloquer " + sel.getDisplayName() + " ?\n\n❌ Il ne pourra plus se connecter !");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Button blockBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        blockBtn.setText("Bloquer");
        blockBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.changerStatut(sel.getId(), false);
                    logUserAction("BLOCK", sel);
                    refreshUsers();
                    showFeedback("✅ " + sel.getDisplayName() + " bloqué", "success");
                } catch (SQLException e) {
                    showFeedback("❌ " + e.getMessage(), "error");
                }
            }
        });
    }

    @FXML
    private void unblockSelectedUser(ActionEvent event) {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showFeedback("⚠️ Sélectionnez un utilisateur", "error");
            return;
        }
        if (sel.isActive()) {
            showFeedback("ℹ️ Déjà actif", "info");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Débloquer");
        confirm.setHeaderText("Confirmer le déblocage");
        confirm.setContentText("Débloquer " + sel.getDisplayName() + " ?\n\n✅ Il pourra se reconnecter !");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Button unblockBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        unblockBtn.setText("Débloquer");
        unblockBtn.setStyle("-fx-background-color: #198754; -fx-text-fill: white;");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.changerStatut(sel.getId(), true);
                    logUserAction("UNBLOCK", sel);
                    refreshUsers();
                    showFeedback("✅ " + sel.getDisplayName() + " débloqué", "success");
                } catch (SQLException e) {
                    showFeedback("❌ " + e.getMessage(), "error");
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    // VALIDATION — Dialogue
    // ══════════════════════════════════════════════════════════

    private boolean vNom(TextField f, Label err) {
        String v = f.getText() != null ? f.getText().trim() : "";
        if (v.isEmpty()) { setErr(f, err, "Requis"); return false; }
        if (v.length() < 3) { setErr(f, err, "Min 3 caractères"); return false; }
        if (v.length() > 50) { setErr(f, err, "Max 50 caractères"); return false; }
        if (!NAME_REGEX.matcher(v).matches()) { setErr(f, err, "Caractères invalides"); return false; }
        clrErr(f, err);
        return true;
    }

    private boolean vEmail(TextField f, Label err) {
        String v = f.getText() != null ? f.getText().trim() : "";
        if (v.isEmpty()) { setErr(f, err, "Requis"); return false; }
        if (!EMAIL_PATTERN.matcher(v).matches()) { setErr(f, err, "Format invalide"); return false; }
        clrErr(f, err);
        return true;
    }

    private boolean vPhone(TextField f, Label err) {
        String v = f.getText() != null ? f.getText().trim() : "";
        if (v.isEmpty()) { clrErr(f, err); return true; }
        if (!PHONE_REGEX.matcher(v).matches()) { setErr(f, err, "Format invalide"); return false; }
        clrErr(f, err);
        return true;
    }

    private boolean vPwd(PasswordField f, Label err) {
        String v = f.getText() != null ? f.getText() : "";
        if (v.isEmpty()) { setErr(f, err, "Requis"); return false; }
        if (v.length() < 6) { setErr(f, err, "Min 6 caractères"); return false; }
        if (!PASSWORD_PATTERN.matcher(v).matches()) { setErr(f, err, "Majuscule + minuscule + chiffre"); return false; }
        clrErr(f, err);
        return true;
    }

    private boolean vPwdConfirm(PasswordField pwd, PasswordField confirm, Label err) {
        String c = confirm.getText() != null ? confirm.getText() : "";
        String p = pwd.getText() != null ? pwd.getText() : "";
        if (c.isEmpty()) { setErr(confirm, err, "Requis"); return false; }
        if (!c.equals(p)) { setErr(confirm, err, "Ne correspond pas"); return false; }
        clrErr(confirm, err);
        return true;
    }

    private void validateUser(User u) {
        if (u.getNom() == null || u.getNom().trim().length() < 3)
            throw new IllegalArgumentException("Le nom doit contenir au moins 3 caractères");
        if (u.getEmail() == null || !EMAIL_PATTERN.matcher(u.getEmail()).matches())
            throw new IllegalArgumentException("Format d'email invalide");
        if (u.getMDP() == null || !PASSWORD_PATTERN.matcher(u.getMDP()).matches())
            throw new IllegalArgumentException("Mot de passe invalide (min 6, majuscule+minuscule+chiffre)");
        if (u.getRole() == null)
            throw new IllegalArgumentException("Le rôle est requis");
        if (u.getPhone() != null && !u.getPhone().isEmpty() && !PHONE_REGEX.matcher(u.getPhone()).matches())
            throw new IllegalArgumentException("Format de téléphone invalide");
    }

    // ══════════════════════════════════════════════════════════
    // ERREURS VISUELLES
    // ══════════════════════════════════════════════════════════

    private void setErr(Control f, Label err, String msg) {
        f.getStyleClass().removeAll("valid", "error");
        if (!f.getStyleClass().contains("error")) f.getStyleClass().add("error");
        if (err != null) {
            err.setText(msg);
            err.setVisible(true);
            err.setManaged(true);
        }
        f.setTooltip(new Tooltip(msg));
    }

    private void clrErr(Control f, Label err) {
        f.getStyleClass().remove("error");
        if (!f.getStyleClass().contains("valid")) f.getStyleClass().add("valid");
        f.setTooltip(null);
        if (err != null) {
            err.setText("");
            err.setVisible(false);
            err.setManaged(false);
        }
    }

    // ══════════════════════════════════════════════════════════
    // FEEDBACK & LOGGING
    // ══════════════════════════════════════════════════════════

    private void showFeedback(String message, String type) {
        if (feedbackLabel != null) {
            javafx.application.Platform.runLater(() -> {
                feedbackLabel.setText(message);
                feedbackLabel.getStyleClass().removeAll("success", "error", "info");
                feedbackLabel.getStyleClass().add(type);

                switch (type) {
                    case "success":
                        feedbackLabel.setTextFill(Color.web("#198754"));
                        break;
                    case "error":
                        feedbackLabel.setTextFill(Color.web("#dc3545"));
                        break;
                    default:
                        feedbackLabel.setTextFill(Color.web("#0d6efd"));
                        break;
                }
            });

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        if (feedbackLabel != null) feedbackLabel.setText("");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void logUserAction(String action, User user) {
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
        User admin = SessionManager.getCurrentUser();
        String adminName = admin != null ? admin.getDisplayName() : "SYSTEM";

        String logMessage = String.format(
                "[%s] 🔐 %s | Admin: %s | Cible: %s (ID:%d) | Email: %s | Rôle: %s",
                timestamp, action, adminName, user.getDisplayName(),
                user.getId(), user.getEmail(), user.getRole());

        System.out.println(logMessage);
        writeToLogFile(logMessage);
        addToActivityLog(logMessage);
    }

    private void writeToLogFile(String message) {
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get("user_actions.log");
            java.nio.file.Files.writeString(logPath, message + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            System.err.println("⚠️ Log file error: " + e.getMessage());
        }
    }

    private void addToActivityLog(String message) {
        activityLog.add(0, message);
        if (activityLog.size() > 50) activityLog.remove(activityLog.size() - 1);
    }

    private String getRoleDisplayName(Role_enum role) {
        if (role == null) return "Non défini";
        switch (role) {
            case ADMIN: return "Administrateur";
            case INVESTISSEUR: return "Investisseur";
            case STARTUP: return "Porteur de projet";
            default: return role.name();
        }
    }

    // ══════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════
    // VALIDATION INLINE (édition directe dans le tableau)
    // ══════════════════════════════════════════════════════════

    private boolean validateAndSaveField(User user, String fieldName, String newValue) {
        if ("nom".equals(fieldName)) {
            if (newValue == null || newValue.trim().isEmpty()) {
                showFeedback("❌ Le nom ne peut pas être vide", "error");
                return false;
            }
            if (newValue.trim().length() < 3) {
                showFeedback("❌ Le nom doit contenir au moins 3 caractères", "error");
                return false;
            }
            if (newValue.trim().length() > 50) {
                showFeedback("❌ Le nom ne peut pas dépasser 50 caractères", "error");
                return false;
            }
            if (!NAME_REGEX.matcher(newValue.trim()).matches()) {
                showFeedback("❌ Caractères invalides dans le nom", "error");
                return false;
            }
            user.setNom(newValue.trim());
        } else if ("email".equals(fieldName)) {
            if (newValue == null || newValue.trim().isEmpty()) {
                showFeedback("❌ L'email ne peut pas être vide", "error");
                return false;
            }
            if (!EMAIL_PATTERN.matcher(newValue.trim()).matches()) {
                showFeedback("❌ Format email invalide", "error");
                return false;
            }
            user.setEmail(newValue.trim());
        }

        try {
            userService.update(user);
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("email")) {
                showFeedback("❌ Cet email est déjà utilisé", "error");
            } else {
                showFeedback("❌ Erreur sauvegarde : " + e.getMessage(), "error");
            }
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CLASSES INTERNES — ÉDITION INLINE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cellule TextField éditable — double-clic pour éditer Nom ou Email
     */
    private class EditableTextFieldCell extends TableCell<User, String> {
        private TextField textField;
        private final String fieldName;

        public EditableTextFieldCell(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
            setStyle("");
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                if (isEditing()) {
                    if (textField != null) textField.setText(getString());
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                    setStyle("-fx-cursor: hand;");
                    setTooltip(new Tooltip("Double-cliquez pour modifier"));
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #0d6efd; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-padding: 8px 12px; " +
                            "-fx-font-size: 13px; " +
                            "-fx-text-fill: #212529; " +
                            "-fx-effect: dropshadow(gaussian, rgba(13,110,253,0.25), 10, 0, 0, 2);");
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

            textField.setOnAction(e -> commitEdit(textField.getText()));

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEdit(textField.getText());
                }
            });

            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }

    /**
     * Cellule badge cliquable pour changer le rôle
     */
    private class EditableRoleCell extends TableCell<User, String> {

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
                setText(null);
            } else {
                User user = getTableView().getItems().get(getIndex());
                Role_enum currentRole = user.getRole();

                Label badge = new Label(getRoleDisplayName(currentRole));
                String badgeStyle = getRoleBadgeStyle(currentRole);
                badge.setStyle(badgeStyle + " -fx-cursor: hand;");
                badge.setTooltip(new Tooltip("Cliquez pour changer le rôle"));

                badge.setOnMouseClicked(event -> {
                    ComboBox<Role_enum> comboBox = new ComboBox<>();
                    comboBox.setItems(FXCollections.observableArrayList(Role_enum.values()));
                    comboBox.setValue(currentRole);
                    comboBox.setStyle("-fx-font-size: 12px; -fx-background-color: white; " +
                            "-fx-border-color: #0d6efd; -fx-border-width: 2px; " +
                            "-fx-border-radius: 8px; -fx-background-radius: 8px;");
                    comboBox.setPrefWidth(160);

                    comboBox.setOnAction(e -> {
                        Role_enum newRole = comboBox.getValue();
                        if (newRole != null && newRole != currentRole) {
                            Role_enum oldRole = user.getRole();
                            user.setRole(newRole);
                            try {
                                userService.update(user);
                                logUserAction("EDIT_ROLE", user);
                                showFeedback("✅ Rôle : " + getRoleDisplayName(oldRole) +
                                        " → " + getRoleDisplayName(newRole), "success");
                                refreshUsers();
                            } catch (SQLException ex) {
                                showFeedback("❌ Erreur : " + ex.getMessage(), "error");
                                user.setRole(oldRole);
                                refreshUsers();
                            }
                        } else {
                            setGraphic(badge);
                        }
                    });

                    comboBox.focusedProperty().addListener((obs, wasFocused, isNow) -> {
                        if (!isNow) {
                            setGraphic(badge);
                        }
                    });

                    setGraphic(comboBox);
                    comboBox.show();
                    comboBox.requestFocus();
                });

                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        }

        private String getRoleBadgeStyle(Role_enum role) {
            String base = "-fx-padding: 5 14; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: 600; ";
            if (role == null) return base + "-fx-background-color: #f0f2f5; -fx-text-fill: #495057;";
            switch (role) {
                case ADMIN:
                    return base + "-fx-background-color: rgba(13,110,253,0.12); -fx-text-fill: #0d6efd;";
                case INVESTISSEUR:
                    return base + "-fx-background-color: rgba(255,193,7,0.15); -fx-text-fill: #997404;";
                case STARTUP:
                    return base + "-fx-background-color: rgba(111,66,193,0.12); -fx-text-fill: #6f42c1;";
                default:
                    return base + "-fx-background-color: #f0f2f5; -fx-text-fill: #495057;";
            }
        }
    }

    /**
     * Cellule toggle badge pour activer/désactiver un utilisateur
     */
    private class ToggleActiveCell extends TableCell<User, Boolean> {

        @Override
        public void updateItem(Boolean active, boolean empty) {
            super.updateItem(active, empty);

            if (empty || active == null) {
                setGraphic(null);
                setText(null);
            } else {
                User user = getTableView().getItems().get(getIndex());

                Label badge = new Label(active ? "✅ Actif" : "❌ Inactif");
                String badgeStyle = active
                        ? "-fx-background-color: rgba(25,135,84,0.12); -fx-text-fill: #198754;"
                        : "-fx-background-color: rgba(220,53,69,0.12); -fx-text-fill: #dc3545;";

                badge.setStyle(badgeStyle +
                        " -fx-padding: 5 14; -fx-background-radius: 20; " +
                        "-fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand; " +
                        "-fx-min-width: 95; -fx-alignment: center;");
                badge.setTooltip(new Tooltip(active ? "Cliquez pour désactiver" : "Cliquez pour activer"));

                badge.setOnMouseClicked(event -> {
                    User me = SessionManager.getCurrentUser();
                    if (me != null && me.getId() == user.getId()) {
                        showFeedback("⚠️ Impossible de modifier votre propre statut", "error");
                        return;
                    }
                    if (user.getRole() == Role_enum.ADMIN && user.isActive()) {
                        showFeedback("⚠️ Impossible de désactiver un admin", "error");
                        return;
                    }

                    boolean newStatus = !user.isActive();
                    user.setActive(newStatus);

                    try {
                        userService.update(user);
                        logUserAction(newStatus ? "ACTIVATE" : "DEACTIVATE", user);
                        showFeedback(newStatus
                                        ? "✅ " + user.getDisplayName() + " activé"
                                        : "⚠️ " + user.getDisplayName() + " désactivé",
                                newStatus ? "success" : "info");
                        refreshUsers();
                    } catch (SQLException e) {
                        showFeedback("❌ Erreur : " + e.getMessage(), "error");
                        user.setActive(!newStatus);
                        refreshUsers();
                    }
                });

                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        }
    }
}