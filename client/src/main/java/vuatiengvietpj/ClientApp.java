package vuatiengvietpj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vuatiengvietpj.controller.UserController;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            System.out.println("Loading Auth.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/Auth.fxml"));
            Parent root = loader.load();
            System.out.println("Root loaded: " + (root != null));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Auth");

            // Set primaryStage cho UserController
            Object controller = loader.getController();
            if (controller instanceof UserController) {
                ((UserController) controller).setPrimaryStage(stage);
            }

            stage.show();
            System.out.println("Stage shown");
        } catch (Exception e) {
            System.err.println("Error loading Auth.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}