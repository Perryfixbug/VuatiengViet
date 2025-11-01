package vuatiengvietpj.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

// Main controller giao diện chính (home)
public class MainController {
    private String host = "localhost";
    private int port = 2208;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button startBtn;

    public void handleLogout(ActionEvent event) throws IOException {
        UserController userController = new UserController(host, port);
        SceneManager.setUserController(userController);
        SceneManager.loadAuthScene();
    }

    public void handleStart(ActionEvent event) {

    }
}
