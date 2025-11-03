package vuatiengvietpj.util;

import java.io.*;
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

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (word.isEmpty()) continue;

                // Lọc bỏ các dòng không hợp lệ
                if (!isValidWord(word)) continue;

                statement.setString(1, word);
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

    // Hàm lọc bỏ các chuỗi không phải từ vựng hợp lệ
    private static boolean isValidWord(String word) {
        if (word.length() < 1 || word.length() > 100) return false;

        // Không chứa ký tự lạ hoặc số
        if (word.matches(".*[0-9~!@#$%^&*()_=+\\[\\]{}|;:'\",.<>?/\\\\].*")) return false;

        // Loại các dòng có tiền tố đặc biệt
        String lower = word.toLowerCase();
        if (lower.startsWith("bản mẫu")) return false;
        if (lower.contains("http") || lower.contains("www")) return false;

        return true;
    }
}
