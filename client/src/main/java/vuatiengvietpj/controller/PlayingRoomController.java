package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.ChallengePack;
import vuatiengvietpj.model.Response;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class PlayingRoomController {

    @FXML
    private Label lblRoomId;
    
    @FXML
    private Label lblStatus;
    
    @FXML
    private Label lblLevel;
    
    @FXML
    private Label lblSubmitMessage;
    
    @FXML
    private Label lblMyScore;
    
    @FXML
    private Label lblMyRank;
    
    @FXML
    private Label lblStatusBar;
    
    @FXML
    private Label lblTimer;
    
    @FXML
    private AnchorPane overlayCountdown;
    
    @FXML
    private Label lblCountdown;
    
    @FXML
    private Button btnSubmit;
    
    @FXML
    private TextField txtAnswer;
    
    @FXML
    private HBox hboxQuizz;
    
    @FXML
    private ListView<String> listGuessedWords;
    
    @FXML
    private TableView<Player> tblScoreboard;
    
    @FXML
    private TableColumn<Player, Integer> colRank;
    
    @FXML
    private TableColumn<Player, String> colPlayerName;
    
    @FXML
    private TableColumn<Player, Integer> colScore;
    
    @FXML
    private Button btnLeaveRoom;

    // State
    private Room currentRoom;
    private Long currentUserId;
    private Stage primaryStage;
    
    // Game controller connection
    private GameController gameController;
    
    // Polling executor
    private java.util.concurrent.ScheduledExecutorService poller;
    
    // Timer executor
    private java.util.concurrent.ScheduledExecutorService timerExecutor;
    
    // Observable list for scoreboard
    private ObservableList<Player> scoreboardData = FXCollections.observableArrayList();
    
    // Timer state
    private int remainingSeconds = 60;
    private boolean gameStarted = false;
    private boolean showCountdownOnLoad = false;
    
    public void setCurrentUserId(Long id) {
        this.currentUserId = id;
        System.out.println("PlayingRoomController.setCurrentUserId: " + id);
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setRoom(Room room) {
        this.currentRoom = room;
        if (room != null) {
            System.out.println("PlayingRoomController.setRoom: roomId=" + room.getId() + 
                             ", status=" + room.getStatus() + 
                             ", currentUserId=" + currentUserId);
            
            // Khởi tạo GameController một lần duy nhất
            initializeGameController();
            
            updateRoomInfo();
            updateChallengePack();
            initializeScoreboard();
            
            // Bắt đầu polling để refresh room data (bao gồm scoreboard)
            // Không dùng broadcast listener vì ObjectInputStream không thread-safe
            startPolling();
            
            // Nếu là lần đầu load (từ PendingRoom sau khi start game), hiển thị countdown
            if (showCountdownOnLoad) {
                startCountdown();
            } else {
                // Nếu đã vào sau khi countdown (hoặc vào giữa game), bắt đầu timer luôn
                startGameTimer();
            }
            
            // Stop polling và cleanup khi đóng cửa sổ
            try {
                javafx.stage.Window w = lblRoomId.getScene().getWindow();
                if (w instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) w).setOnHidden(evt -> {
                        stopPolling();
                        stopGameTimer();
                        cleanupGameController();
                    });
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Khởi tạo GameController một lần duy nhất
     */
    private void initializeGameController() {
        if (gameController != null) {
            try {
                // Đóng instance cũ trước
                gameController.close();
            } catch (Exception ignored) {}
        }
        
        try {
            gameController = new GameController("localhost", 2208);
            System.out.println("PlayingRoomController: Đã khởi tạo GameController");
        } catch (Exception e) {
            System.err.println("PlayingRoomController.initializeGameController - Lỗi: " + e.getMessage());
            e.printStackTrace();
            gameController = null;
        }
    }
    
    /**
     * Đánh dấu là sẽ hiển thị countdown khi load (gọi trước setRoom)
     */
    public void setShowCountdownOnLoad(boolean show) {
        this.showCountdownOnLoad = show;
    }

    private void updateRoomInfo() {
        if (currentRoom == null) return;
        
        lblRoomId.setText(String.valueOf(currentRoom.getId()));
        lblStatus.setText("Đang chơi");
        
        // Update level if challenge pack exists
        if (currentRoom.getCp() != null) {
            lblLevel.setText(String.valueOf(currentRoom.getCp().getLevel()));
        }
        
        // Initialize timer display
        lblTimer.setText("60s");
    }

    private void updateChallengePack() {
        if (currentRoom == null || currentRoom.getCp() == null) {
            return;
        }
        
        ChallengePack cp = currentRoom.getCp();
        char[] quizz = cp.getQuizz();
        
        // Clear existing children
        hboxQuizz.getChildren().clear();
        
        // Display each character
        for (char c : quizz) {
            Text charText = new Text(String.valueOf(c));
            charText.setFont(new Font("Arial Bold", 32));
            charText.setStyle("-fx-fill: #2c3e50; -fx-padding: 5;");
            hboxQuizz.getChildren().add(charText);
        }
        
        System.out.println("PlayingRoomController.updateChallengePack: Displayed " + quizz.length + " characters");
    }

    private void initializeScoreboard() {
        // Setup table columns
        colRank.setCellValueFactory(cellData -> {
            int rank = scoreboardData.indexOf(cellData.getValue()) + 1;
            return new ReadOnlyObjectWrapper<>(rank);
        });
        
        colPlayerName.setCellValueFactory(cellData -> {
            Player p = cellData.getValue();
            String name = p.getName() != null ? p.getName() : "User " + p.getUserId();
            return new ReadOnlyObjectWrapper<>(name);
        });
        
        colScore.setCellValueFactory(cellData -> 
            new ReadOnlyObjectWrapper<>(cellData.getValue().getScore()));
        
        tblScoreboard.setItems(scoreboardData);
        
        // Update scoreboard từ room data
        updateScoreboardFromRoom(currentRoom);
    }

    private void updateScoreboardFromRoom(Room room) {
        if (room == null || room.getPlayers() == null) return;
        
        List<Player> players = new ArrayList<>(room.getPlayers());
        // Sort by score descending
        players.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        
        Platform.runLater(() -> {
            scoreboardData.setAll(players);
            
            // Update my score and rank
            if (currentUserId != null) {
                int myRank = -1;
                int myScore = 0;
                
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).getUserId().equals(currentUserId)) {
                        myRank = i + 1;
                        myScore = players.get(i).getScore();
                        break;
                    }
                }
                
                lblMyScore.setText("Điểm của bạn: " + myScore);
                if (myRank > 0) {
                    lblMyRank.setText("Hạng: " + myRank);
                } else {
                    lblMyRank.setText("Hạng: -");
                }
            }
        });
    }

    private void updateGuessedWords() {
        if (currentUserId == null) return;
        
        List<String> words = new ArrayList<>(GameController.getUserGuessedWords(currentUserId));
        Platform.runLater(() -> {
            ObservableList<String> wordList = FXCollections.observableArrayList(words);
            listGuessedWords.setItems(wordList);
        });
    }

    @FXML
    public void OnSubmitAnswer(ActionEvent event) {
        // Không cho submit nếu game chưa bắt đầu hoặc timer đã hết
        if (!gameStarted) {
            showMessage("Game chưa bắt đầu!", true);
            return;
        }
        
        if (remainingSeconds <= 0) {
            showMessage("Hết thời gian!", true);
            return;
        }
        
        String answer = txtAnswer.getText().trim();
        
        if (answer.isEmpty()) {
            showMessage("Vui lòng nhập đáp án", true);
            return;
        }
        
        if (currentRoom == null || currentUserId == null) {
            showMessage("Không có thông tin phòng hoặc người dùng", true);
            return;
        }
        
        // Disable submit button while processing
        btnSubmit.setDisable(true);
        lblStatusBar.setText("Đang xử lý...");
        
        // Submit answer in background thread
        new Thread(() -> {
            if (gameController == null) {
                Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    showMessage("Lỗi: GameController chưa được khởi tạo", true);
                    lblStatusBar.setText("Lỗi kết nối");
                });
                return;
            }
            
            try {
                GameController.SubmitResult result = gameController.submitAnswerWithResult(
                    currentRoom.getId(), 
                    currentUserId, 
                    answer
                );
                
                Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    txtAnswer.clear();
                    
                    if (result.isSuccess()) {
                        showMessage("✓ Đúng! +" + result.getPoints() + " điểm (Từ: " + result.getMatchedAnswer() + ")", false);
                        lblStatusBar.setText("Đáp án đúng! Đã nhận " + result.getPoints() + " điểm");
                        
                        // Update guessed words list
                        updateGuessedWords();
                        
                        // Scoreboard will be updated via broadcast
                    } else {
                        showMessage(result.getErrorMessage() != null ? result.getErrorMessage() : "Đáp án sai", true);
                        lblStatusBar.setText("Đáp án không đúng");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    showMessage("Lỗi khi submit: " + e.getMessage(), true);
                    lblStatusBar.setText("Lỗi kết nối");
                    e.printStackTrace();
                });
            }
        }).start();
    }


    // NOTE: Broadcast listener đã bị tắt vì ObjectInputStream không thread-safe
    // Thay vào đó, sử dụng polling để refresh scoreboard thông qua refreshRoomData()
    // Nếu cần broadcast real-time, cần refactor để dùng separate connection hoặc message queue

    private void startPolling() {
        stopPolling();
        poller = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("PlayingRoom-poller-" + (currentRoom == null ? "unknown" : currentRoom.getId()));
            return t;
        });
        
        poller.scheduleAtFixedRate(() -> {
            refreshRoomData();
        }, 2, 2, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void refreshRoomData() {
        try {
            if (currentRoom == null) return;
            
            Room latest;
            try (RoomController rc = new RoomController("localhost", 2208)) {
                latest = rc.getRoomById(currentRoom.getId());
            } catch (Exception e) {
                System.err.println("PlayingRoomController.refreshRoomData - Lỗi kết nối: " + e.getMessage());
                return;
            }
            
            if (latest == null) {
                // Room không tồn tại - quay về list room
                Platform.runLater(() -> {
                    stopPolling();
                    navigateToRoomList();
                });
                return;
            }
            
            // Kiểm tra status thay đổi
            if (!"playing".equals(latest.getStatus())) {
                // Room không còn ở trạng thái playing - quay về pending room
                Platform.runLater(() -> {
                    stopPolling();
                    updateRoomInfo();
                    navigateToPendingRoom();
                });
                return;
            }
            
            // Kiểm tra user có còn trong room không
            if (currentUserId != null && latest.getPlayers() != null) {
                boolean stillInRoom = latest.getPlayers().stream()
                    .anyMatch(p -> p.getUserId().equals(currentUserId));
                
                if (!stillInRoom) {
                    Platform.runLater(() -> {
                        stopPolling();
                        showInfo("Rời phòng", "Bạn không còn trong phòng này");
                        navigateToRoomList();
                    });
                    return;
                }
            }
            
            // Update UI if room data changed
            boolean changed = false;
            if (latest.getCp() != null && currentRoom.getCp() != null) {
                if ( latest.getCp().getId() != currentRoom.getCp().getId()) {
                    changed = true;
                }
            } else if (latest.getCp() != null || currentRoom.getCp() != null) {
                changed = true;
            }
            
            if (changed) {
                Room finalLatest = latest;
                Platform.runLater(() -> {
                    this.currentRoom = finalLatest;
                    updateChallengePack();
                    updateScoreboardFromRoom(finalLatest);
                });
            } else {
                // Just update scoreboard
                updateScoreboardFromRoom(latest);
            }
            
        } catch (Exception e) {
            System.err.println("PlayingRoomController.refreshRoomData error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopPolling() {
        try {
            if (poller != null && !poller.isShutdown()) {
                poller.shutdownNow();
            }
        } catch (Exception ignored) {}
        poller = null;
        // Note: Don't close GameController here - it's still needed for game operations
    }
    
    /**
     * Cleanup: Đóng GameController và unsubscribe
     * Chỉ gọi khi thực sự cần cleanup (navigate away, close window, etc.)
     */
    private void cleanupGameController() {
        try {
            if (gameController != null) {
                // Stop broadcast listener if it exists (though it's not used)
                gameController.stopBroadcastListener();
                gameController.close();
            }
        } catch (Exception e) {
            System.err.println("PlayingRoomController.cleanupGameController error: " + e.getMessage());
        }
        gameController = null;
        System.out.println("PlayingRoomController: Đã đóng GameController");
    }
    
    /**
     * Hiển thị countdown 5 giây trước khi bắt đầu game
     */
    private void startCountdown() {
        overlayCountdown.setVisible(true);
        btnSubmit.setDisable(true);
        
        java.util.concurrent.ScheduledExecutorService countdownExecutor = 
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        
        final int[] countdown = {5};
        
        countdownExecutor.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (countdown[0] > 0) {
                    lblCountdown.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                } else {
                    // Kết thúc countdown
                    countdownExecutor.shutdown();
                    overlayCountdown.setVisible(false);
                    btnSubmit.setDisable(false);
                    gameStarted = true;
                    
                    // Bắt đầu timer 60 giây
                    startGameTimer();
                }
            });
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Bắt đầu timer 60 giây cho game
     */
    private void startGameTimer() {
        stopGameTimer(); // Dừng timer cũ nếu có
        
        remainingSeconds = 60;
        gameStarted = true;
        lblTimer.setText("60s");
        
        timerExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("PlayingRoom-timer-" + (currentRoom == null ? "unknown" : currentRoom.getId()));
            return t;
        });
        
        timerExecutor.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            
            Platform.runLater(() -> {
                if (remainingSeconds > 0) {
                    lblTimer.setText(remainingSeconds + "s");
                    // Đổi màu khi còn ít thời gian
                    if (remainingSeconds <= 10) {
                        lblTimer.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                } else {
                    // Timer hết - tự động kết thúc game
                    lblTimer.setText("0s");
                    autoEndGame();
                }
            });
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Dừng timer
     */
    private void stopGameTimer() {
        try {
            if (timerExecutor != null && !timerExecutor.isShutdown()) {
                timerExecutor.shutdownNow();
            }
        } catch (Exception ignored) {}
        timerExecutor = null;
    }
    
    /**
     * Tự động kết thúc game khi timer hết
     */
    private void autoEndGame() {
        stopGameTimer();
        btnSubmit.setDisable(true);
        lblStatusBar.setText("Hết thời gian! Game đang kết thúc...");
        
        // Xóa bộ từ đã đoán của user
        if (currentUserId != null) {
            GameController.clearUserSubmittedResults(currentUserId);
            Platform.runLater(() -> {
                updateGuessedWords();
            });
        }
        
        // Gọi API END game
        new Thread(() -> {
            if (gameController == null) {
                Platform.runLater(() -> {
                    showError("Kết thúc game", "Lỗi: GameController chưa được khởi tạo");
                    navigateToPendingRoom();
                });
                return;
            }
            
            try {
                Response response = gameController.endGame(currentRoom.getId().toString());
                
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        // Refresh scoreboard lần cuối trước khi hiển thị (gọi trong thread riêng)
                        new Thread(() -> {
                            try {
                                // Đợi một chút để server broadcast scoreboard cuối cùng và cập nhật database
                                Thread.sleep(800);
                                
                                // Lấy room data mới nhất từ server một cách đồng bộ
                                try (RoomController rc = new RoomController("localhost", 2208)) {
                                    Room latestRoom = rc.getRoomById(currentRoom.getId());
                                    if (latestRoom != null) {
                                        // Cập nhật scoreboard từ room data mới nhất
                                        Platform.runLater(() -> {
                                            updateScoreboardFromRoom(latestRoom);
                                            // Hiển thị bảng điểm số cuối cùng
                                            showFinalScoreboardDialog();
                                        });
                                        return;
                                    }
                                } catch (Exception ex) {
                                    System.err.println("Lỗi khi refresh room data: " + ex.getMessage());
                                }
                                
                                // Nếu không lấy được data mới, vẫn hiển thị dialog với data hiện tại
                                Platform.runLater(() -> showFinalScoreboardDialog());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                Platform.runLater(() -> showFinalScoreboardDialog());
                            }
                        }).start();
                    } else {
                        String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi";
                        showError("Kết thúc game", "Lỗi: " + errorMsg);
                        // Vẫn quay về PendingRoom dù có lỗi
                        navigateToPendingRoom();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Kết thúc game", "Lỗi: " + e.getMessage());
                    e.printStackTrace();
                    // Vẫn quay về PendingRoom dù có lỗi
                    navigateToPendingRoom();
                });
            }
        }).start();
    }

    private void navigateToPendingRoom() {
        try {
            stopPolling();
            stopGameTimer();
            cleanupGameController();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/PendingRoom.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Object controller = loader.getController();
            if (controller instanceof PendingRoomController) {
                PendingRoomController prc = (PendingRoomController) controller;
                prc.setCurrentUserId(currentUserId);
                prc.setRoom(currentRoom);
            }
            
            if (primaryStage != null) {
                primaryStage.setScene(scene);
                primaryStage.setTitle("Phòng chơi");
                primaryStage.show();
            }
        } catch (IOException e) {
            System.err.println("PlayingRoomController.navigateToPendingRoom - Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToRoomList() {
        try {
            stopPolling();
            stopGameTimer();
            cleanupGameController();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/ListRoom.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Object controller = loader.getController();
            if (controller instanceof ListRoomController) {
                ListRoomController lrc = (ListRoomController) controller;
                lrc.setCurrentUserId(currentUserId);
                if (primaryStage != null) {
                    lrc.setPrimaryStage(primaryStage);
                }
            }
            
            if (primaryStage != null) {
                primaryStage.setScene(scene);
                primaryStage.setTitle("Danh sách phòng");
                primaryStage.show();
            }
        } catch (IOException e) {
            System.err.println("PlayingRoomController.navigateToRoomList - Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMessage(String message, boolean isError) {
        lblSubmitMessage.setText(message);
        lblSubmitMessage.setVisible(true);
        lblSubmitMessage.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        
        // Auto hide after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> lblSubmitMessage.setVisible(false));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị dialog bảng điểm số cuối cùng khi game kết thúc
     */
    private void showFinalScoreboardDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Game đã kết thúc");
        dialog.setHeaderText("Bảng điểm số cuối cùng");
        
        // Tạo TableView để hiển thị bảng điểm
        TableView<Player> scoreTable = new TableView<>();
        scoreTable.setPrefWidth(400);
        scoreTable.setPrefHeight(300);
        
        // Cột Hạng
        TableColumn<Player, Integer> rankCol = new TableColumn<>("Hạng");
        rankCol.setPrefWidth(80);
        rankCol.setCellValueFactory(cellData -> {
            int index = scoreboardData.indexOf(cellData.getValue());
            return new ReadOnlyObjectWrapper<>(index + 1);
        });
        rankCol.setStyle("-fx-alignment: CENTER;");
        
        // Cột Tên người chơi
        TableColumn<Player, String> nameCol = new TableColumn<>("Người chơi");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(cellData -> {
            Player p = cellData.getValue();
            String name = (p.getName() != null && !p.getName().isEmpty()) 
                ? p.getName() 
                : "Người chơi #" + p.getUserId();
            return new ReadOnlyObjectWrapper<>(name);
        });
        
        // Cột Điểm
        TableColumn<Player, Integer> scoreCol = new TableColumn<>("Điểm");
        scoreCol.setPrefWidth(100);
        scoreCol.setCellValueFactory(cellData -> 
            new ReadOnlyObjectWrapper<>(cellData.getValue().getScore()));
        scoreCol.setStyle("-fx-alignment: CENTER;");
        
        scoreTable.getColumns().addAll(rankCol, nameCol, scoreCol);
        
        // Thêm dữ liệu vào table (đã được sắp xếp)
        ObservableList<Player> finalScores = FXCollections.observableArrayList(scoreboardData);
        scoreTable.setItems(finalScores);
        
        // Highlight người chơi hiện tại
        if (currentUserId != null) {
            for (Player p : finalScores) {
                if (p.getUserId().equals(currentUserId)) {
                    int index = finalScores.indexOf(p);
                    scoreTable.getSelectionModel().select(index);
                    scoreTable.scrollTo(index);
                    break;
                }
            }
        }
        
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));
        content.getChildren().add(scoreTable);
        
        // Thêm thông tin điểm của bạn
        if (currentUserId != null) {
            for (Player p : finalScores) {
                if (p.getUserId().equals(currentUserId)) {
                    int rank = finalScores.indexOf(p) + 1;
                    Label yourInfo = new Label("Điểm của bạn: " + p.getScore() + " (Hạng: " + rank + ")");
                    yourInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                    content.getChildren().add(yourInfo);
                    break;
                }
            }
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // Đóng dialog và navigate về PendingRoom
        dialog.setOnCloseRequest(e -> {
            navigateToPendingRoom();
        });
        
        // Xử lý khi click nút OK
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                navigateToPendingRoom();
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    @FXML
    public void OnClickLeaveRoom(ActionEvent event) {
        System.out.println("PlayingRoomController.OnClickLeaveRoom - Thoát phòng");
        
        if (currentRoom == null) {
            showError("Thoát phòng", "Không có thông tin phòng.");
            return;
        }
        
        if (currentUserId == null) {
            showError("Thoát phòng", "Bạn chưa đăng nhập.");
            return;
        }
        
        // Xác nhận trước khi thoát (nếu đang trong game)
        if (gameStarted) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận");
            confirmAlert.setHeaderText("Bạn đang trong game");
            confirmAlert.setContentText("Bạn có chắc chắn muốn thoát khỏi phòng? Điểm số của bạn sẽ không được lưu.");
            
            java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                return; // User cancelled
            }
        }
        
        new Thread(() -> {
            try {
                // Gửi request rời phòng
                try (RoomController rc = new RoomController("localhost", 2208)) {
                    Response response = rc.outRoom(currentRoom.getId(), currentUserId);
                    
                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            System.out.println("PlayingRoomController - Rời phòng thành công");
                            showInfo("Thoát phòng", "Bạn đã rời khỏi phòng #" + currentRoom.getId());
                        } else {
                            String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                            System.err.println("PlayingRoomController - Lỗi khi rời phòng: " + errorMsg);
                            showError("Thoát phòng", "Lỗi khi rời phòng: " + errorMsg);
                        }
                    });
                }
                
                // Dừng polling, timer và cleanup
                Platform.runLater(() -> {
                    stopPolling();
                    stopGameTimer();
                    cleanupGameController();
                    
                    // Clear user's submitted words
                    if (currentUserId != null) {
                        GameController.clearUserSubmittedResults(currentUserId);
                    }
                    
                    // Navigate về danh sách phòng
                    navigateToRoomList();
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("PlayingRoomController - Exception khi rời phòng: " + e.getMessage());
                    e.printStackTrace();
                    showError("Thoát phòng", "Không thể kết nối đến server: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void initialize() {
        // Initialize empty lists
        listGuessedWords.setItems(FXCollections.observableArrayList());
        
        // Setup text field to submit on Enter
        txtAnswer.setOnAction(this::OnSubmitAnswer);
        
        // Initial status
        lblStatusBar.setText("Đang chờ game bắt đầu...");
    }
}

