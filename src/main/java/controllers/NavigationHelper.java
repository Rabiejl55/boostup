package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationHelper {

    public static void navigateTo(Stage stage, String fxmlPath, String title) {
        System.out.println("🔄 NavigationHelper.navigateTo appelé");
        System.out.println("   FXML: " + fxmlPath);
        System.out.println("   Title: " + title);

        try {
            System.out.println("   📂 Chargement du fichier FXML...");
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            System.out.println("   ✅ FXML chargé avec succès");

            if (stage.getScene() == null) {
                System.out.println("   🆕 Création d'une nouvelle scène");
                stage.setScene(new Scene(root));
            } else {
                System.out.println("   🔄 Remplacement du root de la scène existante");
                stage.getScene().setRoot(root);
            }

            if (title != null && !title.isEmpty()) {
                stage.setTitle(title);
            }

            stage.show();
            System.out.println("   ✅ Navigation réussie vers " + fxmlPath);
        } catch (Exception e) {
            System.err.println("❌ ERREUR navigation vers " + fxmlPath);
            System.err.println("   Type: " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de navigation vers " + fxmlPath, e);
        }
    }
}

