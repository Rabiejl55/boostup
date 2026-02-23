package controllers;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
public class FeedbackController {

    @FXML private HBox starBox;
    @FXML private TextArea commentTextArea;
    @FXML private Button sendFeedbackButton;
    @FXML private VBox commentsContainer;

    private int rating = 0; // valeur actuelle du rating

    // Images étoiles
    private final Image starFilled = new Image(getClass().getResourceAsStream("/images/star_filled.png"));
    private final Image starEmpty = new Image(getClass().getResourceAsStream("/images/star_empty.png"));

    @FXML
    public void initialize() {
        setupStars();
    }

    private void setupStars() {
        starBox.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(starEmpty);
            star.setFitWidth(30);
            star.setFitHeight(30);
            final int starValue = i;

            // Hover effect
            star.setOnMouseEntered(e -> highlightStars(starValue));
            star.setOnMouseExited(e -> highlightStars(rating));

            // Click
            star.setOnMouseClicked(e -> {
                rating = starValue;
                highlightStars(rating);
            });

            starBox.getChildren().add(star);
        }
    }

    private void highlightStars(int upTo) {
        for (int i = 0; i < starBox.getChildren().size(); i++) {
            ImageView star = (ImageView) starBox.getChildren().get(i);
            if (i < upTo) star.setImage(starFilled);
            else star.setImage(starEmpty);
        }
    }

    @FXML
    private void handleSendFeedback() {
        String commentText = commentTextArea.getText().trim();
        if (rating == 0 && commentText.isEmpty()) {
            showAlert("Veuillez mettre une note ou écrire un commentaire !");
            return;
        }

        // Ajouter le commentaire dans la page
        VBox commentBox = new VBox();
        commentBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        commentBox.setSpacing(5);

        Label ratingLabel = new Label("⭐".repeat(rating));
        ratingLabel.setStyle("-fx-font-size: 14px;");

        Label commentLabel = new Label(commentText);
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-font-size: 13px;");

        commentBox.getChildren().addAll(ratingLabel, commentLabel);
        commentsContainer.getChildren().add(0, commentBox); // ajout en tête

        // Réinitialiser formulaire
        rating = 0;
        highlightStars(0);
        commentTextArea.clear();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleHomeClick(ActionEvent event) {
        try {
            // Charger Home.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            // Récupérer la fenêtre actuelle
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Remplacer la scène
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Bouton Feedback (optionnel si tu restes sur la même page)
    @FXML
    private void handleFeedbackClick(ActionEvent event) {
        // Si tu veux rester sur Feedback.fxml, tu peux laisser vide
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Exemple simple
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);
        alert.setContentText("Vous avez été déconnecté !");
        alert.showAndWait();
    }
}