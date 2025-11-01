package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.User;

// controller giao diện chính (home)
public class HomeController {
    private String host = "localhost";
    private int port = 2208;
    private Stage primaryStage;
    private User currentUser;
    private UserController userController; // thêm

    @FXML
    private Button logoutBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userEmailLabel;
    @FXML
    private Label userScoreLabel;
    @FXML
    private TableView<User> onlineTable; // thêm

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Update labels khi set user
        if (userNameLabel != null)
            userNameLabel.setText("Tên: " + user.getFullName());
        if (userEmailLabel != null)
            userEmailLabel.setText("Email: " + user.getEmail());
        if (userScoreLabel != null)
            userScoreLabel.setText("Tổng thành tích: " + user.getTotalScore());
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        // Khởi tạo UserController để gọi getOnlineUsers
        try {
            userController = new UserController("localhost", 2208);
        } catch (Exception e) {
            System.err.println();
        }
        setupOnlineTable();
        loadOnlineUsers();
    }

    private void setupOnlineTable() {
        TableColumn<User, Long> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(50);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<User, String> nameCol = new TableColumn<>("Tên");
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(200);
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<User, Long> scoreCol = new TableColumn<>("Điểm");
        scoreCol.setPrefWidth(80);
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

        onlineTable.getColumns().addAll(idCol, nameCol, emailCol, scoreCol);
    }

    private void loadOnlineUsers() {
        try {
            Response resp = userController.getOnlineUsers();
            if (resp != null && resp.isSuccess()) {
                List<User> users = userController.parseUsers(resp.getData()); // giả sử có method parse
                ObservableList<User> userList = FXCollections.observableArrayList(users);
                onlineTable.setItems(userList);
            } else {
                System.err.println("Lỗi tải online users: " + (resp != null ? resp.getData() : "null"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi load online users: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        // Load Auth scene để logout (quay về login)
        if (userController != null) {
            userController.disconnect(); // đóng connection
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/Auth.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Auth");
        Object controller = loader.getController();
        if (controller instanceof UserController) {
            ((UserController) controller).setPrimaryStage(primaryStage);
            ((UserController) controller).logout(currentUser.getId());
        }
        primaryStage.show();
    }

    @FXML
    public void handleStart(ActionEvent event) throws IOException {
        // Load room list scene
        if (userController != null) {
            userController.disconnect(); // đóng connection
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/ListRoom.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Object controller = loader.getController();
        if (controller instanceof ListRoomController) {
            ((ListRoomController) controller).setCurrentUserId(currentUser.getId());
            ((ListRoomController) controller).setPrimaryStage(primaryStage);
        }
        primaryStage.setScene(scene);
        primaryStage.setTitle("Room List");
        primaryStage.show();
    }
}