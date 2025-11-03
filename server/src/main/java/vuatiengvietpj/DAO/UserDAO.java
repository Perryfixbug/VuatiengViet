package vuatiengvietpj.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import vuatiengvietpj.model.User;

public class UserDAO extends DAO {
    // create user trong db
    public boolean createUser(User user) {
        String sql = "INSERT INTO user (fullName, email, password, createAt, updateAt, totalScore) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error creating user: Database connection is null");
                return false;
            }
            PreparedStatement stmt = con.prepareStatement(sql);
            Instant now = Instant.now();
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setTimestamp(4, Timestamp.from(now));
            stmt.setTimestamp(5, Timestamp.from(now));
            stmt.setInt(6, user.getTotalScore() != null ? user.getTotalScore() : 0);

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
    }

    public boolean changePassword(String email, String newPassword) {
        String sql = "UPDATE user SET password = ?, updateAt = ? WHERE email = ?";

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error changing password: Database connection is null");
                return false;
            }
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

    public boolean updateScore(int userId, int newScore) {
        String sql = "UPDATE user SET totalScore = ?, updateAt = ? WHERE id = ?";

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error updating score: Database connection is null");
                return false;
            }
            PreparedStatement stmt = con.prepareStatement(sql);

            stmt.setInt(1, newScore);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setInt(3, userId);

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

    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error finding user by ID: Database connection is null");
                return null;
            }
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, id);

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
        String sql = "SELECT * FROM user WHERE email = ?";

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error finding user by email: Database connection is null");
                return null;
            }
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
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return null;
    }

    public List<User> getUsers(int offset, int limit) {
        String sql = "SELECT * FROM user ORDER BY totalScore DESC LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error getting users: Database connection is null");
                return users;
            }
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
            System.err.println("Error getting user: " + e.getMessage());
        } finally {
            closeConnection();
        }

        return users;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM user WHERE email = ?";

        try {
            getDBconnection();
            if (con == null) {
                System.err.println("Error checking email exists: Database connection is null");
                return false;
            }
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
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return false;
    }

    // hàm mapping
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("fullName"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));

        Timestamp createAt = rs.getTimestamp("createAt");
        if (createAt != null) {
            user.setCreateAt(createAt.toInstant());
        }

        Timestamp updateAt = rs.getTimestamp("updateAt");
        if (updateAt != null) {
            user.setUpdateAt(updateAt.toInstant());
        }

        user.setTotalScore(rs.getInt("totalScore"));

        return user;
    }
}