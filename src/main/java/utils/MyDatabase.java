package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDatabase {

    private final String USER = "root";
    private final String PASSWORD = "";
    private final String URL = "jdbc:mysql://localhost:3306/boostup";

    private static MyDatabase instance;
    private Connection connection;

    // Constructeur PRIVATE (Singleton)
    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base boostup établie");
            ensureSchema();
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données boostup");
            e.printStackTrace();
            // Important: on garde connection à null, mais on rend l'erreur claire.
        }
    }

    /**
     * Mini-migration safe: ajoute la colonne `archived` si elle n'existe pas.
     * Objectif: permettre la soft-delete (évènement grisé côté front) sans action manuelle.
     */
    private void ensureSchema() {
        if (connection == null) return;

        // MySQL: ADD COLUMN IF NOT EXISTS n'est pas garanti selon versions.
        // On tente l'ALTER, et si la colonne existe déjà, on ignore l'erreur.
        String alter = "ALTER TABLE evenement ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(alter);
            System.out.println("✅ Migration: colonne evenement.archived ajoutée");
        } catch (SQLException e) {
            // 1060 = Duplicate column name
            if (e.getErrorCode() == 1060) {
                // colonne déjà présente -> rien à faire
            } else {
                System.err.println("⚠️ Migration ignorée (archived): " + e.getMessage());
            }
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException(
                    "Connexion BD non initialisée. Vérifie MySQL (localhost:3306), la base 'boostup', l'utilisateur/mot de passe dans utils.MyDatabase");
        }
        return connection;
    }
}
