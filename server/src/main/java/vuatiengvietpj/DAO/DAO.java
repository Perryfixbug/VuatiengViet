package vuatiengvietpj.DAO;

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
                
                if (url == null || url.isEmpty()) {
                    System.err.println("Database connection error: DB_URL is not configured in config.properties");
                    return;
                }
                if (username == null || username.isEmpty()) {
                    System.err.println("Database connection error: DB_USER is not configured in config.properties");
                    return;
                }
                
                Class.forName(driver);
                if (password == null || password.isEmpty()) {
                    con = DriverManager.getConnection(url, username, null);
                } else {
                    con = DriverManager.getConnection(url, username, password);
                }
                
                if (con != null) {
                    System.out.println("Database connection established successfully");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Database connection error: JDBC Driver not found - " + e.getMessage());
            e.printStackTrace();
            con = null;
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            con = null;
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