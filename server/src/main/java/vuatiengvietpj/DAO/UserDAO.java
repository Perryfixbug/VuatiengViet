package vuatiengvietpj.DAO;

import vuatiengvietpj.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    private String dbUrl = "jdbc:mysql://localhost:3306/VUATIENGVIET?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false";
    private String dbClass = "com.mysql.cj.jdbc.Driver";
    private String username = "root";
    private String password = "123456";
    private Connection con;

    public void getDBconnection() {
        try {
            // chỉ tạo connection khi chưa có hoặc đã đóng
            if (con == null || con.isClosed()) {
                Class.forName(dbClass);
                con = DriverManager.getConnection(dbUrl, username, password);
            }
            Class.forName(dbClass);
            con = DriverManager.getConnection(dbUrl, username, password);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (con != null && !con.isClosed())
                con.close();
        } catch (Exception ignored) {
        }
    }

    // hàm kiểm tra trạng thái tài khoản
    public int checkUser(User user) {
        try {
            String sql = "SELECT password FROM users WHERE email = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, user.getEmail());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return 0; // user không tồn tại
                    }
                    String dbPass = rs.getString("password");
                    if (dbPass.equals(user.getPassword())) {
                        return 2; // thành công
                    } else {
                        return 1; // sai mật khẩu
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 3;
    }

    public List<User> getListUser() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, full_Name, email, password FROM users";
        try {
            getDBconnection();
            if (con == null)
                return users;

            try (PreparedStatement ps = con.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User(rs.getLong("id"), rs.getString("full_Name"), rs.getString("email"),
                            rs.getString("password"));
                    users.add(u);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection();
        }
        return users;
    }

    public boolean create(User user) {
        try {
            getDBconnection();
            if (user.getId() == 0) // trường hợp id = 0 là khi gửi lên user đăng ký mới
            {
                long newId = System.currentTimeMillis();
                user.setId(newId);
                if (checkUser(user) == 0) // user chưa tồn tại
                {
                    String insertSql = "INSERT INTO users(id, full_Name, email, password) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                        ps.setLong(1, user.getId());
                        ps.setString(2, user.getFullName());
                        ps.setString(3, user.getEmail());
                        ps.setString(4, user.getPassword());
                        int affected = ps.executeUpdate();
                        return affected > 0;
                    }

                }

            }
        } catch (Exception e) {

            System.err.println(e);

        }
        return false;
    }

    public boolean save(User user) {
        try {
            String updateSql = "UPDATE users SET full_Name = ?, email = ?, password = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPassword());
                ps.setLong(4, user.getId());
                int affected = ps.executeUpdate();
                return affected > 0;
            }
        } catch (Exception e) {

            System.err.println(e);
            return false;
        }
    }

    public List<User> findByEmail(String email) {
        List<User> users = new ArrayList<>();
        if (email == null || email.isEmpty())
            return users;

        String sql = "SELECT id, full_Name, email, password FROM users WHERE LOWER(email) LIKE ?";
        try {
            getDBconnection();
            if (con == null)
                return users;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + email.toLowerCase() + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        User u = new User(rs.getLong("id"), rs.getString("full_Name"), rs.getString("email"),
                                rs.getString("password"));
                        users.add(u);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection();
        }
        return users;
    }

    public List<User> findByName(String name) {
        List<User> users = new ArrayList<>();
        if (name == null || name.isEmpty())
            return users;

        String sql = "SELECT id, full_Name, email, password FROM users WHERE LOWER(full_Name) LIKE ?";
        try {
            getDBconnection();
            if (con == null)
                return users;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + name.toLowerCase() + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        User u = new User(rs.getLong("id"), rs.getString("full_Name"), rs.getString("email"),
                                rs.getString("password"));
                        users.add(u);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection();
        }
        return users;
    }

    public User getUserbyEmail(String email) {
        if (email == null || email.isEmpty())
            return null;
        String sql = "SELECT id, full_Name, email, password FROM users WHERE email = ?";
        try {
            getDBconnection();
            if (con == null)
                return null;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new User(
                                rs.getLong("id"),
                                rs.getString("full_Name"),
                                rs.getString("email"),
                                rs.getString("password"));
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            closeConnection();
        }

    }

    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        dao.getDBconnection();
        List<User> list = dao.getListUser();
        for (User u : list) {
            System.out.println(u);
        }

        User u = new User(0, "Trần Minh Hiếu", "TMHaaa@gmail.com", BCrypt.hashpw("hieu123", BCrypt.gensalt(12)));
        dao.create(u);
        list = dao.getListUser();
        for (User p : list) {
            System.out.println(p);
        }

    }

}
