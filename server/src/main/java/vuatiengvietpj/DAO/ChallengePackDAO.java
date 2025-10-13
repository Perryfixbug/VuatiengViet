package vuatiengvietpj.dao;

import vuatiengvietpj.model.*;
import java.sql.*;

public class ChallengePackDAO extends DAO {
    public ChallengePackDAO() {
        getDBconnection();
    }
    public ChallengePack getChallengePackById(Long cpId) {
        ChallengePack cp = null;
        String sql = "SELECT * FROM ChallengePack WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cpId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cp = new ChallengePack();
                    cp.setId(rs.getLong("id"));
                    cp.setQuizz(rs.getString("quizz").toCharArray());
                    cp.setLevel(rs.getInt("level"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cp;
    }
    public int getNumberCP(){
        int count = 0;
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
    
    
}
