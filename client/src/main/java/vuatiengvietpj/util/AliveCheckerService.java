package vuatiengvietpj.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.model.Response;

public class AliveCheckerService {
    private static AliveCheckerService instance;
    private java.util.concurrent.ScheduledExecutorService executor;
    private UserController userController;
    private long userId;
    private String sessionId;
    private Stage primaryStage;

    private AliveCheckerService() {
    }

    public static AliveCheckerService getInstance() {
        if (instance == null) {
            instance = new AliveCheckerService();
        }
        return instance;
    }

    public void start(UserController userController, long userId, String sessionId, Stage primaryStage) {
        stop(); // Stop old
        this.userController = userController;
        this.userId = userId;
        this.sessionId = sessionId;
        this.primaryStage = primaryStage;

        executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Global-alive-checker");
            return t;
        });
        executor.scheduleAtFixedRate(this::checkAlive, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkAlive() {
        try {
            Response resp = userController.sendAlive(userId, sessionId);
            if (resp == null || !resp.isSuccess()
                    || (resp.getData() != null && resp.getData().contains("het han"))) {
                // Session expired, load Auth
                Platform.runLater(() -> {
                    stop();
                    loadAuthScene();
                });
            }
        } catch (Exception e) {
            System.err.println("Alive check error: " + e.getMessage());
        }
    }

    private void loadAuthScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/Auth.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Auth");
            Object controller = loader.getController();
            if (controller instanceof UserController) {
                ((UserController) controller).setPrimaryStage(primaryStage);
                ((UserController) controller)
                        .setStatusLabel("Tài khoản đã đăng nhập ở nơi khác hoặc hết phiên đăng nhập");
            }
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load Auth: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        } catch (Exception ignored) {
        }
        executor = null;
    }
}