package vuatiengvietpj.util;

import java.io.*;
import java.sql.*;
import org.json.JSONObject;

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

            InputStream inputStream = DictionaryImporter.class.getResourceAsStream("/vuatiengvietpj/words.txt");
            if (inputStream == null) {
                System.err.println("words.txt file not found");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    JSONObject obj = new JSONObject(line);
                    String word = obj.getString("text").trim();

                    // Lọc bỏ các dòng không phải từ hợp lệ
                    if (!isValidWord(word)) continue;

                    statement.setString(1, word);
                    statement.addBatch();

                    if (++count % batchSize == 0) {
                        statement.executeBatch();
                        System.out.println("Imported: " + count + " words...");
                    }
                } catch (Exception e) {
                    // Nếu 1 dòng bị lỗi JSON thì bỏ qua
                    continue;
                }
            }

            reader.close();
            statement.executeBatch();
            connection.commit();

            System.out.println("Imported: " + count + " words in total.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Hàm lọc bỏ các chuỗi không phải từ vựng tiếng Việt hợp lệ
    private static boolean isValidWord(String word) {
        if (word.length() < 1 || word.length() > 50) return false;
        if (word.matches(".*[0-9~!@#$%^&*()_=+\\[\\]{}|;:'\",.<>?/\\\\].*")) return false;
        if (word.toLowerCase().startsWith("bản mẫu")) return false;
        return true;
    }
}
