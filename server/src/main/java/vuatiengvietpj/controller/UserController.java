package vuatiengvietpj.controller;

import java.net.Socket;
import java.time.Instant;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import vuatiengvietpj.dao.UserDAO;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.User;
import vuatiengvietpj.util.SessionManager;

public class UserController extends ServerController {
    private UserDAO userDAO = new UserDAO();
    private String module = "USER";
    private Gson gson;

    public UserController(Socket clientSocket) throws java.io.IOException {
        // xóa super(clientSocket);
        this.clientSocket = clientSocket; // thêm nếu cần access socket
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    @Override
    public Response process(Request request) throws java.io.IOException {
        String data = request.getData();
        String ip = request.getIp();
        return switch (request.getMaLenh()) {
            case "LOGIN" -> handleLogin(data, ip);
            case "SIGNUP" -> handleSignUp(data);
            case "LOGOUT" -> handleLogOut(data);
            case "CGPASS" -> handleChangePassword(data);
            case "ALIVE" -> handleCheckAlive(data);
            case "ONUSER" -> handleGetOnlineUsers();
            case "GETIN4" -> handleGetIn4(data);
            // case "FGPASS" -> handleForgetPassword(data);
            default -> createErrorResponse(module, request.getMaLenh(), "Hành động không hợp lệ");
        };
    }

    public Response handleGetIn4(String data) {
        return createSuccessResponse(module, "GETIN4", gson.toJson(userDAO.findById(Integer.parseInt(data))));
    }

    // check client còn sống k
    public Response handleCheckAlive(String data) {
        String x[] = data.trim().split(" ");
        if (SessionManager.checkSessionId(x[1], Integer.parseInt(x[0]))) {
            return createSuccessResponse(module, "ALIVE", "Server know that you’re alive: " + data);
        } else {
            return createErrorResponse(module, "ALIVE", "Phien lam viec het han vui long dang nhap lai");
        }
    }

    // đăng nhập
    public Response handleLogin(String data, String ip) {
        try {
            User loginUser = gson.fromJson(data, User.class);
            System.out.println("UserController.handleLogin: attempting login for email=" + loginUser.getEmail());

            User userChecker = userDAO.findByEmail(loginUser.getEmail());
            System.out.println("UserController.handleLogin: userChecker=" + (userChecker == null ? "null" : "found"));

            if (userChecker == null) {
                return createErrorResponse(module, "LOGIN", "Tai khoan hoac mat khau khong dung");
            } else if (BCrypt.checkpw(loginUser.getPassword(), userChecker.getPassword())) {
                if (SessionManager.isLoggedIn(userChecker.getId())) {
                    SessionManager.destroy(userChecker.getId());
                    // Hủy session cũ
                }
                SessionManager.createOrUpdate(userChecker, ip); // cập nhật session với IP của thiết bị mới
                System.out.println("UserController.handleLogin: login successful for userId=" + userChecker.getId());
                return createSuccessResponse(module, "LOGIN",
                        gson.toJson(userChecker) + "###" + SessionManager.getSessionId(userChecker.getId()));
            } else {
                return createErrorResponse(module, "LOGIN", "Tai khoan hoac mat khau khong dung");
            }
        } catch (Exception e) {
            System.err.println("UserController.handleLogin ERROR: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(module, "LOGIN", "Server error: " + e.getMessage());
        }
    }

    // đăng xuất
    public Response handleLogOut(String data) {
        Integer userId = Integer.parseInt(data);
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

    public Response handleGetOnlineUsers() {
        List<User> users = SessionManager.getOnlineUsers();
        return createSuccessResponse(module, "ONUSER", gson.toJson(users));
    }

}