package vuatiengvietpj;

import vuatiengvietpj.controller.SceneManager;
import vuatiengvietpj.controller.UserController;

import java.io.IOException;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private UserController userController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        String host = "localhost";
        int port = 2208;
        try {
            userController = new UserController(host, port);
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return;
        }
        SceneManager.setPrimaryStage(primaryStage);
        SceneManager.setUserController(userController);
        SceneManager.loadAuthScene();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
// ...existing code...