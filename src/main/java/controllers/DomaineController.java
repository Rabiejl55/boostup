package controllers;

import entities.GAccompagnement.Domaine;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.AccompagnementService.DomaineService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class DomaineController {

    private final DomaineService domaineService = new DomaineService();

    @FXML private TableView<Domaine> domainesTable;
    @FXML private TableColumn<Domaine, Integer> idColumn;
    @FXML private TableColumn<Domaine, String> nomColumn;
    @FXML private TableColumn<Domaine, String> descriptionColumn;
    @FXML private TableColumn<Domaine, String> niveauColumn;
    @FXML private TableColumn<Domaine, String> statutColumn;
    @FXML private TableColumn<Domaine, String> imageColumn;

    @FXML private Label feedbackLabel;

    // Sidebar
    @FXML private ImageView avatarImageView;
    @FXML private Label welcomeLabel;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        niveauColumn.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        imageColumn.setCellFactory(column -> new TableCell<Domaine, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new FileInputStream(imagePath));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                        System.out.println("Image non trouvée : " + imagePath);
                    }
                }
            }
        });

        loadDomaines();

        if (avatarImageView != null) {
            try {
                FileInputStream fis = new FileInputStream("src/images/default_avatar.png");
                avatarImageView.setImage(new Image(fis));
            } catch (FileNotFoundException e) {
                System.out.println("Avatar par défaut non trouvé");
            }
        }

        if (welcomeLabel != null) welcomeLabel.setText("Administrateur");
    }
    private void loadDomaines() {
        try {
            domainesTable.setItems(FXCollections.observableArrayList(domaineService.afficherAll()));
            feedbackLabel.setText("Chargement réussi.");
        } catch (Exception e) {
            feedbackLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToFront(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void goToDomaine(ActionEvent event) {
        navigateTo(event, "/fxml/accompagnement.fxml", "Gestion Domaines");
    }

    @FXML
    private void goToSessions(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Session.fxml"));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("Gestion Sessions");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        feedbackLabel.setText("Déconnexion !");
        // redirection vers login
    }

    // ================= ACTIONS TABLE =================
    @FXML
    private void ajouterDomaine() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddDomaineForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Domaine");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadDomaines();
        } catch (Exception e) {
            feedbackLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierDomaine() {
        System.out.println(getClass().getResource("/fxml/EditDomaineForm.fxml"));

        Domaine selected = domainesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedbackLabel.setText("Veuillez sélectionner un domaine à modifier.");
            return;
        }

        try {
            URL fxmlUrl = getClass().getResource("/fxml/EditDomaineForm.fxml");
            if (fxmlUrl == null) {
                feedbackLabel.setText("Fichier EditDomaineForm.fxml introuvable !");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            EditDomaineController controller = loader.getController();
            controller.setDomaine(selected);

            Stage stage = new Stage();
            stage.setTitle("Modifier Domaine");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadDomaines();

        } catch (IOException e) {
            e.printStackTrace();
            feedbackLabel.setText("Erreur lors du chargement du formulaire : " + e.getMessage());
        }
    }


    @FXML
    private void supprimerDomaine() {
        Domaine selected = domainesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                domaineService.supprimer(selected.getId());
                loadDomaines();
                feedbackLabel.setText("Domaine supprimé avec succès.");
            } catch (Exception e) {
                feedbackLabel.setText("Erreur suppression : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            feedbackLabel.setText("Veuillez sélectionner un domaine à supprimer.");
        }
    }
    @FXML
    private void dummyAction(ActionEvent event) {
        System.out.println("Vous êtes déjà sur Gestion Domaines !");
    }
    @FXML private void goToSession(ActionEvent event) {
        navigateTo(event, "/fxml/Session.fxml", "Gestion Domaines");
    }
    @FXML private void goToCoach(ActionEvent event) {
        navigateTo(event, "/fxml/accompagnement.fxml", "Gestion Domaines");
    }
    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        System.out.println("Chemin FXML : " + getClass().getResource(fxmlPath));
        try {

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void chercherFichier(java.io.File dir, String nomFichier) {
        if (dir == null || !dir.exists()) return;

        for (java.io.File file : dir.listFiles()) {
            if (file.isDirectory()) {
                chercherFichier(file, nomFichier);
            } else if (file.getName().equals(nomFichier)) {
                System.out.println("TROUVÉ: " + file.getAbsolutePath());
            }
        }
    }
    @FXML
    private void goToProfile() {
        // à implémenter plus tard
    }

    @FXML
    private void goToHome() {
        // à implémenter plus tard
    }
    @FXML
    private void viewDomaines() {
        // à implémenter plus tard
    }



}
