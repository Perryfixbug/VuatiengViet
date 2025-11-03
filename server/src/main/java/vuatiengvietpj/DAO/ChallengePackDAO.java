package vuatiengvietpj.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import vuatiengvietpj.model.ChallengePack;

public class ChallengePackDAO extends DAO {
    public ChallengePackDAO() {
        getDBconnection();
    }
    public ChallengePack getChallengePackById(Integer cpId) {
        ChallengePack cp = null;
        String sql = "SELECT * FROM ChallengePack WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cp = new ChallengePack();
                    cp.setId(rs.getInt("id"));
                    cp.setQuizz(rs.getString("quizz").toCharArray());
                    cp.setLevel(rs.getInt("level"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cp;
    }
    public Integer getNumberCP(){
        Integer count = 0;
        String sql = "SELECT COUNT(*) AS total FROM challengePack";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    // Lấy danh sách tất cả challenge pack IDs
    public java.util.List<Integer> getAllChallengePackIds() {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        String sql = "SELECT id FROM challengePack";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
    
    // Lấy danh sách đáp án của challenge pack
    public java.util.List<String> getAnswersByChallengePackId(Integer cpId) {
        java.util.List<String> answers = new java.util.ArrayList<>();
        String sql = "SELECT dictionaryWord FROM Answer WHERE challengePackId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(rs.getString("dictionaryWord"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answers;
    }
}
