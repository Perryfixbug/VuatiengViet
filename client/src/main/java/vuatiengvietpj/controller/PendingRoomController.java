package vuatiengvietpj.controller;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

import com.google.gson.Gson;

import javafx.collections.FXCollections;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Player;

public class PendingRoomController {

    @FXML
    public Button btnOutRoom;

    @FXML
    public Button btnStart;

    @FXML
    public ChoiceBox<Integer> cbxNumberPlayer;

    @FXML
    public Label lblCountPlayer;

    @FXML
    public Label lblRoomId;

    @FXML
    public TableView<Player> tblPlayerList;
    
    // state
    private Room currentRoom;
    private Long currentUserId;
    private javafx.stage.Stage primaryStage;
    
    // suppress selection events when we programmatically set ChoiceBox value
    private boolean suppressSelectionEvents = false;
    
    // Flag để tránh navigate 2 lần khi start game
    private volatile boolean isNavigatingToGame = false;
    
    // LISTENER fields (thay thế polling)
    private Thread listenerThread;
    private Socket listenerSocket;
    private ObjectInputStream listenerIn;
    private ObjectOutputStream listenerOut;
    private volatile boolean listening = false;
    
    private Gson gson = new com.google.gson.GsonBuilder()
        .registerTypeAdapter(java.time.Instant.class, 
            (com.google.gson.JsonDeserializer<java.time.Instant>) (json, type, ctx) -> 
                java.time.Instant.parse(json.getAsString()))
        .create();
    
    // optional callback to notify parent/list controller to refresh room list
    private Runnable onRoomUpdated;
    
    // flag để phân biệt tự out hay bị kick
    private boolean isManualExit = false;

    public void setCurrentUserId(Long id) {
        this.currentUserId = id;
        System.out.println("PendingRoomController.setCurrentUserId: " + id);
    }
    
    public void setPrimaryStage(javafx.stage.Stage stage) {
        this.primaryStage = stage;
    }

    public void setOnRoomUpdated(Runnable cb) {
        this.onRoomUpdated = cb;
    }

    public void setRoom(Room room) {
        this.currentRoom = room;
        if (room != null) {
            System.out.println("PendingRoomController.setRoom: roomId=" + room.getId() + 
                             ", ownerId=" + room.getOwnerId() + 
                             ", currentUserId=" + currentUserId + 
                             ", max=" + room.getMaxPlayer());
            
            // Cập nhật thông tin phòng
            lblRoomId.setText(String.valueOf(room.getId()));
            updatePlayerCountLabel();
            
            // Cập nhật danh sách người chơi
            updatePlayerList();
            
            // Kiểm tra quyền (chỉ chủ phòng mới được thay đổi cài đặt)
            updateOwnerPermissions();
            
            // Tránh trigger selection listener khi set giá trị
            suppressSelectionEvents = true;
            cbxNumberPlayer.setValue(room.getMaxPlayer());
            suppressSelectionEvents = false;
            
            // ADD LISTENER CHỈ MỘT LẦN - sau khi đã set room và currentUserId
            addChoiceBoxListenerOnce();
            
            // THAY POLLING bằng LISTENING
            startListening();
        }
    }

    /**
     * Add listener cho ChoiceBox chỉ một lần duy nhất
     */
    private boolean listenerAdded = false;
    
