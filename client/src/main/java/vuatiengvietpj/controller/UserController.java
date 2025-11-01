package vuatiengvietpj.controller;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.Instant;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.User;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import vuatiengvietpj.controller.UserController;

public class UserController extends ClientController {
    private String module = "USER";
    private Gson gson;

    @FXML
    private TextField loginEmail, signupEmail, signupName, changePasswordEmail;
    @FXML
    private PasswordField loginPassword, signupPassword, oldPassword, newPassword;
    @FXML
    private Label statusLabel;
    @FXML
    private TabPane tabPane;

    public UserController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    public Response login(String email, String password) {
        try {
            User userLogin = new User();
            userLogin.setEmail(email);
            userLogin.setPassword(password);

            // Gửi request và nhận response
            return sendAndReceive(module, "LOGIN", gson.toJson(userLogin));
        } catch (Exception e) {
            System.err.println("Loi dang nhap: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Response alive(String userId, String email) {
        try {
            return sendAndReceive("USER", "ALIVE", userId + "," + email);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        return null;
    }

    public Response signup(String email, String fullName, String password) {
        try {

            User signupUser = new User();
            signupUser.setEmail(email);
            signupUser.setFullName(fullName);
            signupUser.setPassword(password);

            return sendAndReceive(module, "SIGNUP", gson.toJson(signupUser));

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

    public Response changePassword(String email, String oldPassword, String newPassword) {
        try {

            String data = email + "," + oldPassword + "," + newPassword;
            return sendAndReceive(module, "CGPASS", data);

        } catch (Exception e) {
            System.err.println("Loi doi mat khau: " + e.getMessage());
        }

        return null;
    }

    public boolean logout(Long userId) {
        try {
            Response response = sendAndReceive(module, "LOGOUT", userId.toString());

            if (response.isSuccess()) {
                System.out.println("Dang xuat thanh cong " + userId);
                return true;
            } else {
                System.out.println("Dang xuat that bai: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("loi dang xuat: " + e.getMessage());
        }

        return false;
    }

    public void disconnect() {
        close();
    }

    @FXML
    void onLogin(ActionEvent event) {
        String email = loginEmail.getText().trim();
        String pw = loginPassword.getText();
        if (email.isEmpty() || pw.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        setUiEnabled(false);
        statusLabel.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                Response resp = login(email, pw);
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    if (resp != null && resp.isSuccess()) {
                        SceneManager.loadMainScene();
                    } else {
                        statusLabel.setText(resp == null ? "Lỗi kết nối" : resp.getData());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    statusLabel.setText("Lỗi: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void onSignup(ActionEvent event) {
        String email = signupEmail.getText().trim();
        String name = signupName.getText().trim();
        String pw = signupPassword.getText();
        if (email.isEmpty() || name.isEmpty() || pw.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        setUiEnabled(false);
        statusLabel.setText("Đang đăng ký...");

        new Thread(() -> {
            try {
                Response resp = signup(email, name, pw);
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    if (resp != null && resp.isSuccess()) {
                        statusLabel.setText("Đăng ký thành công! Vui lòng đăng nhập.");
                        tabPane.getSelectionModel().select(0);
                    } else {
                        statusLabel.setText(resp == null ? "Lỗi kết nối" : resp.getData());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    statusLabel.setText("Lỗi: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void onChangePassword(ActionEvent event) {
        String email = changePasswordEmail.getText().trim();
        String oldPw = oldPassword.getText();
        String newPw = newPassword.getText();
        if (email.isEmpty() || oldPw.isEmpty() || newPw.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ");
            return;
        }
        setUiEnabled(false);
        statusLabel.setText("Đang đổi mật khẩu...");

        new Thread(() -> {
            try {
                Response resp = changePassword(email, oldPw, newPw);
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    if (resp != null && resp.isSuccess()) {
                        statusLabel.setText("Đổi mật khẩu thành công!");
                    } else {
                        statusLabel.setText(resp == null ? "Lỗi kết nối" : resp.getData());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    statusLabel.setText("Lỗi: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void setUiEnabled(boolean enabled) {
        tabPane.setDisable(!enabled);
    }

}