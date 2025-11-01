package vuatiengvietpj.controller;

import java.util.Arrays;

import javafx.collections.FXCollections;
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
    // suppress selection events when we programmatically set ChoiceBox value
    private boolean suppressSelectionEvents = false;
    // polling executor to refresh room state periodically
    private java.util.concurrent.ScheduledExecutorService poller;
    // optional callback to notify parent/list controller to refresh room list
    private Runnable onRoomUpdated;

    public void setCurrentUserId(Long id) {
        this.currentUserId = id;
        System.out.println("PendingRoomController.setCurrentUserId: " + id);
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
            
            // Bắt đầu polling để cập nhật trạng thái phòng
            startPolling();
            
            // Stop polling khi đóng cửa sổ
            try {
                javafx.stage.Window w = btnOutRoom.getScene().getWindow();
                if (w instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) w).setOnHidden(evt -> stopPolling());
                }
            } catch (Exception ignored) {}
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

    /**
     * Kiểm tra và cập nhật quyền hạn dựa trên vai trò (chủ phòng/thành viên)
     */
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


    private void startPolling() {
        stopPolling();
        // create a daemon thread so the JVM can exit if only the poller remains
        poller = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("PendingRoom-poller-" + (currentRoom == null ? "unknown" : currentRoom.getId()));
            return t;
        });
        System.out.println("PendingRoomController.startPolling: started poller for room=" + (currentRoom == null ? "null" : currentRoom.getId()));
        poller.scheduleAtFixedRate(() -> {
            try {
                if (currentRoom == null) return;
                Room latest;
                try (RoomController rc = new RoomController("localhost", 2208)) {
                    latest = rc.getRoomById(currentRoom.getId());
                } catch (Exception e) {
                    System.err.println("PendingRoomController.poller - Lỗi kết nối: " + e.getMessage());
                    return; // Skip this poll cycle
                }
                
                if (latest == null) {
                    // room no longer exists on server: notify parent and close this pending window
                    javafx.application.Platform.runLater(() -> {
                        showError("Phòng không tồn tại", "Phòng đã bị xóa hoặc không còn tồn tại trên server.");
                        try {
                            stopPolling();
                            if (onRoomUpdated != null) onRoomUpdated.run();
                        } catch (Exception ignored) {}
                        try {
                            javafx.stage.Window w = btnOutRoom.getScene().getWindow();
                            if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage) w).close();
                        } catch (Exception ignored) {}
                    });
                    return;
                }
                // if players changed or owner changed or status changed, update UI
                boolean changed = false;
                if (latest.getPlayers() == null && currentRoom.getPlayers() != null) changed = true;
                else if (latest.getPlayers() != null && currentRoom.getPlayers() == null) changed = true;
                else if (latest.getPlayers() != null && currentRoom.getPlayers() != null && latest.getPlayers().size() != currentRoom.getPlayers().size()) changed = true;
                else if (!java.util.Objects.equals(latest.getOwnerId(), currentRoom.getOwnerId())) changed = true;
                else if (!java.util.Objects.equals(latest.getStatus(), currentRoom.getStatus())) changed = true;
                else if (!java.util.Objects.equals(latest.getMaxPlayer(), currentRoom.getMaxPlayer())) changed = true;
                
                if (changed) {
                    Room finalLatest = latest;
                    javafx.application.Platform.runLater(() -> {
                        // Update room data WITHOUT triggering setRoom (to avoid re-starting poller)
                        updateRoomData(finalLatest);
                    });
                }
            } catch (Exception e) {
                System.err.println("PendingRoomController.poller error: " + e.getMessage());
                e.printStackTrace();
            }
        }, 2, 2, java.util.concurrent.TimeUnit.SECONDS); // Start after 2s delay, not immediately
    }

    /**
     * Cập nhật dữ liệu phòng mà KHÔNG restart poller (dùng cho polling update)
     */
    private void updateRoomData(Room room) {
        if (room == null) return;
        
        this.currentRoom = room;
        System.out.println("PendingRoomController.updateRoomData: roomId=" + room.getId() + 
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
        
        // Cập nhật ChoiceBox NHƯNG suppress listener để tránh trigger auto-edit
        suppressSelectionEvents = true;
        cbxNumberPlayer.setValue(room.getMaxPlayer());
        suppressSelectionEvents = false;
    }

    private void stopPolling() {
        try {
            if (poller != null && !poller.isShutdown()) poller.shutdownNow();
        } catch (Exception ignored) {}
        poller = null;
    }

    // expose a safe stop for external callers (e.g., parent stage onHidden)
    public void stopPollingPublic() {
        stopPolling();
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
                // Cột 1: Tên người chơi (hoặc userId nếu không có tên)
                @SuppressWarnings({"unchecked","rawtypes"})
                javafx.scene.control.TableColumn<Player, String> col0 = 
                    (javafx.scene.control.TableColumn) tblPlayerList.getColumns().get(0);
                col0.setCellValueFactory(cell -> {
                    Player p = cell.getValue();
                    if (p == null) return new javafx.beans.property.SimpleStringProperty("");
                    String text = (p.getName() == null || p.getName().isBlank()) 
                        ? "User #" + p.getUserId() 
                        : p.getName();
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
            
            // Dừng polling
            stopPolling();
            
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
        
        // TODO: Implement logic bắt đầu game
        showInfo("Bắt đầu", "Tính năng bắt đầu game sẽ được implement sau.");
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

}
