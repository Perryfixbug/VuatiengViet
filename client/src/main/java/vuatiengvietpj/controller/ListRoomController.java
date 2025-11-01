package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.User;
import vuatiengvietpj.model.Response;

public class ListRoomController {

    @FXML
    private Button BtnSearch;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnCreateRoom;

    @FXML
    private Button btnQuickJoin;

    @FXML
    private Button btnReload;

    @FXML
    private ScrollPane scrollPaneTable;

    @FXML
    private TableView<Room> tblRoomList;

    @FXML
    private TextField txtSearchRoom;

    private Long currentUserId; // ID người dùng hiện tại (sẽ được set từ nơi khác)
    private Stage primaryStage;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        System.out.println("ListRoomController.initialize() - Bắt đầu khởi tạo");
        setupTableColumns();
        loadAllRooms();
    }

    // Thiết lập các cột cho bảng danh sách phòng

    private void setupTableColumns() {
        tblRoomList.getColumns().clear();

        // Cột "Mã phòng"
        TableColumn<Room, Long> colId = new TableColumn<>("Mã phòng");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(106);

        // Cột "Chủ phòng"
        TableColumn<Room, Long> colOwner = new TableColumn<>("Chủ phòng");
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        colOwner.setPrefWidth(110);

        // Cột "Số người" - hiển thị dạng "current/max"
        TableColumn<Room, String> colPlayers = new TableColumn<>("Số người");
        colPlayers.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();
            int currentPlayers = (room.getPlayers() != null) ? room.getPlayers().size() : 0;
            int maxPlayers = room.getMaxPlayer();
            return new javafx.beans.property.SimpleStringProperty(currentPlayers + "/" + maxPlayers);
        });
        colPlayers.setPrefWidth(77);

        // Cột "Trạng thái"
        TableColumn<Room, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(88);

        // Cột "Hành động" - nút "Vào"
        TableColumn<Room, Void> colAction = new TableColumn<>("Hành động");
        colAction.setPrefWidth(90);

        Callback<TableColumn<Room, Void>, TableCell<Room, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Room, Void> call(final TableColumn<Room, Void> param) {
                final TableCell<Room, Void> cell = new TableCell<>() {
                    private final Button btnJoin = new Button("Vào");

                    {
                        btnJoin.setOnAction((ActionEvent event) -> {
                            Room room = getTableView().getItems().get(getIndex());
                            joinRoomById(room.getId());
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnJoin);
                        }
                    }
                };
                return cell;
            }
        };

        colAction.setCellFactory(cellFactory);

        tblRoomList.getColumns().addAll(colId, colOwner, colPlayers, colStatus, colAction);
    }

    // Tải danh sách tất cả các phòng từ server
    private void loadAllRooms() {
        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response response = rc.getAllRooms();

            if (response != null && response.isSuccess()) {
                List<Room> rooms = rc.parseRooms(response.getData());
                ObservableList<Room> roomList = FXCollections.observableArrayList(rooms);
                tblRoomList.setItems(roomList);
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                showAlert("Lỗi", "Không thể tải danh sách phòng: " + errorMsg);
                tblRoomList.setItems(FXCollections.observableArrayList());
            }
        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
        }
    }

    // Tham gia vào phòng theo ID
    private void joinRoomById(Long roomId) {
        System.out.println("ListRoomController.joinRoomById() - Tham gia phòng ID: " + roomId);

        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập!");
            return;
        }

        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response response = rc.joinRoom(roomId, currentUserId);

            if (response != null && response.isSuccess()) {
                Room joinedRoom = rc.parseRoom(response.getData());
                System.out.println("ListRoomController - Tham gia phòng thành công: " + roomId);
                openPendingRoom(joinedRoom);
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                System.err.println("ListRoomController - Không thể tham gia phòng: " + errorMsg);
                showAlert("Lỗi", "Không thể vào phòng: " + errorMsg);
            }
        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi tham gia phòng: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
        }
    }

    // Mở giao diện phòng chờ (PendingRoom)
    private void openPendingRoom(Room room) {
        if (room == null) {
            System.err.println("ListRoomController.openPendingRoom() - Room null!");
            return;
        }

        try {
            System.out.println("ListRoomController - Mở PendingRoom cho phòng ID: " + room.getId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/PendingRoom.fxml"));
            Parent root = loader.load();

            // Lấy controller của PendingRoom và set dữ liệu
            Object controller = loader.getController();
            if (controller instanceof PendingRoomController) {
                PendingRoomController pendingController = (PendingRoomController) controller;
                pendingController.setCurrentUserId(this.currentUserId);

                // Set callback để reload danh sách khi pending room đóng
                pendingController.setOnRoomUpdated(() -> {
                    loadAllRooms();
                });

                pendingController.setRoom(room);
            }

            Stage stage = new Stage();
            stage.setTitle("Phòng chờ - Phòng #" + room.getId());
            stage.setScene(new Scene(root));

            // Khi đóng cửa sổ pending room, reload danh sách phòng
            stage.setOnHidden(evt -> {
                System.out.println("ListRoomController - PendingRoom đã đóng, reload danh sách");
                if (controller instanceof PendingRoomController) {
                    try {
                        ((PendingRoomController) controller).stopPollingPublic();
                    } catch (Exception ignored) {
                    }
                }
                loadAllRooms();
            });

            stage.show();

        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi mở PendingRoom: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện phòng chờ: " + e.getMessage());
        }
    }

    @FXML
    void OnClickBack(ActionEvent event) {
        try {
            UserController userController = new UserController();
            User loggedInUser = userController.getIn4(currentUserId);
            userController.disconnect();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/Home.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            Object controller = loader.getController();
            if (controller instanceof HomeController) {
                ((HomeController) controller).setCurrentUser(loggedInUser);
                ((HomeController) controller).setPrimaryStage(primaryStage);
            }
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Lỗi quay về Home: " + e.getMessage());
            e.printStackTrace(); // thêm stack trace
            showAlert("Lỗi", "Không thể quay về trang chủ: " + e.getMessage()); // thêm alert
        }
    }

    @FXML
    void OnClickCreate(ActionEvent event) {
        System.out.println("ListRoomController.OnClickCreate() - Tạo phòng mới");

        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập!");
            return;
        }

        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response response = rc.createRoom(currentUserId);

            if (response != null && response.isSuccess()) {
                Room createdRoom = rc.parseRoom(response.getData());
                System.out.println("ListRoomController - Tạo phòng thành công, ID: " + createdRoom.getId());
                openPendingRoom(createdRoom);
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi từ server";
                System.err.println("ListRoomController - Không thể tạo phòng: " + errorMsg);
                showAlert("Lỗi", "Không thể tạo phòng: " + errorMsg);
            }
        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi tạo phòng: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
        }
    }

    @FXML
    void OnClickJoin(ActionEvent event) {
        System.out.println("ListRoomController.OnClickJoin() - Vào nhanh");

        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập!");
            return;
        }

        ObservableList<Room> rooms = tblRoomList.getItems();

        if (rooms == null || rooms.isEmpty()) {
            showAlert("Thông báo", "Không có phòng nào để tham gia!");
            return;
        }

        // Tìm phòng đầu tiên chưa đầy
        for (Room room : rooms) {
            int currentPlayers = (room.getPlayers() != null) ? room.getPlayers().size() : 0;
            int maxPlayers = room.getMaxPlayer();

            if (currentPlayers < maxPlayers) {
                System.out.println("ListRoomController - Tìm thấy phòng chưa đầy: " + room.getId());
                joinRoomById(room.getId());
                return;
            }
        }

        // Không tìm thấy phòng nào chưa đầy
        showAlert("Thông báo", "Không có phòng nào còn chỗ trống!");
    }

    @FXML
    void OnClickReload(ActionEvent event) {
        System.out.println("ListRoomController.OnClickReload() - Tải lại danh sách phòng");
        loadAllRooms();
    }

    @FXML
    void OnClickSearch(ActionEvent event) {
        searchRoom(event);
    }

    @FXML
    void searchRoom(ActionEvent event) {
        System.out.println("ListRoomController.searchRoom() - Tìm kiếm phòng");

        String searchText = txtSearchRoom.getText();

        if (searchText == null || searchText.trim().isEmpty()) {
            showAlert("Thông báo", "Vui lòng nhập mã phòng cần tìm!");
            return;
        }

        Long roomId;
        try {
            roomId = Long.parseLong(searchText.trim());
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Mã phòng phải là số!");
            return;
        }

        System.out.println("ListRoomController - Tìm kiếm phòng ID: " + roomId);

        try (RoomController rc = new RoomController("localhost", 2208)) {
            Room room = rc.getRoomById(roomId);

            if (room != null) {
                System.out.println("ListRoomController - Tìm thấy phòng: " + roomId);

                if (currentUserId == null) {
                    showAlert("Thông báo", "Bạn chưa đăng nhập!");
                    return;
                }

                // Kiểm tra phòng có đầy không
                int currentPlayers = (room.getPlayers() != null) ? room.getPlayers().size() : 0;
                int maxPlayers = room.getMaxPlayer();

                if (currentPlayers >= maxPlayers) {
                    showAlert("Thông báo", "Phòng đã đầy!");
                    return;
                }

                joinRoomById(roomId);
            } else {
                System.err.println("ListRoomController - Không tìm thấy phòng: " + roomId);
                showAlert("Không tìm thấy", "Không tìm thấy phòng với mã: " + roomId);
            }
        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi tìm phòng: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
        }
    }

    // Hiển thị hộp thoại thông báo
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
