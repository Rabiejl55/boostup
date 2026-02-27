package controllers;

import services.AccompagnementService.SuggestionAPI;
import services.AccompagnementService.ProfanityDetectorAPI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;

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
            star.setImage(i < upTo ? starFilled : starEmpty);
        }
    }

    @FXML
    private void handleSendFeedback() {
        String commentText = commentTextArea.getText().trim();

        if (rating == 0 && commentText.isEmpty()) {
            showAlert("Veuillez mettre une note ou écrire un commentaire !");
            return;
        }

        // Vérification langage inapproprié dans un thread séparé
        new Thread(() -> {
            boolean containsBadWords = ProfanityDetectorAPI.hasBadWords(commentText);

            Platform.runLater(() -> {
                if (containsBadWords) {
                    showAlert("Commentaire refusé : langage inapproprié");
                } else {
                    // Créer la boîte du commentaire
                    VBox commentBox = new VBox();
                    commentBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
                    commentBox.setSpacing(5);

                    // Label pour le rating
                    Label ratingLabel = new Label("⭐".repeat(rating));
                    ratingLabel.setStyle("-fx-font-size: 14px;");

                    // Label pour le commentaire
                    Label commentLabel = new Label(commentText);
                    commentLabel.setWrapText(true);
                    commentLabel.setStyle("-fx-font-size: 13px;");

                    // --- Suggestion automatique ---
                    String correctedText = SuggestionAPI.getCorrectedText(commentText);
                    Label suggestionLabel = new Label("Phrase corrigée :\n" + correctedText);
                    suggestionLabel.setWrapText(true);
                    suggestionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

                    // Ajouter les labels à la VBox
                    commentBox.getChildren().addAll(ratingLabel, commentLabel, suggestionLabel);

                    // Ajouter à l'UI
                    commentsContainer.getChildren().add(0, commentBox);

                    // Réinitialiser formulaire
                    rating = 0;
                    highlightStars(0);
                    commentTextArea.clear();

                    // Sauvegarde en base (à compléter)
                    saveFeedback(commentText);
                }
            });
        }).start();
    }

    private void saveFeedback(String feedback) {
        // Ici, ajoute ton code pour enregistrer le feedback en base
        System.out.println("Feedback sauvegardé : " + feedback);
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFeedbackClick(ActionEvent event) {
        // Rester sur Feedback.fxml
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);
        alert.setContentText("Vous avez été déconnecté !");
        alert.showAndWait();
    }
}