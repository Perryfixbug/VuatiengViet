package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import javafx.scene.control.TextField;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Response;

public class ListRoomController {

    @FXML
    private Button BtnSearch;

    @FXML
    private Button btnCreateRoom;

    @FXML
    private Button btnQuickJoin;

    @FXML
    private TableView<Room> tblRoomList;

    @FXML
    private TextField txtSearchRoom;

    // current logged-in user id; set this from your login flow
    private Long currentUserId;

    public void setCurrentUserId(Long id) {
        this.currentUserId = id;
    }

    @FXML
    public void initialize() {
        // configure table columns (id, owner, players, status, action)
        if (tblRoomList.getColumns().isEmpty()) {
            TableColumn<Room, String> colId = new TableColumn<>("Mã phòng");
            colId.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getId())));

            TableColumn<Room, String> colOwner = new TableColumn<>("Chủ phòng");
            colOwner.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getOwnerId())));

            TableColumn<Room, String> colCount = new TableColumn<>("Số người");
            colCount.setCellValueFactory(cell -> {
                Room r = cell.getValue();
                int current = (r.getPlayers() == null) ? 0 : r.getPlayers().size();
                return new javafx.beans.property.SimpleStringProperty(current + " / " + r.getMaxPlayer());
            });

            TableColumn<Room, String> colStatus = new TableColumn<>("Trạng thái");
            colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getStatus())));

            TableColumn<Room, Void> colAction = createActionColumn();
            java.util.List<TableColumn<Room, ?>> cols = java.util.Arrays.asList(colId, colOwner, colCount, colStatus, colAction);
            tblRoomList.getColumns().addAll(cols);
        }

        loadRooms();
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void loadRooms() {
        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response res = rc.getAllRooms();
            if (res != null && res.isSuccess()) {
                List<Room> rooms = new com.google.gson.Gson().fromJson(res.getData(), new TypeToken<List<Room>>(){}.getType());
                ObservableList<Room> items = FXCollections.observableArrayList(rooms);
                tblRoomList.setItems(items);
            } else {
                tblRoomList.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể tải danh sách phòng: " + e.getMessage());
        }
    }

    @FXML
    void OnClickCreate(ActionEvent event) {
        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập.");
            return;
        }
        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response res = rc.createRoom(currentUserId);
            if (res != null && res.isSuccess()) {
                showAlert("Tạo phòng", "Tạo phòng thành công");
                loadRooms();
            } else {
                showAlert("Tạo phòng", "Tạo phòng thất bại: " + (res == null ? "no response" : res.getData()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể kết nối tới server: " + e.getMessage());
        }
    }

    private TableColumn<Room, Void> createActionColumn() {
        TableColumn<Room, Void> col = new TableColumn<>(" ");

        Callback<TableColumn<Room, Void>, TableCell<Room, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Room, Void> call(final TableColumn<Room, Void> param) {
                final TableCell<Room, Void> cell = new TableCell<>() {

                    private final Button btn = new Button("Vào");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            event.consume();
                            Room data = getTableView().getItems().get(getIndex());
                            // attempt join
                            if (currentUserId == null) {
                                showAlert("Thông báo", "Bạn chưa đăng nhập.");
                                return;
                            }
                            int current = (data.getPlayers() == null) ? 0 : data.getPlayers().size();
                            if (current >= data.getMaxPlayer()) {
                                showAlert("Vào phòng", "Phòng đầy");
                                return;
                            }
                            try (RoomController rc = new RoomController("localhost", 2208)) {
                                Response res = rc.joinRoom(data.getId(), currentUserId);
                                if (res != null && res.isSuccess()) {
                                    showAlert("Vào phòng", "Vào phòng thành công: " + data.getId());
                                    loadRooms();
                                } else {
                                    showAlert("Vào phòng", "Vào phòng thất bại: " + (res == null ? "no response" : res.getData()));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                showAlert("Lỗi", "Kết nối thất bại: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        col.setCellFactory(cellFactory);
        col.setPrefWidth(80);
        return col;
    }

    @FXML
    void OnClickJoin(ActionEvent event) {
        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập.");
            return;
        }
        ObservableList<Room> items = tblRoomList.getItems();
        boolean joined = false;
        try (RoomController rc = new RoomController("localhost", 2208)) {
            for (Room r : items) {
                int current = (r.getPlayers() == null) ? 0 : r.getPlayers().size();
                if (current < r.getMaxPlayer()) {
                    Response res = rc.joinRoom(r.getId(), currentUserId);
                    if (res != null && res.isSuccess()) {
                        showAlert("Vào phòng", "Vào phòng thành công: " + r.getId());
                        joined = true;
                        break;
                    }
                }
            }
            if (!joined) showAlert("Vào phòng", "Không tìm thấy phòng trống để vào.");
            loadRooms();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Kết nối thất bại: " + e.getMessage());
        }
    }

    @FXML
    void OnClickSearch(ActionEvent event) {
        String text = txtSearchRoom.getText();
        if (text == null || text.trim().isEmpty()) {
            showAlert("Tìm phòng", "Vui lòng nhập mã phòng");
            return;
        }
        Long roomId = null;
        try {
            roomId = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            showAlert("Tìm phòng", "Mã phòng không hợp lệ");
            return;
        }

        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập.");
            return;
        }

        RoomController rc = null;
        try {
            rc = new RoomController("localhost", 2208);
            Room room = rc.getRoomById(roomId);
            if (room == null) {
                showAlert("Tìm phòng", "Mã phòng không chính xác");
            } else {
                int current = (room.getPlayers() == null) ? 0 : room.getPlayers().size();
                if (current < room.getMaxPlayer()) {
                    Response res = rc.joinRoom(room.getId(), currentUserId);
                    if (res != null && res.isSuccess()) {
                        showAlert("Vào phòng", "Vào phòng thành công: " + room.getId());
                    } else {
                        showAlert("Vào phòng", "Vào phòng thất bại: " + (res == null ? "no response" : res.getData()));
                    }
                } else {
                    showAlert("Vào phòng", "Phòng đầy");
                }
            }
            loadRooms();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Kết nối thất bại: " + e.getMessage());
        } finally {
            if (rc != null) rc.disconnect();
        }
    }

    @FXML
    void searchRoom(ActionEvent event) {
        OnClickSearch(event);
    }

}
