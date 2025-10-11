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
                String url = ConfigManager.get("db.url");
                String username = ConfigManager.get("db.username");
                String password = ConfigManager.get("db.password");
                String driver = ConfigManager.get("db.driver", "com.mysql.cj.jdbc.Driver");

                Class.forName(driver);
                con = DriverManager.getConnection(url, username, password);
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