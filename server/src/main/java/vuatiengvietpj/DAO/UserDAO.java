package vuatiengvietpj.dao;

import vuatiengvietpj.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.mindrot.jbcrypt.BCrypt;

public class UserDAO extends DAO {
    // create user trong db
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (full_name, email, password, create_at, update_at, total_score) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            Instant now = Instant.now();
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setTimestamp(4, Timestamp.from(now));
            stmt.setTimestamp(5, Timestamp.from(now));
            stmt.setLong(6, user.getTotalScore() != null ? user.getTotalScore() : 0L);

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }

    public boolean changePassword(String email, String newPassword) {
        String sql = "UPDATE users SET password = ?, update_at = ? WHERE email = ?";

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, email);

            int result = stmt.executeUpdate();
            stmt.close();

            return result > 0;

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }

    public boolean updateScore(Long userId, Long newScore) {
        String sql = "UPDATE users SET total_score = ?, update_at = ? WHERE id = ?";

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);

            stmt.setLong(1, newScore);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setLong(3, userId);

            int result = stmt.executeUpdate();
            stmt.close();

            return result > 0;

        } catch (SQLException e) {
            System.err.println("Error updating score: " + e.getMessage());
            return false;
        } finally {
            closeConnection();
        }
    }

    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setLong(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                rs.close();
                stmt.close();
                return user;
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        } finally {
            closeConnection();
        }

        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                rs.close();
                stmt.close();
                return user;
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        } finally {
            closeConnection();
        }

        return null;
    }

    public List<User> getUsers(int offset, int limit) {
        String sql = "SELECT * FROM users ORDER BY total_score DESC LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting users: " + e.getMessage());
        } finally {
            closeConnection();
        }

        return users;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";

        try {
            getDBconnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                rs.close();
                stmt.close();
                return count > 0;
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error checking email exists: " + e.getMessage());
        } finally {
            closeConnection();
        }

        return false;
    }

    // hàm mapping
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));

        Timestamp createAt = rs.getTimestamp("create_at");
        if (createAt != null) {
            user.setCreateAt(createAt.toInstant());
        }

        Timestamp updateAt = rs.getTimestamp("update_at");
        if (updateAt != null) {
            user.setUpdateAt(updateAt.toInstant());
        }

        user.setTotalScore(rs.getLong("total_score"));

        return user;
    }
}