import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.UtilisateurService.UserService;

import java.sql.SQLException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root, 1200, 800);

        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("BoostUp - Connexion");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            // Créer le service UserService (seul service maintenant)
            UserService userService = new UserService();

            System.out.println("=== TEST USER SERVICE AVEC PROFIL INTÉGRÉ ===");

            // Vérifier si les utilisateurs de test existent déjà
            System.out.println("\n=== VÉRIFICATION DES UTILISATEURS EXISTANTS ===");
            User adminFromDB = userService.findByEmail("admin.profil@test.com");
            User investorFromDB = userService.findByEmail("invest.profil@test.com");

            if (adminFromDB != null && investorFromDB != null) {
                System.out.println("Utilisateurs de test déjà existants.");
                System.out.println("Admin ID: " + adminFromDB.getId());
                System.out.println("Investisseur ID: " + investorFromDB.getId());

                // 2. Tests avec utilisateurs existants
                System.out.println("\n=== TESTS AVEC UTILISATEURS EXISTANTS ===");

                // Mise à jour de l'avatar avec une URL valide
                System.out.println("\n=== MISE À JOUR AVATAR (URL VALIDE) ===");
                if (adminFromDB != null) {
                    // Utiliser une URL d'avatar valide (placeholder)
                    boolean avatarUpdated = userService.updateAvatar(adminFromDB.getId(),
                            "https://via.placeholder.com/150/007bff/ffffff?text=Admin");
                    System.out.println("Avatar mis à jour? " + avatarUpdated);
                }

                // Recherche d'utilisateurs
                System.out.println("\n=== RECHERCHE D'UTILISATEURS ===");
                var searchResults = userService.searchUsers("admin");
                System.out.println("Résultats de recherche pour 'admin': " + searchResults.size());

                // Tester la connexion
                System.out.println("\n=== TEST DE CONNEXION ===");
                User loggedInUser = userService.seConnecter("admin.profil@test.com", "admin123");
                if (loggedInUser != null) {
                    System.out.println("Connexion réussie pour: " + loggedInUser.getNom() +
                            " | Fullname: " + loggedInUser.getFullname());
                }

                // Compter les utilisateurs avec profil
                System.out.println("\n=== COMPTAGE UTILISATEURS AVEC PROFIL ===");
                int usersWithProfile = userService.countUsersWithProfile();
                System.out.println("Nombre d'utilisateurs avec profil: " + usersWithProfile);

            } else {
                System.out.println("Création des utilisateurs de test...");

                // 1. Créer des utilisateurs avec informations de profil
                System.out.println("\n=== CRÉATION D'UTILISATEURS AVEC PROFIL ===");

                User admin = new User(
                        "Admin Profil",
                        "admin.profil@test.com",
                        "admin123",
                        Role_enum.ADMIN,
                        "Administrateur Principal",
                        "+216 12 345 678",
                        "https://via.placeholder.com/150/007bff/ffffff?text=Admin"
                );
                userService.ajouter(admin);

                User investor = new User(
                        "Invest Profil",
                        "invest.profil@test.com",
                        "invest123",
                        Role_enum.INVESTISSEUR,
                        "Investisseur Gold",
                        "+216 98 765 432",
                        "https://via.placeholder.com/150/28a745/ffffff?text=Invest"
                );
                userService.ajouter(investor);
            }

            // 3. Lire tous les utilisateurs (test final)
            System.out.println("\n=== TOUS LES UTILISATEURS ===");
            var allUsers = userService.read();
            System.out.println("Nombre total d'utilisateurs: " + allUsers.size());

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors des tests: " + e.getMessage());
        }

        // Lancer l'application JavaFX
        launch(args);
    }
}