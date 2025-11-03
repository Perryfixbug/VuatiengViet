package vuatiengvietpj.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class DictionaryImporter {
    static final String jdbcURL = ConfigManager.get("DB_URL");
    static final String username = ConfigManager.get("DB_USER");
    static final String password = ConfigManager.get("DB_PASS");

    public static void main(String[] args) {

        int batchSize = 1000;
        int count = 0;

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            connection.setAutoCommit(false);

            String sql = "INSERT INTO Dictionary (word, frequency) VALUES (?, 0) " +
                         "ON DUPLICATE KEY UPDATE word = word";
            PreparedStatement statement = connection.prepareStatement(sql);

            // Đọc file từ resources
            InputStream inputStream = DictionaryImporter.class.getResourceAsStream("/vuatiengvietpj/tudien.txt");
            if (inputStream == null) {
                System.err.println("❌ Không tìm thấy file tudien.txt trong resources!");
                return;
            }

            // Đọc đúng UTF-8 (hỗ trợ BOM)
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                // Loại bỏ ký tự BOM (ẩn đầu file)
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                // Bỏ dòng trống
                if (line.trim().isEmpty()) continue;

                // Không động chạm gì khác — giữ nguyên mọi dấu, chữ hoa, chữ thường
                statement.setString(1, line);
                statement.addBatch();

                if (++count % batchSize == 0) {
                    statement.executeBatch();
                    System.out.println("✅ Imported: " + count + " words...");
                }
            }

            reader.close();
            statement.executeBatch();
            connection.commit();

            System.out.println("🎯 Imported total: " + count + " words.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
