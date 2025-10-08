package vuatiengvietpj.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServerApp {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/vuatiengvietdb"; // đổi port & db name nếu cần
        String user = "root";
        String pass = "****";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("✅ Kết nối MySQL thành công!");

            String sql = "SELECT * FROM users";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println("User: " + rs.getString("username"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
