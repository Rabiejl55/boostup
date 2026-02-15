package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String USER = "root";
    private final String PASSWORD = "";
    private final String URL =
            "jdbc:mysql://localhost:3306/boostup?useSSL=false&serverTimezone=UTC";

    private static MyDatabase instance;
    private Connection connection;

    public MyDatabase() {
        try {
            // 🔥 important
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database");

        } catch (Exception e) {
            System.out.println("❌ Database connection failed");
            e.printStackTrace();
        }
    }

    public static MyDatabase getInstance(){
        if(instance == null){
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
