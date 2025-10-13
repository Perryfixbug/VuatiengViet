package vuatiengvietpj.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import vuatiengvietpj.util.ConfigManager;

public abstract class DAO {
    protected Connection con;

    protected void getDBconnection() {
        try {
            if (con == null || con.isClosed()) {
                String url = ConfigManager.get("DB_URL");
                String username = ConfigManager.get("DB_USER");
                String password = ConfigManager.get("DB_PASS");
                String driver = ConfigManager.get("db.driver", "com.mysql.cj.jdbc.Driver");

                System.out.println("DB_URL: " + url);
                System.out.println("DB_USER: " + username);
                System.out.println("DB_PASS: " + password);

                Class.forName(driver);
                if (password == null || password.isEmpty()) {
                    con = DriverManager.getConnection(url, username, null);
                } else {
                    con = DriverManager.getConnection(url, username, password);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void closeConnection() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}