package controllers;

import entities.GUtilisateurs.User;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class SessionManager {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            System.out.println("Session démarrée pour: " +
                    (user.getFullname() != null && !user.getFullname().isEmpty()
                            ? user.getFullname() : user.getNom()));
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
        System.out.println("Session nettoyée avec succès");
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static String getUserInitials() {
        if (currentUser == null) return "GU";

        String nom = currentUser.getNom();
        if (nom == null || nom.isEmpty()) return "GU";

        String[] parts = nom.split(" ");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0);
        }
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }

    public static String getUserAvatar() {
        if (currentUser != null && currentUser.getAvatar() != null) {
            // Vérifier si l'URL de l'avatar est valide
            String avatar = currentUser.getAvatar();
            if (avatar != null && !avatar.isEmpty() &&
                    (avatar.matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)$") || avatar.startsWith("http"))) {
                return avatar;
            }
        }
        return null;
    }

    public static Image createDefaultAvatarImage(String initials) {
        int size = 36;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();

        // Créer un dégradé circulaire
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double distance = Math.sqrt(Math.pow(x - size/2, 2) + Math.pow(y - size/2, 2));

                if (distance <= size/2) {
                    // Couleur basée sur les initiales (pour avoir une couleur constante par utilisateur)
                    int colorSeed = initials.hashCode();
                    Color color = Color.hsb(colorSeed % 360, 0.7, 0.8);
                    writer.setColor(x, y, color);
                }
            }
        }

        return image;
    }

    // Nouvelle méthode pour obtenir le nom d'affichage
    public static String getDisplayName() {
        if (currentUser == null) return "Utilisateur";

        if (currentUser.getFullname() != null && !currentUser.getFullname().isEmpty()) {
            return currentUser.getFullname();
        }
        return currentUser.getNom();
    }
}