package controllers;

import entities.GUtilisateurs.User;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class SessionManager {
    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    /**
     * Crée une image avatar par défaut avec les initiales
     */
    public static Image createDefaultAvatarImage(String initials) {
        int size = 80;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();

        // Gradient bleu
        Color color1 = Color.web("#1b2a4a");
        Color color2 = Color.web("#2d1b4e");

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double ratio = (double)(x + y) / (2.0 * size);
                Color mixed = color1.interpolate(color2, ratio);
                writer.setColor(x, y, mixed);
            }
        }

        return image;
    }
}

