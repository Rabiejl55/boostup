package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;

    // ⚠️ Mets tes infos DB ici
    private static final String URL  = "jdbc:mysql://localhost:3306/boostup?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDatabase() {}

    public static synchronized MyDatabase getInstance() {
        if (instance == null) instance = new MyDatabase();
        return instance;
    }

    /** Retourne TOUJOURS une connexion valide (nouvelle). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}