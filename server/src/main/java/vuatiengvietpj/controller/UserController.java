package vuatiengvietpj.controller;

import java.net.*;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.mindrot.jbcrypt.BCrypt;

import vuatiengvietpj.model.User;
import vuatiengvietpj.dao.UserDAO;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.util.SessionManager;

public class UserController extends ServerController {
    private UserDAO userDAO = new UserDAO();
    private String module = "USER";
    private Gson gson;

    public UserController(Socket clientSocket) throws java.io.IOException {
        super(clientSocket);
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();

    }

    @Override
    protected Response process(Request request) throws java.io.IOException {
        String data = request.getData();
        String ip = request.getIp();
        return switch (request.getMaLenh()) {
            case "LOGIN" -> handleLogin(data, ip);
            case "SIGNUP" -> handleSignUp(data);
            case "LOGOUT" -> handleLogOut(data);
            case "CGPASS" -> handleChangePassword(data);
            case "ALIVE" -> handleCheckAlive(data);
            // case "FGPASS" -> handleForgetPassword(data);
            default -> createErrorResponse(module, request.getMaLenh(), "Hành động không hợp lệ");
        };
    }

    // check client còn sống k
    public Response handleCheckAlive(String data) {
        return createSuccessResponse(module, "ALIVE", "Server know that you’re alive: " + data);
    }

    // đăng nhập
    public Response handleLogin(String data, String ip) {
        User loginUser = gson.fromJson(data, User.class);
        User userChecker = userDAO.findByEmail(loginUser.getEmail());
        if (userChecker == null) {
            return createErrorResponse(module, "LOGIN", "Tai khoan hoac mat khau khong dung");
        } else if (BCrypt.checkpw(loginUser.getPassword(), userChecker.getPassword())) {
            if (SessionManager.isLoggedIn(userChecker.getId())) {
                SessionManager.destroy(userChecker.getId());
                // Hủy session cũ
            }
            SessionManager.createOrUpdate(userChecker, ip); // cập nhật session với IP của thiết bị mới
            return createSuccessResponse(module, "LOGIN", gson.toJson(userChecker));
        } else {
            return createErrorResponse(module, "LOGIN", "Tai khoan hoac mat khau khong dung");
        }
    }

    // đăng xuất
    public Response handleLogOut(String data) {
        Long userId = Long.parseLong(data);
        boolean ok = SessionManager.destroy(userId); // hủy session
        return ok ? createSuccessResponse(module, "LOGOUT", "OK")
                : createErrorResponse(module, "LOGOUT", "Not logged in");
    }

    // đăng ký
    public Response handleSignUp(String data) {
        User sigupUser = gson.fromJson(data, User.class);
        sigupUser.setPassword(BCrypt.hashpw(sigupUser.getPassword(), BCrypt.gensalt(12)));
        if (userDAO.emailExists(sigupUser.getEmail())) {
            return createErrorResponse(module, "SIGNUP", "EMAIL da ton tai");
        } else {
            userDAO.createUser(sigupUser);

            User newUser = userDAO.findByEmail(sigupUser.getEmail());
            return createSuccessResponse(module, "SIGNUP", gson.toJson(newUser));
        }
    }

    // đổi mật khẩu
    public Response handleChangePassword(String data) {
        String parts[] = data.trim().split(",");
        User user = userDAO.findByEmail(parts[0]);
        if (user != null) {
            if (BCrypt.checkpw(parts[1], user.getPassword())) {
                userDAO.changePassword(parts[0], BCrypt.hashpw(parts[2], BCrypt.gensalt(12)));
                user = userDAO.findByEmail(parts[0]);
                return createSuccessResponse(module, "CGPASS", gson.toJson(user));
            }
        }
        return createErrorResponse(module, "CGPASS", "DOI MAT KHAU KHONG THANH CONG");

    }

    // email
    // public void handleForgetPassword(String str) {
    // try {
    // String[] parts = str.split(",");
    // String email = parts[0].trim();
    // // Kiểm tra email có tồn tại
    // User user = userDAO.findByEmail(email);
    // if (user != null) {
    // // Tạo mật khẩu mới ngẫu nhiên (6 ký tự)
    // String newPassword = generateRandomPassword();

    // // Cập nhật mật khẩu mới
    // boolean success = userDAO.changePassword(user.getEmail(),
    // BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));

    // if (success) {
    // System.out.println("Mat khau moi cua email" + user.getEmail() + " "
    // + newPassword);
    // } else {

    // }
    // } else {
    // System.out.println("Email không tồn tại: " + email);
    // }

    // } catch (Exception e) {
    // System.err.println("Lỗi handleForgetPassword: " + e.getMessage());
    // }
    // }

    // private String generateRandomPassword() {
    // String chars =
    // "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    // StringBuilder password = new StringBuilder();

    // for (int i = 0; i < 6; i++) {
    // int index = (int) (Math.random() * chars.length());
    // password.append(chars.charAt(index));
    // }

    // return password.toString();
    // }

}