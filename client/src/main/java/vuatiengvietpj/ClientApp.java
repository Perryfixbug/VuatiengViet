package vuatiengvietpj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vuatiengvietpj.controller.ListRoomController;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/ListRoom.fxml"));
        Parent root = loader.load();

        // set a dummy logged-in user id for testing so create/join buttons work
        Object controller = loader.getController();
        if (controller instanceof ListRoomController) {
            ((ListRoomController) controller).setCurrentUserId(1L);
        }

        Scene scene = new Scene(root);
        stage.setTitle("Room List");
        stage.setScene(scene);
        stage.show();
    }
}