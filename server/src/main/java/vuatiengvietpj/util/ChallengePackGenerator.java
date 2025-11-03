package vuatiengvietpj.util;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChallengePackGenerator {
    static final String URL = ConfigManager.get("DB_URL") +
        "?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    static final String USER = ConfigManager.get("DB_USER");
    static final String PASS = ConfigManager.get("DB_PASS");

    static final int MIN_ANSWER_COUNT = 10;   // ít nhất bao nhiêu từ hợp lệ để chấp nhận bộ đề
    static final int TARGET_PACK = 90;        // số bộ đề cần sinh
    static final int MIN_QUIZ_LEN = 5;        // độ dài quiz tối thiểu
    static final int MAX_QUIZ_LEN = 8;        // độ dài quiz tối đa
    static final int MAX_ATTEMPTS = 20000;    // số lần thử sinh (tránh vòng vô hạn)

    static final Set<Character> INVALID_CHARS = Set.of('f', 'j', 'w', 'z');

    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(true);

            // 1️⃣ Tải toàn bộ từ điển
            List<String> dictionary = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT word FROM dictionary WHERE CHAR_LENGTH(word) BETWEEN 2 AND 20")) {
                while (rs.next()) dictionary.add(rs.getString("word").toLowerCase());
            }

            if (dictionary.isEmpty()) {
                System.err.println("❌ Dictionary rỗng. Dừng.");
                return;
            }

            // 2️⃣ Chuẩn hóa từ không dấu
            List<String> dictNoAccent = dictionary.stream()
                    .map(ChallengePackGenerator::normalizeWord)
                    .collect(Collectors.toList());

            System.out.println("✅ Đã tải " + dictionary.size() + " từ từ điển.");
            System.out.println("🚀 Bắt đầu sinh " + TARGET_PACK + " bộ đề...");

            Random random = new Random();
            Set<String> usedQuiz = new HashSet<>();
            int packCount = 0;
            int attempts = 0;

            while (packCount < TARGET_PACK && attempts < MAX_ATTEMPTS) {
                attempts++;

                // 3️⃣ Sinh quiz ngẫu nhiên (chỉ dùng ký tự hợp lệ)
                String quiz = randomQuiz(random);
                if (usedQuiz.contains(quiz)) continue;
                usedQuiz.add(quiz);

                // 4️⃣ Lọc đáp án hợp lệ: có thể tạo từ quiz
                List<String> valid = new ArrayList<>();
                for (int i = 0; i < dictionary.size(); i++) {
                    String wordNorm = dictNoAccent.get(i);
                    if (wordNorm.isEmpty()) continue;
                    if (canForm(wordNorm, quiz)) valid.add(dictionary.get(i));
                }

                // 5️⃣ Nếu đạt yêu cầu thì lưu vào DB
                if (valid.size() >= MIN_ANSWER_COUNT) {
                    packCount++;
                    int level = getLevel(quiz.length());
                    savePack(conn, quiz, level, valid);
                    System.out.printf("✅ %2d. Quiz: %-8s (%3d đáp án)\n", packCount, quiz, valid.size());
                }

                if (attempts % 2000 == 0) {
                    System.out.println("⏱ vẫn đang sinh... attempts=" + attempts + ", packs=" + packCount);
                }
            }

            System.out.println("🎉 Hoàn thành: sinh được " + packCount + " bộ đề.");
            if (packCount < TARGET_PACK) {
                System.out.println("⚠️ Gợi ý: nếu không đủ, giảm MIN_ANSWER_COUNT hoặc tăng MAX_ATTEMPTS hoặc giảm MIN_QUIZ_LEN.");
            }
        }
    }

    // === Lưu pack & đáp án ===
    static void savePack(Connection conn, String quiz, int level, List<String> validWords) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO challengepack (quizz, level) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz);
            ps.setInt(2, level);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không lấy được generated key");
                int id = rs.getInt(1);

                try (PreparedStatement pa = conn.prepareStatement(
                        "INSERT IGNORE INTO answer (challengePackId, dictionaryWord) VALUES (?, ?)")) {
                    for (String w : validWords) {
                        pa.setInt(1, id);
                        pa.setString(2, w);
                        pa.addBatch();
                    }
                    pa.executeBatch();
                }
            }
        }
    }

    // === Sinh quiz chỉ chứa chữ hợp lệ (ko f, j, w, z) ===
    static String randomQuiz(Random r) {
        String letters = "abcdeghiklmnopqrstuvxy"; // bỏ f, j, w, z
        int len = MIN_QUIZ_LEN + r.nextInt(MAX_QUIZ_LEN - MIN_QUIZ_LEN + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(letters.charAt(r.nextInt(letters.length())));
        }
        return sb.toString();
    }

    // === Kiểm tra word có thể được tạo từ quiz ===
    static boolean canForm(String word, String quiz) {
        int[] q = new int[26];
        for (char c : quiz.toCharArray()) {
            if (c >= 'a' && c <= 'z') q[c - 'a']++;
        }
        for (char c : word.toCharArray()) {
            if (c < 'a' || c > 'z') return false;
            if (INVALID_CHARS.contains(c)) return false;
            if (q[c - 'a'] <= 0) return false;
            q[c - 'a']--;
        }
        return true;
    }

    // === Chuẩn hóa bỏ dấu ===
    static String normalizeWord(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(temp).replaceAll("");
        temp = temp.replace('đ', 'd').replace('Đ', 'D');
        temp = temp.toLowerCase();
        temp = temp.replaceAll("[^a-z]", "");
        return temp;
    }

    // === Đánh cấp độ theo độ dài quiz ===
    static int getLevel(int len) {
        if (len <= 5) return 1;
        else if (len <= 7) return 2;
        else return 3;
    }
}
