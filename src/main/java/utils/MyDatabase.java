package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données boostup");
            e.printStackTrace();
            // Important: on garde connection à null, mais on rend l'erreur claire.
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
