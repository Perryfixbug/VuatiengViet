package vuatiengvietpj.controller;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.User;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class UserController extends ClientController {
    private String module = "USER";
    private Gson gson;
    private Stage primaryStage;

    @FXML
    private TextField loginEmail, signupEmail, signupName, changePasswordEmail;
    @FXML
    private PasswordField loginPassword, signupPassword, oldPassword, newPassword;
    @FXML
    private Label statusLabel;
    @FXML
    private TabPane tabPane;

    public UserController() throws IOException { // thêm no-arg constructor
        this("localhost", 2208); // default host/port
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    public UserController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
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
                System.out.println(resp);
                Platform.runLater(() -> {
                    setUiEnabled(true);
                    if (resp != null && resp.isSuccess()) {
                        try {
                            User loggedInUser = gson.fromJson(resp.getData(), User.class);
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/Home.fxml"));
                            Parent root = loader.load();
                            Scene scene = new Scene(root);

                            Object controller = loader.getController();
                            if (controller instanceof HomeController) {
                                ((HomeController) controller).setCurrentUser(loggedInUser);
                                ((HomeController) controller).setPrimaryStage(primaryStage);
                            }

                            primaryStage.setScene(scene);
                            primaryStage.show();
                        } catch (IOException e) {
                            statusLabel.setText("Lỗi load main scene: " + e.getMessage());
                        }
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

    public Response login(String email, String password) {
        try {
            User userLogin = new User();
            userLogin.setEmail(email);
            userLogin.setPassword(password);
            System.out.println(userLogin);
            // Gửi request và nhận response
            Response response = sendAndReceive(module, "LOGIN", gson.toJson(userLogin));
            return response;
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

    public Response changePassword(String email, String oldPassword, String newPassword) {
        try {

            String data = email + "," + oldPassword + "," + newPassword;
            return sendAndReceive(module, "CGPASS", data);

        } catch (Exception e) {
            System.err.println("Loi doi mat khau: " + e.getMessage());
        }

        return null;
    }

    public boolean logout(Integer userId) {
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

    public User getIn4(Integer userId) {
        try {
            Response response = sendAndReceive(module, "GETIN4", userId.toString());
            User user = gson.fromJson(response.getData(), User.class);
            return user;
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    public Response getOnlineUsers() {
        try {
            return sendAndReceive(module, "ONUSER", "");
        } catch (Exception e) {
            System.err.println("Lỗi lấy danh sách online: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<User> parseUsers(String json) {
        try {
            return gson.fromJson(json, new TypeToken<List<User>>() {
            }.getType());
        } catch (Exception e) {
            System.err.println("Lỗi parse users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void disconnect() {
        close();
    }

}