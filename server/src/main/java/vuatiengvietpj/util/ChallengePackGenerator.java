package vuatiengvietpj.util;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChallengePackGenerator {
    static final String URL = ConfigManager.get("DB_URL");
    static final String USER = ConfigManager.get("DB_USER");
    static final String PASS = ConfigManager.get("DB_PASS");

    static final int MIN_ANSWER_COUNT = 10;

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(URL, USER, PASS);
        Statement st = conn.createStatement();

        // Lấy toàn bộ từ điển ra RAM
        List<String> dictionary = new ArrayList<>();
        ResultSet rs = st.executeQuery("SELECT word FROM Dictionary WHERE CHAR_LENGTH(word) BETWEEN 3 AND 8");
        while (rs.next()) dictionary.add(rs.getString("word").toLowerCase());
        rs.close();

        // Chuẩn bị bản không dấu để so sánh
        List<String> dictionaryNoAccent = dictionary.stream()
                .map(w -> removeAccent(w).replaceAll("\\s+", ""))
                .collect(Collectors.toList());

        Random random = new Random();
        int packCount = 0;

        for (int i = 0; i < 2000 && packCount < 90; i++) { // sinh pack
            String baseWord = dictionary.get(random.nextInt(dictionary.size()));
            String baseNoAccent = removeAccent(baseWord).replaceAll("\\s+", "").trim();

            String quizz = shuffleAndAddLetters(baseNoAccent, random);

            // Lọc ra các từ hợp lệ dựa vào quizz không dấu
            List<String> validWords = new ArrayList<>();
            for (int j = 0; j < dictionary.size(); j++) {
                String noAccent = dictionaryNoAccent.get(j);
                if (canForm(noAccent, quizz)) validWords.add(dictionary.get(j));
            }

            if (validWords.size() >= MIN_ANSWER_COUNT) { // ít nhất n từ có thể tạo
                packCount++;
                int level = quizz.length() <= 6 ? 1 : (quizz.length() <= 8 ? 2 : 3);

                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ChallengePack (quizz, level) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, quizz);
                ps.setInt(2, level);
                ps.executeUpdate();

                ResultSet key = ps.getGeneratedKeys();
                key.next();
                int challengeId = key.getInt(1);

                PreparedStatement pa = conn.prepareStatement(
                    "INSERT INTO Answer (challengePackId, dictionaryWord) VALUES (?, ?)"
                );
                for (String w : validWords) {
                    pa.setInt(1, challengeId);
                    pa.setString(2, w); // từ gốc có dấu
                    pa.addBatch();
                }
                pa.executeBatch();

                System.out.printf("%d. %s (%d từ)\n", packCount, quizz, validWords.size());
            }
        }

        conn.close();
        System.out.println("Hoàn thành sinh challenge pack!");
    }

    // Sinh chuỗi quiz (không dấu)
    static String shuffleAndAddLetters(String base, Random r) {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        Set<Character> set = new HashSet<>();
        for (char c : base.toCharArray()) {
            if (Character.isLetter(c)) set.add(c); // bỏ qua dấu cách hoặc ký tự khác
        }
        while (set.size() < base.length() + 2) {
            set.add(letters.charAt(r.nextInt(letters.length())));
        }
        List<Character> list = new ArrayList<>(set);
        Collections.shuffle(list);
        StringBuilder sb = new StringBuilder();
        for (char c : list) sb.append(c);
        return sb.toString().replaceAll("\\s+", ""); // đảm bảo không có space
    }


    // Kiểm tra xem word có thể tạo từ quizz không (không dấu)
    static boolean canForm(String word, String quizz) {
        Map<Character, Long> wCount = word.chars().mapToObj(c -> (char) c)
            .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        Map<Character, Long> qCount = quizz.chars().mapToObj(c -> (char) c)
            .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        for (var e : wCount.entrySet()) {
            if (qCount.getOrDefault(e.getKey(), 0L) < e.getValue()) return false;
        }
        return true;
    }

    // Hàm xóa dấu tiếng Việt
    static String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");
        return temp.replace("đ", "d").replace("Đ", "D");
    }
}
