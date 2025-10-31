package vuatiengvietpj.controller;

import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

import vuatiengvietpj.model.Room;

public class PendingRoomController {

    @FXML
    private Button btnOutRoom;

    @FXML
    private Button btnStart;

    @FXML
    private ChoiceBox<Integer> cbxNumberPlayer;

    @FXML
    private Label lblCountPlayer;

    @FXML
    private Label lblRoomId;

    // state
    private Room currentRoom;
    private Long currentUserId;

    public void setCurrentUserId(Long id) {
        this.currentUserId = id;
    }

    public void setRoom(Room room) {
        this.currentRoom = room;
        if (room != null) {
            lblRoomId.setText(String.valueOf(room.getId()));
            int count = (room.getPlayers() == null) ? 0 : room.getPlayers().size();
            lblCountPlayer.setText(count + " / " + room.getMaxPlayer());
            cbxNumberPlayer.setValue(room.getMaxPlayer());
        }
    }

    @FXML
    public void initialize() {
        // initialize choicebox with allowed values
        cbxNumberPlayer.setItems(FXCollections.observableArrayList(Arrays.asList(2, 4, 6, 8)));
        // default selection if nothing set
        if (cbxNumberPlayer.getValue() == null) cbxNumberPlayer.setValue(4);
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
    void OnClickOut(ActionEvent event) {
        if (currentRoom == null || currentUserId == null) {
            showError("Rời phòng", "Không có thông tin phòng hoặc người dùng chưa đăng nhập.");
            return;
        }

        try (RoomController rc = new RoomController("localhost", 2208)) {
            // send only roomId; server will determine the user from the session
            rc.outRoom(currentRoom.getId());
            showInfo("Rời phòng", "Bạn đã rời khỏi phòng: " + currentRoom.getId());
            // refresh room info from server
            Room updated = rc.getRoomById(currentRoom.getId());
            if (updated != null) {
                this.currentRoom = updated;
                int count = (updated.getPlayers() == null) ? 0 : updated.getPlayers().size();
                lblCountPlayer.setText(count + " / " + updated.getMaxPlayer());
                cbxNumberPlayer.setValue(updated.getMaxPlayer());
            } else {
                // room may have been deleted -> clear
                lblCountPlayer.setText("0 / " + cbxNumberPlayer.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Rời phòng", "Lỗi khi rời phòng: " + e.getMessage());
        }
    }

    @FXML
    void OnClickStart(ActionEvent event) {
        // Start logic (not requested) - could call refreshRoom to set isPlaying=true
    }

    // Called when user changes max player via choicebox (hook this in FXML as onAction="#onMaxChanged")
    @FXML
    void onMaxChanged(ActionEvent event) {
        if (currentRoom == null) {
            showError("Cập nhật phòng", "Không có phòng để cập nhật.");
            return;
        }
        Integer newMax = cbxNumberPlayer.getValue();
        if (newMax == null) return;
        try (RoomController rc = new RoomController("localhost", 2208)) {
            rc.editRoom(currentRoom.getId(), newMax);
            // update local view
            currentRoom.setMaxPlayer(newMax);
            int count = (currentRoom.getPlayers() == null) ? 0 : currentRoom.getPlayers().size();
            lblCountPlayer.setText(count + " / " + newMax);
            showInfo("Cập nhật phòng", "Số người tối đa đã được cập nhật: " + newMax);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cập nhật phòng", "Lỗi khi cập nhật phòng: " + e.getMessage());
        }
    }

}
