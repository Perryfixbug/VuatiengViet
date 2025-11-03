package vuatiengvietpj.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
import vuatiengvietpj.model.Request;
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
    private Integer currentUserId;
    private Stage primaryStage;
    
    // Game controller connection
    private GameController gameController;
    
    // LISTENER fields (thay thế polling)
    private volatile boolean listening = false;
    private Socket listenerSocket;
    private ObjectOutputStream listenerOut;
    private ObjectInputStream listenerIn;
    private Thread listenerThread;
    
    // Flag để tránh gọi END nhiều lần
    private volatile boolean gameEnding = false;
    
    // Gson with Instant serializer
    private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.Instant.class,
                    (com.google.gson.JsonDeserializer<java.time.Instant>) (json, typeOfT, context) -> 
                        java.time.Instant.parse(json.getAsString()))
            .create();
    
    // Timer executor
    private java.util.concurrent.ScheduledExecutorService timerExecutor;
    
    // Observable list for scoreboard
    private ObservableList<Player> scoreboardData = FXCollections.observableArrayList();
    
    // Timer state
    private Integer remainingSeconds = 60;
    private boolean gameStarted = false;
    private boolean showCountdownOnLoad = false;
    
    public void setCurrentUserId(Integer id) {
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
            
            // Bắt đầu listener để nhận realtime updates từ server
            startListening();
            
            // Nếu là lần đầu load (từ PendingRoom sau khi start game), hiển thị countdown
            if (showCountdownOnLoad) {
                startCountdown();
            } else {
                // Nếu đã vào sau khi countdown (hoặc vào giữa game), bắt đầu timer luôn
                startGameTimer();
            }
            
            // LƯU primaryStage nếu chưa có (lưu NGAY khi scene còn attach vào window)
            if (primaryStage == null) {
                try {
                    javafx.stage.Window w = lblRoomId.getScene().getWindow();
                    if (w instanceof javafx.stage.Stage) {
                        primaryStage = (javafx.stage.Stage) w;
                        System.out.println("[PlayingRoom] Auto-saved primaryStage from scene in setRoom()");
                    }
                } catch (Exception e) {
                    System.err.println("[PlayingRoom] Could not save primaryStage in setRoom(): " + e.getMessage());
                }
            }
            
            // Cleanup khi đóng cửa sổ
            try {
                javafx.stage.Window w = lblRoomId.getScene().getWindow();
                if (w instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) w).setOnHidden(evt -> {
                        stopGameTimer();
                        stopListening();
                        cleanupGameController();
                        // Auto-kick khi đóng cửa sổ (ấn X)
                        handleWindowClose();
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
            Integer rank = scoreboardData.indexOf(cellData.getValue()) + 1;
            return new ReadOnlyObjectWrapper<>(rank);
        });
        
        colPlayerName.setCellValueFactory(cellData -> {
            Player p = cellData.getValue();
            String name = p.getName() != null ? p.getName() : "User " + p.getUserId();
            return new ReadOnlyObjectWrapper<>(name);
        });
        
        colScore.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScore();
            return new ReadOnlyObjectWrapper<>(score != null ? score : 0);
        });
        
        tblScoreboard.setItems(scoreboardData);
        
        // Update scoreboard từ room data
        updateScoreboardFromRoom(currentRoom);
    }

    private void updateScoreboardFromRoom(Room room) {
        if (room == null || room.getPlayers() == null) return;
        
        List<Player> players = new ArrayList<>(room.getPlayers());
        // Sort by score descending (handle null scores - treat as 0)
        players.sort((p1, p2) -> {
            Integer score1 = (p1.getScore() != null) ? p1.getScore() : 0;
            Integer score2 = (p2.getScore() != null) ? p2.getScore() : 0;
            return Integer.compare(score2, score1);
        });
        
        Platform.runLater(() -> {
            scoreboardData.setAll(players);
            
            // Update my score and rank
            if (currentUserId != null) {
                Integer myRank = -1;
                Integer myScore = 0;
                
                for (Integer i = 0; i < players.size(); i++) {
                    if (players.get(i).getUserId().equals(currentUserId)) {
                        myRank = i + 1;
                        myScore = (players.get(i).getScore() != null) ? players.get(i).getScore() : 0;
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

    /**
     * Cập nhật điểm của player trực tiếp vào scoreboard (không cần refresh từ server)
     * Gọi sau khi submit đáp án đúng để update real-time
     */
    private void updatePlayerScoreDirectly(Integer userId, Integer addedPoints) {
        if (userId == null || scoreboardData.isEmpty()) return;
        
        Platform.runLater(() -> {
            // Tìm player trong scoreboard
            Player targetPlayer = null;
            for (Player p : scoreboardData) {
                if (p.getUserId().equals(userId)) {
                    targetPlayer = p;
                    break;
                }
            }
            
            if (targetPlayer != null) {
                // Cập nhật điểm (cộng thêm), xử lý null-safe
                Integer currentScore = (targetPlayer.getScore() != null) ? targetPlayer.getScore() : 0;
                Integer newScore = currentScore + addedPoints;
                targetPlayer.setScore(newScore);
                
                // Sort lại danh sách với null-safe comparison
                List<Player> sortedPlayers = new ArrayList<>(scoreboardData);
                sortedPlayers.sort((p1, p2) -> {
                    Integer score1 = (p1.getScore() != null) ? p1.getScore() : 0;
                    Integer score2 = (p2.getScore() != null) ? p2.getScore() : 0;
                    return Integer.compare(score2, score1);
                });
                scoreboardData.setAll(sortedPlayers);
                
                // Update my score and rank
                Integer myRank = -1;
                Integer myScore = 0;
                
                for (Integer i = 0; i < sortedPlayers.size(); i++) {
                    if (sortedPlayers.get(i).getUserId().equals(currentUserId)) {
                        myRank = i + 1;
                        myScore = (sortedPlayers.get(i).getScore() != null) ? sortedPlayers.get(i).getScore() : 0;
                        break;
                    }
                }
                
                lblMyScore.setText("Điểm của bạn: " + myScore);
                if (myRank > 0) {
                    lblMyRank.setText("Hạng: " + myRank);
                } else {
                    lblMyRank.setText("Hạng: -");
                }
                
                System.out.println("[PlayingRoom] Updated score directly: userId=" + userId + 
                                 ", addedPoints=" + addedPoints + ", newScore=" + newScore);
            }
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
                        
                        // CẬP NHẬT ĐIỂM TRỰC TIẾP vào scoreboard (không đợi polling)
                        updatePlayerScoreDirectly(currentUserId, result.getPoints());
                    } else {
                        showMessage(result.getErrorMessage() != null ? result.getErrorMessage() : "Đáp án sai", true);
                        // Không hiển thị message lỗi trong status bar
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

    // ========== LISTENER METHODS (thay thế polling) ==========
    
    private void startListening() {
        stopListening();
        
        // SUBSCRIBE vào RoomManager để nhận broadcast
        if (gameController != null && currentRoom != null) {
            try {
                Response subResponse = gameController.subscribe(currentRoom.getId());
                if (subResponse != null && subResponse.isSuccess()) {
                    System.out.println("[PlayingRoom] Subscribed to room " + currentRoom.getId());
                } else {
                    System.err.println("[PlayingRoom] Failed to subscribe: " + 
                        (subResponse != null ? subResponse.getData() : "null response"));
                }
            } catch (Exception e) {
                System.err.println("[PlayingRoom] Subscribe error: " + e.getMessage());
            }
        }
        
        listening = true;
        
        listenerThread = new Thread(() -> {
            try {
                System.out.println("[PlayingRoom] Starting listener for room " + currentRoom.getId());
                
                listenerSocket = new Socket("localhost", 2208);
                listenerOut = new ObjectOutputStream(listenerSocket.getOutputStream());
                listenerIn = new ObjectInputStream(listenerSocket.getInputStream());
                
                Request req = new Request("ROOM", "LISTEN", 
                    currentRoom.getId() + "," + currentUserId);
                listenerOut.writeObject(req);
                listenerOut.flush();
                
                System.out.println("[PlayingRoom] Listener started for room " + currentRoom.getId());
                
                while (listening && !listenerSocket.isClosed()) {
                    try {
                        Response response = (Response) listenerIn.readObject();
                        handleServerUpdate(response);
                    } catch (Exception e) {
                        if (listening) {
                            System.err.println("[PlayingRoom] Listener read error: " + e.getMessage());
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[PlayingRoom] Failed to start listener: " + e.getMessage());
            } finally {
                System.out.println("[PlayingRoom] Listener thread ending");
            }
        }, "PlayingRoomListener-" + currentRoom.getId());
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    private void handleServerUpdate(Response response) {
        System.out.println("[PlayingRoom] ========== Received UPDATE ==========");
        System.out.println("[PlayingRoom] Module: " + response.getmodule());
        System.out.println("[PlayingRoom] MaLenh: " + response.getMaLenh());
        System.out.println("[PlayingRoom] IsSuccess: " + response.isSuccess());
        System.out.println("[PlayingRoom] Data preview: " + (response.getData() != null ? response.getData().substring(0, Math.min(100, response.getData().length())) : "null"));
        
        // XỬ LÝ SCOREBOARD BROADCAST (từ GAME module)
        if ("GAME".equals(response.getmodule()) && "SCOREBOARD".equals(response.getMaLenh())) {
            System.out.println("[PlayingRoom] Received SCOREBOARD broadcast");
            try {
                vuatiengvietpj.model.ScoreBoard scoreBoard = gson.fromJson(response.getData(), vuatiengvietpj.model.ScoreBoard.class);
                if (scoreBoard != null && scoreBoard.getPlayer() != null) {
                    Platform.runLater(() -> {
                        // Cập nhật scoreboard từ broadcast
                        scoreboardData.setAll(scoreBoard.getPlayer());
                        // Tìm và update my score & rank
                        for (int i = 0; i < scoreboardData.size(); i++) {
                            Player p = scoreboardData.get(i);
                            if (p.getUserId().equals(currentUserId)) {
                                lblMyScore.setText("Điểm: " + (p.getScore() != null ? p.getScore() : 0));
                                lblMyRank.setText("Hạng: " + (i + 1));
                                break;
                            }
                        }
                        System.out.println("[PlayingRoom] Scoreboard updated from broadcast, total players: " + scoreBoard.getPlayer().size());
                    });
                }
            } catch (Exception e) {
                System.err.println("[PlayingRoom] Error parsing SCOREBOARD: " + e.getMessage());
                e.printStackTrace();
            }
            return; // Không xử lý thêm
        }
        
        // XỬ LÝ ROOM UPDATE (từ ROOM module)
        if ("UPDATE".equals(response.getMaLenh())) {
            try {
                Room updatedRoom = gson.fromJson(response.getData(), Room.class);
                
                System.out.println("[PlayingRoom] Parsed updatedRoom: id=" + updatedRoom.getId() + 
                                 ", status=" + updatedRoom.getStatus() + 
                                 ", players=" + (updatedRoom.getPlayers() != null ? updatedRoom.getPlayers().size() : 0));
                
                // Kiểm tra xem có phải phòng mới (game đã kết thúc) không
                // QUAN TRỌNG: Phải check TRƯỚC KHI updateRoomData()
                // 
                // CÓ 2 TRƯỜNG HỢP:
                // 1. Status "playing" → "pending" với CÙNG ID (không xảy ra với logic hiện tại)
                // 2. Room ID KHÁC và status = "pending" (server tạo phòng mới sau END)
                
                // Debug từng điều kiện riêng lẻ
                boolean cond1 = (currentRoom != null);
                boolean cond2 = cond1 && "playing".equalsIgnoreCase(currentRoom.getStatus());
                boolean cond3 = (updatedRoom != null);
                boolean cond4 = cond3 && "pending".equalsIgnoreCase(updatedRoom.getStatus());
                boolean cond5 = cond1 && cond3 && !currentRoom.getId().equals(updatedRoom.getId());
                
                System.out.println("[DEBUG] Condition breakdown:");
                System.out.println("  cond1 (currentRoom != null): " + cond1);
                System.out.println("  cond2 (currentRoom.status = 'playing'): " + cond2);
                System.out.println("  cond3 (updatedRoom != null): " + cond3);
                System.out.println("  cond4 (updatedRoom.status = 'pending'): " + cond4);
                System.out.println("  cond5 (IDs different): " + cond5);
                if (cond1 && cond3) {
                    System.out.println("  currentRoom.getId(): " + currentRoom.getId() + " (type: " + currentRoom.getId().getClass().getSimpleName() + ")");
                    System.out.println("  updatedRoom.getId(): " + updatedRoom.getId() + " (type: " + updatedRoom.getId().getClass().getSimpleName() + ")");
                    System.out.println("  IDs equal? " + currentRoom.getId().equals(updatedRoom.getId()));
                }
                
                final boolean gameJustEnded = cond1 && cond2 && cond3 && cond4 && cond5;
                
                System.out.println("[PlayingRoom] currentRoom: " + (currentRoom != null ? "id=" + currentRoom.getId() + ", status=" + currentRoom.getStatus() : "null"));
                System.out.println("[PlayingRoom] gameJustEnded check: currentRoomId=" + 
                                 (currentRoom != null ? currentRoom.getId() : "null") +
                                 ", currentStatus=" + (currentRoom != null ? currentRoom.getStatus() : "null") + 
                                 ", newRoomId=" + (updatedRoom != null ? updatedRoom.getId() : "null") +
                                 ", newStatus=" + (updatedRoom != null ? updatedRoom.getStatus() : "null") +
                                 ", gameJustEnded=" + gameJustEnded);
                
                Platform.runLater(() -> {
                    // Cập nhật room data TRƯỚC
                    updateRoomData(updatedRoom);
                    
                    // Nếu game vừa kết thúc (chuyển từ playing → pending)
                    // CHỈ hiển thị dialog cho NON-OWNER vì owner đã nhận kết quả từ END request
                    if (gameJustEnded && !isRoomOwner()) {
                        System.out.println("[PlayingRoom] *** GAME JUST ENDED (non-owner) - Showing final scoreboard dialog ***");
                        // Hiển thị dialog kết quả cho người chơi thường
                        stopListening();
                        showFinalScoreboardDialog();
                        // navigateToPendingRoom() sẽ được gọi trong dialog
                    } else if (gameJustEnded && isRoomOwner()) {
                        System.out.println("[PlayingRoom] Game ended but owner already handled in OnClickEndGame");
                    } else {
                        System.out.println("[PlayingRoom] Game still playing, just updating scoreboard");
                    }
                });
                
            } catch (Exception e) {
                System.err.println("[PlayingRoom] Error parsing update: " + e.getMessage());
                e.printStackTrace();
            }
        } else if ("KICKED".equals(response.getMaLenh())) {
            Platform.runLater(() -> {
                stopListening();
                showInfo("Bị Kick", "Bạn đã bị kick khỏi phòng");
                navigateToRoomList();
            });
        }
        System.out.println("[PlayingRoom] ========== End UPDATE handling ==========");
    }
    
    private void stopListening() {
        listening = false;
        try {
            if (listenerIn != null) listenerIn.close();
            if (listenerOut != null) listenerOut.close();
            if (listenerSocket != null) listenerSocket.close();
            if (listenerThread != null) listenerThread.interrupt();
        } catch (Exception e) {
            System.err.println("[PlayingRoom] Error stopping listener: " + e.getMessage());
        }
        System.out.println("[PlayingRoom] Listener stopped");
    }
    
    private void updateRoomData(Room room) {
        this.currentRoom = room;
        System.out.println("[PlayingRoom] Room updated: " + room.getId() + 
                         ", players=" + (room.getPlayers() != null ? room.getPlayers().size() : 0));
        
        // Cập nhật scoreboard với sort
        updateScoreboardFromRoom(room);
    }
    
    // ========== END LISTENER METHODS ==========

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
     * Xử lý khi đóng cửa sổ (ấn X) - Auto OUT khỏi phòng
     */
    private void handleWindowClose() {
        if (currentRoom == null || currentUserId == null) {
            return;
        }
        
        try (vuatiengvietpj.controller.RoomController rc = new vuatiengvietpj.controller.RoomController("localhost", 2208)) {
            Response response = rc.outRoom(currentRoom.getId(), currentUserId);
            if (response != null && response.isSuccess()) {
                System.out.println("PlayingRoomController: Auto OUT room khi đóng cửa sổ - roomId=" + currentRoom.getId());
            }
        } catch (Exception e) {
            System.err.println("PlayingRoomController.handleWindowClose error: " + e.getMessage());
        }
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
        // PREVENT duplicate END calls (race condition từ nhiều listeners)
        if (gameEnding) {
            System.out.println("[PlayingRoom] Game already ending, skip duplicate call");
            return;
        }
        gameEnding = true;
        
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
        
        // CHỈ CHỦ PHÒNG mới gọi END API, các thành viên khác chờ nhận UPDATE từ server
        boolean isOwner = (currentRoom != null && currentUserId != null && 
                          currentUserId.equals(currentRoom.getOwnerId()));
        
        if (!isOwner) {
            System.out.println("[PlayingRoom] Not owner, waiting for server update...");
            lblStatusBar.setText("Đang chờ kết quả từ chủ phòng...");
            // Không làm gì, chờ handleServerUpdate nhận UPDATE với phòng mới
            return;
        }
        
        System.out.println("[PlayingRoom] Owner ending game and creating new room...");
        
        // Gọi API END game (CHỈ CHỦ PHÒNG)
        new Thread(() -> {
            if (gameController == null) {
                Platform.runLater(() -> {
                    showError("Kết thúc game", "Lỗi: GameController chưa được khởi tạo");
                    navigateToPendingRoom();
                });
                gameEnding = false; // Reset flag
                return;
            }
            
            try {
                // Gọi END để server tạo phòng mới và trả về roomId mới
                System.out.println("[PlayingRoom] Calling END for room: " + currentRoom.getId());
                Response response = gameController.endGame(currentRoom.getId().toString());
                
                if (response == null || !response.isSuccess()) {
                    System.err.println("[PlayingRoom] END failed: " + (response != null ? response.getData() : "null response"));
                    Platform.runLater(() -> {
                        showError("Kết thúc game", "Không thể kết thúc game: " + 
                                (response != null ? response.getData() : "Không nhận được phản hồi"));
                    });
                    gameEnding = false; // Reset flag
                    return;
                }
                
                // Parse NEW room ID từ response
                String newRoomIdStr = response.getData();
                System.out.println("[PlayingRoom] END response data: " + newRoomIdStr);
                if (newRoomIdStr != null && !newRoomIdStr.isEmpty()) {
                    try {
                        Integer newRoomId = Integer.parseInt(newRoomIdStr);
                        System.out.println("[PlayingRoom] Game ended, new room created: " + newRoomId);
                        
                        // FETCH phòng mới từ server để có đầy đủ data
                        try (vuatiengvietpj.controller.RoomController rc = 
                             new vuatiengvietpj.controller.RoomController("localhost", 2208)) {
                            Room newRoom = rc.getRoomById(newRoomId);
                            if (newRoom != null) {
                                currentRoom = newRoom; // Replace toàn bộ room object
                                System.out.println("[PlayingRoom] Fetched new room data: " + 
                                                 newRoom.getId() + ", players=" + 
                                                 (newRoom.getPlayers() != null ? newRoom.getPlayers().size() : 0));
                                
                                Platform.runLater(() -> {
                                    // HIỂN THỊ dialog scoreboard TRƯỚC KHI navigate
                                    updateScoreboardFromRoom(currentRoom);
                                    stopListening();
                                    showFinalScoreboardDialog();
                                    // navigateToPendingRoom() sẽ được gọi TRONG showFinalScoreboardDialog()
                                });
                            } else {
                                System.err.println("[PlayingRoom] Failed to fetch new room from server");
                                Platform.runLater(() -> {
                                    showError("Lỗi", "Không thể tải thông tin phòng mới");
                                });
                                gameEnding = false;
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[PlayingRoom] Invalid new room ID: " + newRoomIdStr);
                        Platform.runLater(() -> {
                            showError("Lỗi", "ID phòng mới không hợp lệ");
                        });
                        gameEnding = false; // Reset flag
                    } catch (Exception e) {
                        System.err.println("[PlayingRoom] Error fetching new room: " + e.getMessage());
                        Platform.runLater(() -> {
                            showError("Lỗi", "Không thể tải thông tin phòng mới: " + e.getMessage());
                        });
                        gameEnding = false;
                    }
                } else {
                    System.err.println("[PlayingRoom] No new room ID in response");
                    Platform.runLater(() -> {
                        showError("Lỗi", "Không nhận được ID phòng mới");
                    });
                    gameEnding = false; // Reset flag
                }
                
            } catch (Exception ex) {
                System.err.println("Lỗi khi gọi endGame: " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError("Lỗi", "Không thể kết thúc game: " + ex.getMessage());
                });
                gameEnding = false; // Reset flag
            }
        }).start();
    }

    private void navigateToPendingRoom() {
        System.out.println("[PlayingRoom] navigateToPendingRoom called");
        
        if (primaryStage == null) {
            System.err.println("[PlayingRoom] ERROR: primaryStage is NULL - this should never happen!");
            showError("Lỗi", "Không thể quay về phòng chờ. Vui lòng đóng cửa sổ và vào lại.");
            return;
        }
        
        try {
            stopListening();
            stopGameTimer();
            cleanupGameController();
            
            // Tạo PendingRoom mới với NEW room ID
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/PendingRoom.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Object controller = loader.getController();
            if (controller instanceof PendingRoomController) {
                PendingRoomController prc = (PendingRoomController) controller;
                prc.setCurrentUserId(currentUserId);
                prc.setPrimaryStage(primaryStage);
                prc.setRoom(currentRoom);
            }
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("Phòng chơi");
            primaryStage.show();
            System.out.println("[PlayingRoom] Successfully navigated to PendingRoom");
            
        } catch (IOException e) {
            System.err.println("PlayingRoomController.navigateToPendingRoom - IOException: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi", "Không thể tải giao diện phòng chờ: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("PlayingRoomController.navigateToPendingRoom - Exception: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi", "Không thể quay về phòng chờ: " + e.getMessage());
        }
    }

    private void navigateToRoomList() {
        try {
            stopListening();
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

    // Helper method to check if current user is room owner
    private boolean isRoomOwner() {
        return currentRoom != null && currentUserId != null && 
               currentUserId.equals(currentRoom.getOwnerId());
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
        // LƯU STAGE TRƯỚC KHI hiển thị dialog (vì dialog có thể làm scene detach)
        Stage savedStage = primaryStage;
        if (savedStage == null) {
            try {
                if (lblRoomId != null && lblRoomId.getScene() != null) {
                    javafx.stage.Window window = lblRoomId.getScene().getWindow();
                    if (window instanceof Stage) {
                        savedStage = (Stage) window;
                        System.out.println("[PlayingRoom] Saved stage before showing dialog");
                    }
                }
            } catch (Exception e) {
                System.err.println("[PlayingRoom] Error saving stage: " + e.getMessage());
            }
        }
        
        // Lưu vào primaryStage nếu chưa có
        if (primaryStage == null && savedStage != null) {
            primaryStage = savedStage;
            System.out.println("[PlayingRoom] Set primaryStage from saved stage");
        }
        
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
            Integer index = scoreboardData.indexOf(cellData.getValue());
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
        scoreCol.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScore();
            return new ReadOnlyObjectWrapper<>(score != null ? score : 0);
        });
        scoreCol.setStyle("-fx-alignment: CENTER;");
        
        scoreTable.getColumns().addAll(rankCol, nameCol, scoreCol);
        
        // Thêm dữ liệu vào table (đã được sắp xếp)
        ObservableList<Player> finalScores = FXCollections.observableArrayList(scoreboardData);
        scoreTable.setItems(finalScores);
        
        // Highlight người chơi hiện tại
        if (currentUserId != null) {
            for (Player p : finalScores) {
                if (p.getUserId().equals(currentUserId)) {
                    Integer index = finalScores.indexOf(p);
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
                    Integer rank = finalScores.indexOf(p) + 1;
                    Integer score = (p.getScore() != null) ? p.getScore() : 0;
                    Label yourInfo = new Label("Điểm của bạn: " + score + " (Hạng: " + rank + ")");
                    yourInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                    content.getChildren().add(yourInfo);
                    break;
                }
            }
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // SET OWNER cho dialog = stage hiện tại (để có thể lấy lại sau)
        try {
            if (lblRoomId != null && lblRoomId.getScene() != null && lblRoomId.getScene().getWindow() != null) {
                dialog.initOwner(lblRoomId.getScene().getWindow());
                System.out.println("[PlayingRoom] Set dialog owner to current window");
            }
        } catch (Exception e) {
            System.err.println("[PlayingRoom] Could not set dialog owner: " + e.getMessage());
        }
        
        // Hiển thị dialog và chờ đóng
        dialog.showAndWait();
        
        // Navigate về PendingRoom SAU KHI dialog đóng
        System.out.println("[PlayingRoom] Dialog closed, navigating to PendingRoom...");
        navigateToPendingRoom();
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
                
                // Dừng listener, timer và cleanup
                Platform.runLater(() -> {
                    stopListening();
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

