package vuatiengvietpj.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage primaryStage;
    private static UserController userController; // instance chung

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setUserController(UserController uc) {
        userController = uc;
    }

    // load giao diện đăng nhập
    public static void loadAuthScene() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/vuatiengvietpj/UI/Auth.fxml"));
            loader.setController(userController); // set UserController làm controller
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // load trang main sau khi đăng nhập thành công
    public static void loadMainScene() {
        try {
            // Giả sử main.fxml là trang sau khi đăng nhập thành công
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/vuatiengvietpj/UI/Main.fxml"));
            // Nếu cần controller, inject tương tự
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}