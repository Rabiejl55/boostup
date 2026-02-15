package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static javafx.application.Application.launch;

public class AppF extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Charger le fichier FXML
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));

        // Créer la scène
        Scene scene = new Scene(root, 1000, 650);

        // Appliquer le CSS
        scene.getStylesheets().add(getClass().getResource("/css/sidebar.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/financement.css").toExternalForm());

        // Mettre la scène dans la fenêtre
        stage.setScene(scene);
        stage.setTitle("Test Interface CSS");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args); // lance JavaFX
    }
}