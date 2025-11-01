package vuatiengvietpj.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import vuatiengvietpj.model.Dictionary;
import vuatiengvietpj.util.RedisManager;
import vuatiengvietpj.util.ConfigManager;

public class DictionaryDAO extends DAO {
    private static final int DICT_TTL = ConfigManager.getInt("cache.dictionary.ttl", 3600);

    public List<Dictionary> getWordsByCategory(String category) {
        String cacheKey = "dictionary:category:" + category.toLowerCase();

        // Try cache first
        List<Dictionary> cachedWords = RedisManager.getCache(cacheKey, List.class);
        if (cachedWords != null) {
            return cachedWords;
        }

        List<Dictionary> words = new ArrayList<>();
        String sql = "SELECT id, word, meaning, category, difficulty FROM dictionary WHERE category = ? ORDER BY word";

        try {
            getDBconnection();
            if (con == null)
                return words;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, category);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Dictionary dict = new Dictionary(
                                rs.getString("word"),
                                rs.getString("meaning"),
                                rs.getLong("frequency"));
                        words.add(dict);
                    }
                }
            }

            // Cache the result
            RedisManager.setCache(cacheKey, words, DICT_TTL);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return words;
    }

    public List<Dictionary> getRandomWords(int limit) {
        List<Dictionary> words = new ArrayList<>();
        String sql = "SELECT id, word, meaning, category, difficulty FROM dictionary ORDER BY RAND() LIMIT ?";

        try {
            getDBconnection();
            if (con == null)
                return words;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Dictionary dict = new Dictionary(
                                rs.getString("word"),
                                rs.getString("meaning"),
                                rs.getLong("frequency"));
                        words.add(dict);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return words;
    }

    public boolean checkWord(String word) {
        String cacheKey = "word:check:" + word.toLowerCase();

        // Try cache first
        Boolean cached = RedisManager.getCache(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }

        String sql = "SELECT COUNT(*) FROM dictionary WHERE LOWER(word) = ?";
        try {
            getDBconnection();
            if (con == null)
                return false;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, word.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean exists = rs.getInt(1) > 0;
                        // Cache the result
                        RedisManager.setCache(cacheKey, exists, DICT_TTL);
                        return exists;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return false;
    }

    // các methods khác
}