    private void addChoiceBoxListenerOnce() {
        if (listenerAdded) return; // Đã add rồi thì không add nữa
        
        try {
            cbxNumberPlayer.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("PendingRoomController.choice selection changed: from=" + oldVal + " to=" + newVal);
                if (suppressSelectionEvents) {
                    System.out.println("PendingRoomController: selection change suppressed (programmatic).");
                    return;
                }
                if (newVal == null || currentRoom == null || currentUserId == null) {
                    System.out.println("PendingRoomController: skipping edit - null values");
                    return;
                }
                if (oldVal != null && newVal.equals(oldVal)) return;
                
                // Kiểm tra quyền TRƯỚC KHI edit
                if (!currentUserId.equals(currentRoom.getOwnerId())) {
                    showError("Cập nhật phòng", "Chỉ chủ phòng mới được thay đổi số người tối đa!");
                    // Revert về giá trị cũ
                    suppressSelectionEvents = true;
                    cbxNumberPlayer.setValue(currentRoom.getMaxPlayer());
                    suppressSelectionEvents = false;
                    return;
                }
                
                // perform edit request
                doEditMax(newVal);
            });
            listenerAdded = true;
            System.out.println("PendingRoomController - ChoiceBox listener đã được thêm");
        } catch (Exception e) {
            System.err.println("Failed to add selection listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cập nhật nhãn hiển thị số người chơi theo format "current/max"
     */
    private void updatePlayerCountLabel() {
        if (currentRoom == null) return;
        int count = (currentRoom.getPlayers() == null) ? 0 : currentRoom.getPlayers().size();
        lblCountPlayer.setText(count + " / " + currentRoom.getMaxPlayer());
        System.out.println("PendingRoomController - Cập nhật số người: " + lblCountPlayer.getText());
    }

    /**
     * Cập nhật danh sách người chơi trong bảng
     */
    private void updatePlayerList() {
        if (currentRoom == null) return;
        
        if (currentRoom.getPlayers() != null) {
            javafx.collections.ObservableList<Player> items = 
                FXCollections.observableArrayList(currentRoom.getPlayers());
            tblPlayerList.setItems(items);
            System.out.println("PendingRoomController - Cập nhật danh sách: " + 
                             currentRoom.getPlayers().size() + " người chơi");
        } else {
            tblPlayerList.setItems(FXCollections.observableArrayList());
            System.out.println("PendingRoomController - Danh sách người chơi trống");
        }
    }

    // Kiểm tra và cập nhật quyền hạn dựa trên vai trò (chủ phòng/thành viên)
    private void updateOwnerPermissions() {
        if (currentRoom == null || currentUserId == null) {
            cbxNumberPlayer.setDisable(true);
            btnStart.setDisable(true);
            return;
        }
        
        boolean isOwner = currentUserId.equals(currentRoom.getOwnerId());
        System.out.println("PendingRoomController - Kiểm tra quyền: isOwner=" + isOwner);
        
        // Chỉ chủ phòng mới được thay đổi số người tối đa và bắt đầu game
        cbxNumberPlayer.setDisable(!isOwner);
        btnStart.setDisable(!isOwner);
    }

    // ========== LISTENER METHODS (thay thế polling) ==========
    
    /**
     * Bắt đầu lắng nghe updates từ server qua persistent connection
     */
    private void startListening() {
        if (currentRoom == null || currentUserId == null) {
            System.err.println("[PendingRoom] Cannot start listening: room or userId is null");
            return;
        }
        if (listening) {
            System.out.println("[PendingRoom] Already listening, skip");
            return;
        }
        
        listening = true;
        listenerThread = new Thread(() -> {
            try {
                System.out.println("[PendingRoom] Starting listener for room " + currentRoom.getId());
                
                listenerSocket = new Socket("localhost", 2208);
                listenerOut = new ObjectOutputStream(listenerSocket.getOutputStream());
                listenerIn = new ObjectInputStream(listenerSocket.getInputStream());
                
                Request req = new Request("ROOM", "LISTEN", currentRoom.getId() + "," + currentUserId);
                listenerOut.writeObject(req);
                listenerOut.flush();
                
                System.out.println("[PendingRoom] Listener started for room " + currentRoom.getId());
                
                while (listening && !listenerSocket.isClosed()) {
                    try {
                        Response response = (Response) listenerIn.readObject();
                        handleServerUpdate(response);
                    } catch (Exception e) {
                        if (listening) {
                            System.err.println("[PendingRoom] Listener read error: " + e.getMessage());
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[PendingRoom] Failed to start listener: " + e.getMessage());
            } finally {
                System.out.println("[PendingRoom] Listener thread ending");
            }
        }, "PendingRoomListener-" + currentRoom.getId());
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Xử lý updates nhận được từ server
     */
    private void handleServerUpdate(Response response) {
        System.out.println("Received: " + response.getMaLenh());
        
        if ("UPDATE".equals(response.getMaLenh())) {
            // Room được cập nhật
            try {
                Room updatedRoom = gson.fromJson(response.getData(), Room.class);
                
                // Kiểm tra nếu game bắt đầu
                if ("playing".equals(updatedRoom.getStatus()) && 
                    !"playing".equals(currentRoom.getStatus())) {
                    System.out.println("Game started! Navigating to PlayingRoom...");
                    javafx.application.Platform.runLater(() -> {
                        // NOTE: GIỮ listener chạy để không bị auto-kick!
                        navigateToPlayingRoom(updatedRoom);
                    });
                    return;
                }
                
                // Cập nhật UI trên JavaFX thread
                javafx.application.Platform.runLater(() -> updateRoomData(updatedRoom));
                
            } catch (Exception e) {
                System.err.println("Error parsing room update: " + e.getMessage());
                e.printStackTrace();
            }
            
        } else if ("KICKED".equals(response.getMaLenh())) {
            // Bị kick khỏi phòng
            System.out.println("👢 You were kicked from the room!");
            javafx.application.Platform.runLater(() -> {
                stopListening();
                showInfo("Bị Kick", "Bạn đã bị kick khỏi phòng #" + currentRoom.getId());
                closeWindow();
            });
        }
    }
    
    /**
     * Dừng lắng nghe updates từ server
     */
    private void stopListening() {
        listening = false;
        try {
            if (listenerIn != null) listenerIn.close();
            if (listenerOut != null) listenerOut.close();
            if (listenerSocket != null) listenerSocket.close();
            if (listenerThread != null) listenerThread.interrupt();
        } catch (Exception e) {
            System.err.println("[PendingRoom] Error stopping listener: " + e.getMessage());
        }
        System.out.println("[PendingRoom] Listener stopped");
    }
    
    /**
     * Đóng cửa sổ
     */
    private void closeWindow() {
        try {
            if (onRoomUpdated != null) onRoomUpdated.run();
        } catch (Exception ignored) {}
        try {
            javafx.stage.Window w = btnOutRoom.getScene().getWindow();
            if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage) w).close();
        } catch (Exception ignored) {}
    }

    // ========== END LISTENER METHODS ==========

    // Load dữ liệu phòng từ server ngay lập tức (đồng bộ)
    private void loadRoomDataFromServer() {
        if (currentRoom == null) return;
        
        try (RoomController rc = new RoomController("localhost", 2208)) {
            Room latest = rc.getRoomById(currentRoom.getId());
            if (latest != null) {
                // Cập nhật dữ liệu phòng với thông tin đầy đủ từ server
                updateRoomData(latest);
                System.out.println("PendingRoomController - Đã load dữ liệu phòng từ server");
            }
        } catch (Exception e) {
            System.err.println("PendingRoomController.loadRoomDataFromServer - Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== OLD POLLING METHODS - XÓA ==========
    // (Đã được thay thế bằng listener methods ở trên)

    // ========== END OLD POLLING METHODS ==========
    
    /**
     * Cập nhật dữ liệu phòng (được gọi khi nhận update từ server)
     */
    private void updateRoomData(Room room) {
        if (room == null) return;
        
        this.currentRoom = room;
        System.out.println("PendingRoomController.updateRoomData: roomId=" + room.getId() + 
                         ", players=" + (room.getPlayers() != null ? room.getPlayers().size() : 0) + 
                         ", max=" + room.getMaxPlayer());
        
        // Cập nhật thông tin phòng
        lblRoomId.setText(String.valueOf(room.getId()));
        updatePlayerCountLabel();
        
        // Cập nhật danh sách người chơi
        updatePlayerList();
        
        // Kiểm tra quyền (chỉ chủ phòng mới được thay đổi cài đặt)
        updateOwnerPermissions();
        
        // Cập nhật ChoiceBox NHƯNG suppress listener để tránh trigger auto-edit
        suppressSelectionEvents = true;
        cbxNumberPlayer.setValue(room.getMaxPlayer());
        suppressSelectionEvents = false;
    }

    @FXML
    public void initialize() {
        System.out.println("PendingRoomController.initialize called");
        // initialize choicebox with allowed values
        cbxNumberPlayer.setItems(FXCollections.observableArrayList(Arrays.asList(2, 4, 6, 8)));
        // default selection if nothing set (nhưng KHÔNG add listener ở đây)
        if (cbxNumberPlayer.getValue() == null) cbxNumberPlayer.setValue(4);
        
        // Setup player table columns
        try {
            if (tblPlayerList.getColumns().size() >= 2) {
                // Cột 1: Tên người chơi theo format "Tên(#ID)"
                @SuppressWarnings({"unchecked","rawtypes"})
                javafx.scene.control.TableColumn<Player, String> col0 = 
                    (javafx.scene.control.TableColumn) tblPlayerList.getColumns().get(0);
                col0.setCellValueFactory(cell -> {
                    Player p = cell.getValue();
                    if (p == null) return new javafx.beans.property.SimpleStringProperty("");
                    String name = (p.getName() == null || p.getName().isBlank()) 
                        ? "User" 
                        : p.getName();
                    String text = name + " (#" + p.getUserId() + ")";
                    return new javafx.beans.property.SimpleStringProperty(text);
                });

                // Cột 2: Vai trò - Chủ phòng hoặc Thành viên
                @SuppressWarnings({"unchecked","rawtypes"})
                javafx.scene.control.TableColumn<Player, String> col1 = 
                    (javafx.scene.control.TableColumn) tblPlayerList.getColumns().get(1);
                col1.setCellValueFactory(cell -> {
                    Player p = cell.getValue();
                    if (p == null || currentRoom == null) {
                        return new javafx.beans.property.SimpleStringProperty("Thành viên");
                    }
                    Long ownerId = currentRoom.getOwnerId();
                    String role = java.util.Objects.equals(p.getUserId(), ownerId) 
                        ? "Chủ phòng" 
                        : "Thành viên";
                    return new javafx.beans.property.SimpleStringProperty(role);
                });

                // Cột 3: Hành động - Nút Kick (chỉ hiển thị cho chủ phòng và không kick chính mình)
                if (tblPlayerList.getColumns().size() >= 3) {
                    @SuppressWarnings({"unchecked","rawtypes"})
                    javafx.scene.control.TableColumn<Player, Void> col2 = 
                        (javafx.scene.control.TableColumn) tblPlayerList.getColumns().get(2);
                    
                    javafx.util.Callback<javafx.scene.control.TableColumn<Player, Void>, 
                                        javafx.scene.control.TableCell<Player, Void>> cellFactory = 
                        new javafx.util.Callback<>() {
                            @Override
                            public javafx.scene.control.TableCell<Player, Void> call(
                                    final javafx.scene.control.TableColumn<Player, Void> param) {
                                return new javafx.scene.control.TableCell<>() {
                                    private final javafx.scene.control.Button btnKick = 
                                        new javafx.scene.control.Button("Kick");

                                    {
                                        btnKick.setOnAction((javafx.event.ActionEvent event) -> {
                                            Player player = getTableView().getItems().get(getIndex());
                                            handleKickPlayer(player);
                                        });
                                    }

                                    @Override
                                    public void updateItem(Void item, boolean empty) {
                                        super.updateItem(item, empty);
                                        if (empty) {
                                            setGraphic(null);
                                        } else {
                                            Player player = getTableView().getItems().get(getIndex());
                                            // Chỉ hiển thị nút Kick nếu:
                                            // 1. User hiện tại là chủ phòng
                                            // 2. Không phải kick chính mình
                                            if (currentRoom != null && currentUserId != null && 
                                                currentUserId.equals(currentRoom.getOwnerId()) &&
                                                !currentUserId.equals(player.getUserId())) {
                                                setGraphic(btnKick);
                                            } else {
                                                setGraphic(null);
                                            }
                                        }
                                    }
                                };
                            }
                        };
                    col2.setCellFactory(cellFactory);
                }
            } else {
                // fallback: create columns if FXML didn't provide them
                javafx.scene.control.TableColumn<Player, String> colPlayer = 
                    new javafx.scene.control.TableColumn<>("Người chơi");
                colPlayer.setCellValueFactory(cell -> {
                    Player p = cell.getValue();
                    String text = (p.getName() == null || p.getName().isBlank()) 
                        ? "User #" + p.getUserId() 
                        : p.getName();
                    return new javafx.beans.property.SimpleStringProperty(text);
                });

                javafx.scene.control.TableColumn<Player, String> colRole = 
                    new javafx.scene.control.TableColumn<>("Vai trò");
                colRole.setCellValueFactory(cell -> {
                    if (currentRoom == null) {
                        return new javafx.beans.property.SimpleStringProperty("Thành viên");
                    }
                    Long ownerId = currentRoom.getOwnerId();
                    String role = java.util.Objects.equals(cell.getValue().getUserId(), ownerId) 
                        ? "Chủ phòng" 
                        : "Thành viên";
                    return new javafx.beans.property.SimpleStringProperty(role);
                });
                tblPlayerList.getColumns().addAll(java.util.Arrays.asList(colPlayer, colRole));
            }
        } catch (Exception e) {
            System.err.println("Error setting up player table columns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    @FXML
    public void OnClickOut(ActionEvent event) {
        System.out.println("PendingRoomController.OnClickOut - Rời phòng");
        
        if (currentRoom == null) {
            showError("Rời phòng", "Không có thông tin phòng.");
            return;
        }
        
        if (currentUserId == null) {
            showError("Rời phòng", "Bạn chưa đăng nhập.");
            return;
        }

        // Đánh dấu là tự out (không phải bị kick)
        isManualExit = true;

        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response response = rc.outRoom(currentRoom.getId(), currentUserId);
            
            if (response != null && response.isSuccess()) {
                System.out.println("PendingRoomController - Rời phòng thành công");
                showInfo("Rời phòng", "Bạn đã rời khỏi phòng #" + currentRoom.getId());
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                System.err.println("PendingRoomController - Lỗi khi rời phòng: " + errorMsg);
                showError("Rời phòng", "Lỗi khi rời phòng: " + errorMsg);
            }
            
            // Dừng listener
            stopListening();
            
            // Đóng cửa sổ pending room và quay về danh sách phòng
            try {
                javafx.stage.Window w = btnOutRoom.getScene().getWindow();
                if (w instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) w).close();
                }
            } catch (Exception ex) {
                System.err.println("Lỗi khi đóng cửa sổ: " + ex.getMessage());
                ex.printStackTrace();
            }
            
            // Gọi callback để reload danh sách phòng
            if (onRoomUpdated != null) {
                onRoomUpdated.run();
            }
            
        } catch (Exception e) {
            System.err.println("PendingRoomController - Exception khi rời phòng: " + e.getMessage());
            e.printStackTrace();
            showError("Rời phòng", "Không thể kết nối đến server: " + e.getMessage());
        } finally {
            // Reset flag
            isManualExit = false;
        }
    }

    @FXML
    public void OnClickStart(ActionEvent event) {
        System.out.println("PendingRoomController.OnClickStart - Bắt đầu chơi");
        
        // Kiểm tra quyền: chỉ chủ phòng mới được bắt đầu
        if (currentRoom == null || currentUserId == null) {
            showError("Bắt đầu", "Không có thông tin phòng hoặc người dùng.");
            return;
        }
        
        if (!currentUserId.equals(currentRoom.getOwnerId())) {
            showError("Bắt đầu", "Chỉ chủ phòng mới được bắt đầu!");
            return;
        }
        
        // Gọi API START game
        try (vuatiengvietpj.controller.GameController gc = new vuatiengvietpj.controller.GameController("localhost", 2208)) {
            Response response = gc.startGame(currentRoom.getId(), currentUserId);
            
            if (response != null && response.isSuccess()) {
                // Parse Room object từ response
                Room startedRoom = gc.parseRoom(response.getData());
                if (startedRoom != null) {
                    // Cập nhật room local
                    updateRoomData(startedRoom);
                    System.out.println("PendingRoomController - Game đã bắt đầu thành công, ChallengePack: " + 
                                     (startedRoom.getCp() != null ? startedRoom.getCp().getId() : "null"));
                    
                    // Chuyển sang PlayingRoom với countdown
                    navigateToPlayingRoom(startedRoom);
                } else {
                    showError("Bắt đầu game", "Không thể parse dữ liệu từ server");
                }
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                System.err.println("PendingRoomController - Lỗi khi bắt đầu game: " + errorMsg);
                showError("Bắt đầu game", "Lỗi: " + errorMsg);
            }
        } catch (Exception e) {
            System.err.println("PendingRoomController - Exception khi bắt đầu game: " + e.getMessage());
            e.printStackTrace();
            showError("Bắt đầu game", "Không thể kết nối đến server: " + e.getMessage());
        }
    }
    
    /**
     * Chuyển sang màn hình PlayingRoom khi game bắt đầu
     */
    private void navigateToPlayingRoom(Room room) {
        // PREVENT duplicate navigation
        if (isNavigatingToGame) {
            System.out.println("[PendingRoom] Already navigating to game, skip duplicate call");
            return;
        }
        isNavigatingToGame = true;
        
        try {
            // Stop listener cũ vì sẽ tạo phòng mới sau khi game end
            stopListening();
            
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/vuatiengvietpj/PlayingRoom.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            
            Object controller = loader.getController();
            PlayingRoomController prc = null;
            if (controller instanceof PlayingRoomController) {
                prc = (PlayingRoomController) controller;
                prc.setCurrentUserId(currentUserId);
                
                // Pass primaryStage - ưu tiên primaryStage, fallback sang scene.getWindow()
                javafx.stage.Stage stageToPass = primaryStage;
                if (stageToPass == null) {
                    System.out.println("[PendingRoom] primaryStage is null, getting from scene...");
                    try {
                        javafx.stage.Window w = btnOutRoom.getScene().getWindow();
                        if (w instanceof javafx.stage.Stage) {
                            stageToPass = (javafx.stage.Stage) w;
                            System.out.println("[PendingRoom] Got stage from scene: " + (stageToPass != null ? "OK" : "NULL"));
                        }
                    } catch (Exception e) {
                        System.err.println("[PendingRoom] Error getting stage from scene: " + e.getMessage());
                    }
                }
                
                if (stageToPass != null) {
                    prc.setPrimaryStage(stageToPass);
                    System.out.println("[PendingRoom] Passed primaryStage to PlayingRoomController");
                } else {
                    System.err.println("[PendingRoom] WARNING: Cannot find stage to pass to PlayingRoomController");
                }
                
                // Đánh dấu hiển thị countdown khi load
                prc.setShowCountdownOnLoad(true);
            }
            
            // Cập nhật scene TRƯỚC - attach scene vào stage
            javafx.stage.Stage stage = primaryStage;
            if (stage == null) {
                try {
                    javafx.stage.Window w = btnOutRoom.getScene().getWindow();
                    if (w instanceof javafx.stage.Stage) {
                        stage = (javafx.stage.Stage) w;
                    }
                } catch (Exception e) {
                    System.err.println("PendingRoomController - Không thể lấy Stage: " + e.getMessage());
                }
            }
            
            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle("Chơi game - Phòng #" + room.getId());
                stage.show();
                
                // GỌI setRoom() SAU KHI scene đã attach vào stage
                if (prc != null) {
                    prc.setRoom(room);
                    System.out.println("[PendingRoom] Called setRoom() after scene attached");
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("PendingRoomController.navigateToPlayingRoom - Lỗi: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi", "Không thể mở màn hình chơi game: " + e.getMessage());
        }
    }

    // perform the edit request and update UI only on success
    private void doEditMax(Integer newMax) {
        if (currentRoom == null) return;
        
        // Kiểm tra quyền
        if (currentUserId == null || !currentUserId.equals(currentRoom.getOwnerId())) {
            System.err.println("PendingRoomController.doEditMax: không có quyền chỉnh sửa");
            return;
        }
        
        System.out.println("PendingRoomController.doEditMax: sending editRoom request room=" + currentRoom.getId() + ", newMax=" + newMax);
        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response res = rc.editRoom(currentRoom.getId(), newMax);
            if (res != null && res.isSuccess()) {
                // update local view
                currentRoom.setMaxPlayer(newMax);
                updatePlayerCountLabel();
                System.out.println("PendingRoomController.doEditMax: cập nhật thành công");
                showInfo("Cập nhật phòng", "Số người tối đa đã được cập nhật: " + newMax);
            } else {
                String msg = (res == null ? "no response" : res.getData());
                System.err.println("PendingRoomController.doEditMax: server returned error: " + msg);
                showError("Cập nhật phòng", "Cập nhật thất bại: " + msg);
                // revert selection to previous value
                suppressSelectionEvents = true;
                cbxNumberPlayer.setValue(currentRoom.getMaxPlayer());
                suppressSelectionEvents = false;
            }
        } catch (Exception e) {
            System.err.println("PendingRoomController.doEditMax error: " + e.getMessage());
            e.printStackTrace();
            showError("Cập nhật phòng", "Lỗi khi cập nhật phòng: " + e.getMessage());
            // revert selection
            suppressSelectionEvents = true;
            cbxNumberPlayer.setValue(currentRoom.getMaxPlayer());
            suppressSelectionEvents = false;
        }
    }

    /**
     * Xử lý kick người chơi khỏi phòng (chỉ chủ phòng)
     */
    private void handleKickPlayer(Player player) {
        if (player == null || currentRoom == null || currentUserId == null) {
            showError("Kick người chơi", "Thông tin không hợp lệ");
            return;
        }

        // Kiểm tra quyền: chỉ chủ phòng mới được kick
        if (!currentUserId.equals(currentRoom.getOwnerId())) {
            showError("Kick người chơi", "Chỉ chủ phòng mới được kick người chơi!");
            return;
        }

        // Không thể kick chính mình
        if (currentUserId.equals(player.getUserId())) {
            showError("Kick người chơi", "Bạn không thể kick chính mình!");
            return;
        }

        // Confirm trước khi kick
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận Kick");
        confirmAlert.setHeaderText(null);
        String playerName = (player.getName() == null || player.getName().isBlank()) 
            ? "User #" + player.getUserId() 
            : player.getName();
        confirmAlert.setContentText("Bạn có chắc muốn kick " + playerName + " khỏi phòng?");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // Thực hiện kick
            try (RoomController rc = new RoomController("localhost", 2208)) {
                Response response = rc.kickPlayer(currentRoom.getId(), currentUserId, player.getUserId());
                
                if (response != null && response.isSuccess()) {
                    System.out.println("PendingRoomController - Kick thành công player: " + player.getUserId());
                    showInfo("Kick người chơi", "Đã kick " + playerName + " khỏi phòng");
                    // Room sẽ tự động cập nhật qua polling
                } else {
                    String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                    System.err.println("PendingRoomController - Lỗi khi kick: " + errorMsg);
                    showError("Kick người chơi", "Không thể kick: " + errorMsg);
                }
            } catch (Exception e) {
                System.err.println("PendingRoomController - Exception khi kick: " + e.getMessage());
                e.printStackTrace();
                showError("Kick người chơi", "Không thể kết nối đến server: " + e.getMessage());
            }
        }
    }

}
