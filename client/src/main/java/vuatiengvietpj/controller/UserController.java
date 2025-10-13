package vuatiengvietpj.controller;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.Instant;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.User;

public class UserController extends ClientController {
    private String module = "USER";
    private Gson gson;

    public UserController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    public User login(String email, String password) {
        try {
            // Hash password trước khi gửi

            User userLogin = new User();
            userLogin.setEmail(email);
            userLogin.setPassword(password);

            // Gửi request và nhận response
            Response response = sendAndReceive(module, "LOGIN", gson.toJson(userLogin));

            if (response.isSuccess()) {
                // ✅ Parse JSON từ response.getData() thành User object
                User user = gson.fromJson(response.getData(), User.class);
                System.out.println("Dang nhap thanh cong: " + user);
                return user;
            } else {
                System.out.println("Dang nhap that bai " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Loi dang nhap: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public User signup(String email, String fullName, String password) {
        try {

            User signupUser = new User();
            signupUser.setEmail(email);
            signupUser.setFullName(fullName);
            signupUser.setPassword(password);

            Response response = sendAndReceive(module, "SIGNUP", gson.toJson(signupUser));

            if (response.isSuccess()) {
                User user = gson.fromJson(response.getData(), User.class);
                System.out.println("Dang ki thanh cong: " + user.getFullName());
                return user;

            } else {
                System.out.println("Dang ky that bai: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Loi dang ky: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // public String forgetPassword(String email) {
    // try {
    // Response response = sendAndReceive(module, "FGPASS", email);

    // if (response.isSuccess()) {
    // System.out.println("✅ " + response.getData());

    // // ✅ Parse JSON hoặc extract password từ message
    // String data = response.getData();

    // if (data.startsWith("{")) {
    // // JSON format: {"message":"Mật khẩu mới", "newPassword":"ABC123"}
    // try {
    // var jsonResponse = gson.fromJson(data, com.google.gson.JsonObject.class);
    // if (jsonResponse.has("newPassword")) {
    // return jsonResponse.get("newPassword").getAsString();
    // }
    // } catch (Exception ignored) {}
    // }

    // // Text format: "Mật khẩu mới: ABC123"
    // if (data.contains(":")) {
    // return data.split(":")[1].trim();
    // }

    // return data;
    // } else {
    // System.out.println("❌ Quên mật khẩu thất bại: " + response.getData());
    // }

    // } catch (Exception e) {
    // System.err.println("❌ Lỗi quên mật khẩu: " + e.getMessage());
    // }

    // return null;
    // }

    public boolean changePassword(String email, String oldPassword, String newPassword) {
        try {

            String data = email + "," + oldPassword + "," + newPassword;

            Response response = sendAndReceive(module, "CGPASS", data);

            if (response.isSuccess()) {
                System.out.println("Doi mat khau thanh cong");
                return true;
            } else {
                System.out.println("Doi mat khau that bai: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Loi doi mat khau: " + e.getMessage());
        }

        return false;
    }

    public boolean logout() {
        try {
            Response response = sendAndReceive(module, "LOGOUT", "");

            if (response.isSuccess()) {
                System.out.println("✅ Đăng xuất thành công");
                return true;
            } else {
                System.out.println("❌ Đăng xuất thất bại: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi đăng xuất: " + e.getMessage());
        }

        return false;
    }

    /**
     * ✅ Đóng kết nối
     */
    public void disconnect() {
        close();
    }
}