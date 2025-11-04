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

    private Integer currentUserId; // ID người dùng hiện tại (sẽ được set từ nơi khác)
    private Stage primaryStage;
    private String sessionId;

    public void setCurrentUserId(Integer userId) {
        this.currentUserId = userId;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        TableColumn<Room, Integer> colId = new TableColumn<>("Mã phòng");
        colId.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        colId.setPrefWidth(106);

        // Cột "Chủ phòng"
        TableColumn<Room, String> colOwner = new TableColumn<>("Chủ phòng");
        colOwner.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();
            String ownerName = room.getOwnerName();
            if (ownerName != null && !ownerName.trim().isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty(ownerName);
            } else {
                // Fallback về ID nếu không có tên
                return new javafx.beans.property.SimpleStringProperty("User #" + room.getOwnerId());
            }
        });
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
                            Room room = (Room) getTableView().getItems().get(getIndex());
                            Integer roomId = room.getId();
                            joinRoomById(roomId);
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
                
                // Log chi tiết danh sách phòng
                System.out.println("📋 Danh sách phòng (" + rooms.size() + " phòng):");
                for (Room room : rooms) {
                    System.out.println("  - Phòng #" + room.getId() + 
                                     " | Trạng thái: " + room.getStatus().toUpperCase() + 
                                     " | Người chơi: " + (room.getPlayers() != null ? room.getPlayers().size() : 0) + "/" + room.getMaxPlayer() +
                                     " | Chủ phòng: " + room.getOwnerName());
                }
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
    private void joinRoomById(Integer roomId) {
        System.out.println("ListRoomController.joinRoomById() - Tham gia phòng ID: " + roomId);

        if (currentUserId == null) {
            showAlert("Thông báo", "Bạn chưa đăng nhập!");
            return;
        }
        
        // Lấy thông tin phòng hiện tại từ table để so sánh
        Room cachedRoom = null;
        for (Room r : tblRoomList.getItems()) {
            if (r.getId().equals(roomId)) {
                cachedRoom = r;
                break;
            }
        }

        try (RoomController rc = new RoomController("localhost", 2208)) {
            Response response = rc.joinRoom(roomId, currentUserId);

            if (response != null && response.isSuccess()) {
                Room joinedRoom = rc.parseRoom(response.getData());
                
                // Log so sánh status cũ và mới
                if (cachedRoom != null) {
                    String cachedStatus = cachedRoom.getStatus();
                    String actualStatus = joinedRoom.getStatus();
                    
                    if (!cachedStatus.equalsIgnoreCase(actualStatus)) {
                        System.out.println("⚠️  CẢNH BÁO: Dữ liệu phòng đã CŨ!");
                        System.out.println("    Status trong danh sách: " + cachedStatus);
                        System.out.println("    Status thực tế: " + actualStatus);
                        System.out.println("    → Vui lòng nhấn 'Tải lại' để cập nhật danh sách phòng!");
                        
                        // Hiển thị cảnh báo cho người dùng
                        showAlert("Thông báo", 
                            "Thông tin phòng đã thay đổi!\n" +
                            "Trạng thái hiển thị: " + cachedStatus + "\n" +
                            "Trạng thái thực tế: " + actualStatus + "\n\n" +
                            "Vui lòng nhấn 'Tải lại' để cập nhật danh sách phòng.");
                    }
                }
                
                System.out.println("ListRoomController - Tham gia phòng thành công: " + roomId + ", status=" + joinedRoom.getStatus());
                
                // Kiểm tra status của phòng để navigate đúng màn hình
                if ("playing".equalsIgnoreCase(joinedRoom.getStatus())) {
                    System.out.println("ListRoomController - Room đang playing, navigate to PlayingRoom");
                    openPlayingRoom(joinedRoom);
                } else {
                    System.out.println("ListRoomController - Room đang pending, navigate to PendingRoom");
                    openPendingRoom(joinedRoom);
                }
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
                pendingController.setSessionId(sessionId);

                // Set callback để quay lại ListRoom khi rời phòng
                pendingController.setOnRoomUpdated(() -> {
                    // Quay lại màn hình ListRoom
                    returnToListRoom();
                });

                pendingController.setRoom(room);
            }

            // Sử dụng primaryStage hiện tại thay vì tạo Stage mới
            if (primaryStage != null) {
                Scene scene = new Scene(root);
                primaryStage.setTitle("Phòng chờ - Phòng #" + room.getId());
                primaryStage.setScene(scene);
                primaryStage.show();
            } else {
                System.err.println("ListRoomController - primaryStage is null!");
                showAlert("Lỗi", "Không thể mở phòng chờ: Lỗi Stage");
            }

        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi mở PendingRoom: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện phòng chờ: " + e.getMessage());
        }
    }

    // Mở giao diện phòng chơi (PlayingRoom) - cho người vào phòng đang playing
    private void openPlayingRoom(Room room) {
        if (room == null) {
            System.err.println("ListRoomController.openPlayingRoom() - Room null!");
            return;
        }

        try {
            System.out.println("ListRoomController - Mở PlayingRoom cho phòng ID: " + room.getId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/PlayingRoom.fxml"));
            Parent root = loader.load();

            // Lấy controller của PlayingRoom và set dữ liệu
            Object controller = loader.getController();
            if (controller instanceof PlayingRoomController) {
                PlayingRoomController playingController = (PlayingRoomController) controller;
                playingController.setCurrentUserId(this.currentUserId);
                playingController.setRoom(room);
                
                System.out.println("ListRoomController - Đã set room và userId cho PlayingRoom");
            }

            // Sử dụng primaryStage hiện tại
            if (primaryStage != null) {
                Scene scene = new Scene(root);
                primaryStage.setTitle("Phòng chơi - Phòng #" + room.getId());
                primaryStage.setScene(scene);
                primaryStage.show();
            } else {
                System.err.println("ListRoomController - primaryStage is null!");
                showAlert("Lỗi", "Không thể mở phòng chơi: Lỗi Stage");
            }

        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi mở PlayingRoom: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện phòng chơi: " + e.getMessage());
        }
    }

    // Quay lại màn hình ListRoom
    private void returnToListRoom() {
        try {
            System.out.println("ListRoomController - Quay lại ListRoom");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vuatiengvietpj/ListRoom.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ListRoomController) {
                ListRoomController listController = (ListRoomController) controller;
                listController.setCurrentUserId(this.currentUserId);
                listController.setPrimaryStage(this.primaryStage);
            }

            if (primaryStage != null) {
                Scene scene = new Scene(root);
                primaryStage.setTitle("Danh sách phòng");
                primaryStage.setScene(scene);
                primaryStage.show();
            }
        } catch (IOException e) {
            System.err.println("ListRoomController - Lỗi khi quay lại ListRoom: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể quay lại danh sách phòng: " + e.getMessage());
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
                ((HomeController) controller).setCurrentUserAndSession(loggedInUser, sessionId);
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
            Response response = rc.createRoom(currentUserId, null);

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
                Integer roomId = room.getId();
                joinRoomById(roomId);
                return;
            }
        }

        // Không tìm thấy phòng nào chưa đầy
        showAlert("Thông báo", "Không có phòng nào còn chỗ trống!");
    }

    @FXML
    void OnClickReload(ActionEvent event) {
        System.out.println("=".repeat(60));
        System.out.println("🔄 ListRoomController.OnClickReload() - Đang tải lại danh sách phòng...");
        System.out.println("=".repeat(60));
        loadAllRooms();
        System.out.println("✅ Đã cập nhật danh sách phòng mới nhất!");
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

        Integer roomId;
        try {
            roomId = Integer.parseInt(searchText.trim());
        } catch (NumberFormatException e) {
            showAlert("Thông báo", "Mã phòng phải là số nguyên!");
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
