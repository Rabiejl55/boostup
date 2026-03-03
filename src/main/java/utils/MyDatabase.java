package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;

    private final String USER = "root";
    private final String PASSWORD = "";
    private final String URL = "jdbc:mysql://localhost:3306/4eme?useSSL=false&serverTimezone=UTC";

    private MyDatabase() {}

    public static MyDatabase getInstance() {
        if (instance == null) instance = new MyDatabase();
        return instance;
    }

    /** ✅ NO CACHE: une nouvelle connexion à chaque fois (safe avec Task/threads) */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}