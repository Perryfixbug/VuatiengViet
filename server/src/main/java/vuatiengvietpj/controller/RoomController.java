package vuatiengvietpj.controller;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import vuatiengvietpj.dao.RoomDAO;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.util.RoomUpdateManager;

public class RoomController extends ServerController {
    private RoomDAO roomDAO;
    private String module = "ROOM";
    private Gson gson;

    public RoomController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.roomDAO = new RoomDAO();
        this.gson = new GsonBuilder()
                // do not exclude fields without @Expose — we want full model serialization for
                // client
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    @Override
    public Response process(Request request) throws IOException {
        String data = request.getData();
        return switch (request.getMaLenh()) {
            case "CREATE" -> handleCreate(data);
            case "JOIN" -> handleJoin(data);
            case "EDIT" -> handleEdit(data);
            case "OUT" -> handleOut(data);
            case "GETALL" -> handleGetAll();
            case "GETBYID" -> handleGetById(data);
            case "REFRESH" -> handleRefresh(data);
            case "KICK" -> handleKick(data);
            case "LISTEN" -> handleListen(data);
            default -> createErrorResponse(module, request.getMaLenh(), "Hanh dong khong hop le");
        };
    }

    // Tạo phòng mới
    private Response handleCreate(String data) {
        // data format: "ownerId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "CREATE", "Du lieu khong hop le");
        }
        try {
            Integer ownerId = Integer .parseInt(data);
            Room room = createRoom(ownerId);
            if (room != null) {
                return createSuccessResponse(module, "CREATE", gson.toJson(room));
            } else {
                return createErrorResponse(module, "CREATE", "Tao phong moi that bai");
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "CREATE", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Tham gia phòng
    private Response handleJoin(String data) {
        // data format: "roomId,userId"
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "JOIN", "Du lieu khong hop le");
        }

        try {
            Integer roomId = Integer .parseInt(parts[0]);
            Integer userId = Integer .parseInt(parts[1]);
            Room room = joinRoom(roomId, userId);
            if (room != null) {
                // Broadcast update tới tất cả listeners trong phòng
                RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
                
                return createSuccessResponse(module, "JOIN", gson.toJson(room));
            } else {
                return createErrorResponse(module, "JOIN", "Tham gia phong that bai hoac phong da day");
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "JOIN", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Chỉnh sửa phòng
    private Response handleEdit(String data) {
        // data format: "roomId,maxPlayer"
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "EDIT", "Du lieu khong hop le");
        }
        try {
            Integer roomId = Integer .parseInt (parts[0]);
            int maxPlayer = parts[1].isEmpty() ? 0 : Integer.parseInt(parts[1]);
            Room room = editRoom(roomId, maxPlayer);
            if (room != null) {
                // Broadcast update tới tất cả listeners trong phòng
                RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
                
                return createSuccessResponse(module, "EDIT", gson.toJson(room));
            } else {
                return createErrorResponse(module, "EDIT", "Loi chinh sua phong");
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "EDIT", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Rời phòng
    private Response handleOut(String data) {
        // data format: "roomId,userId"
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "OUT", "Du lieu khong hop le");
        }
        try {
            Integer roomId = Integer.parseInt(parts[0]);
            Integer userId = Integer.parseInt(parts[1]);
            Room room = outRoom(roomId, userId);
            
            // Broadcast update nếu phòng còn tồn tại và có người
            if (room != null && room.getPlayers() != null && !room.getPlayers().isEmpty()) {
                RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
            }
            
            return createSuccessResponse(module, "OUT", gson.toJson(room));
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "OUT", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Lấy tất cả phòng
    private Response handleGetAll() {
        try {
            List<Room> rooms = getAllRooms();
            System.out.println("Rooms: " + rooms);
            return createSuccessResponse(module, "GETALL", gson.toJson(rooms));
        } catch (Exception e) {
            return createErrorResponse(module, "GETALL", "Loi khi lay danh sach phong: " + e.getMessage());
        }
    }

    // Tìm phòng theo id, trả về danh sách (có thể rỗng nếu không tìm thấy)
    private Response handleGetById(String data) {
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "GETBYID", "Du lieu khong hop le");
        }
        try {
            Integer roomId = Integer .parseInt(data);
            Room room = roomDAO.getRoomById(roomId);
            List<Room> result = new ArrayList<>();
            if (room != null)
                result.add(room);
            return createSuccessResponse(module, "GETBYID", gson.toJson(result));
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "GETBYID", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Gọi khi bắt đầu chơi hoặc kết thúc ván chơi
    private Response handleRefresh(String data) {
        // data format: "roomId,isPlaying"
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "REFRESH", "Du lieu khong hop le");
        }
        try {
            Integer roomId = Integer.parseInt(parts[0]);
            boolean isPlaying = Boolean.parseBoolean(parts[1]);

            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "REFRESH", "Phong khong ton tai");
            }
            room = changeStatus(roomId, isPlaying);
            return createSuccessResponse(module, "REFRESH", gson.toJson(room));
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "REFRESH", "Du lieu khong hop le: " + e.getMessage());
        }
    }

    // Đuổi người chơi khác chủ phòng ra khỏi phòng
    public Response handleKick(String data) {
        // data format: "roomId,userId,kickedPlayerId"
        String[] parts = data.split(",");
        if (parts.length < 3) {
            return createErrorResponse(module, "KICK", "Du lieu khong hop le");
        }
        Integer roomId = Integer.parseInt(parts[0]);
        Integer userId = Integer.parseInt(parts[1]);
        Integer kickedPlayerId = Integer.parseInt(parts[2]);
        Room room = kickPlayer(roomId, userId, kickedPlayerId);
        if (room != null) {
            // Gửi KICKED message tới user bị kick
            RoomUpdateManager.getInstance().sendToUser(roomId, kickedPlayerId, "KICKED", "Ban da bi kick");
            
            // Broadcast update tới những người còn lại
            RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
            
            return createSuccessResponse(module, "KICK", gson.toJson(room));
        } else {
            return createErrorResponse(module, "KICK", "Du lieu khong hop le hoac khong the kick");
        }
    }
    
    // Xử lý LISTEN - persistent connection để nhận updates
    private Response handleListen(String data) {
        // data format: "roomId,userId"
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "LISTEN", "Du lieu khong hop le");
        }
        
        try {
            Integer roomId = Integer.parseInt(parts[0]);
            Integer userId = Integer.parseInt(parts[1]);

            System.out.println("RoomController.LISTEN: room=" + roomId + ", user=" + userId);
            
            // SỬA: Sử dụng lại ObjectOutputStream có sẵn từ ServerController
            // KHÔNG tạo mới vì sẽ corrupt stream!
            // ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            
            // Đăng ký listener với out có sẵn
            RoomUpdateManager.getInstance().addListener(roomId, userId, this.out);
            
            // Gửi initial room state ngay lập tức
            Room room = roomDAO.getRoomById(roomId);
            if (room != null) {
                RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
            }
            
            // GIỮ CONNECTION MỞ - đọc liên tục để phát hiện disconnect
            try {
                byte[] buffer = new byte[1];
                while (true) {
                    // Đọc để check connection alive (client sẽ không gửi gì)
                    // Khi client disconnect, read() sẽ throw exception
                    int result = clientSocket.getInputStream().read(buffer);
                    if (result == -1) {
                        // Client đóng connection gracefully
                        System.out.println("RoomController.LISTEN: Client disconnected gracefully - user=" + userId);
                        break;
                    }
                }
            } catch (Exception e) {
                // Client disconnect (network error, force close...)
                System.out.println("RoomController.LISTEN: Client disconnected - user=" + userId + ", reason: " + e.getMessage());
            } finally {
                // Cleanup: xóa listener
                RoomUpdateManager.getInstance().removeListener(roomId, userId);
                System.out.println("RoomController.LISTEN: Cleanup done for user=" + userId);
                
                // AUTO-KICK: Tự động đuổi player khỏi phòng khi disconnect
                // NHƯNG chỉ áp dụng khi:
                // 1. Phòng đang ở trạng thái "pending" (chưa chơi)
                // 2. User KHÔNG CÓ listener nào khác đang active (tránh kick khi đang navigate)
                try {
                    // Delay nhỏ để cho client kịp tạo listener mới khi navigate
                    Thread.sleep(500);
                    
                    // Kiểm tra xem user có listener nào khác không
                    boolean hasOtherListener = RoomUpdateManager.getInstance().hasListener(roomId, userId);
                    
                    if (hasOtherListener) {
                        System.out.println("RoomController.LISTEN: User=" + userId + " has another active listener, skip auto-kick");
                    } else {
                        Room latestRoom = roomDAO.getRoomById(roomId);
                        if (latestRoom != null && latestRoom.getPlayers() != null) {
                            // CHỈ auto-kick khi phòng đang "pending" (chưa chơi)
                            if ("pending".equals(latestRoom.getStatus())) {
                                // Kiểm tra user còn trong phòng không
                                boolean userInRoom = latestRoom.getPlayers().stream()
                                    .anyMatch(p -> p.getUserId().equals(userId));
                                
                                if (userInRoom) {
                                    System.out.println("RoomController.LISTEN: Auto-kicking user=" + userId + " from pending room=" + roomId);
                                    
                                    // Gọi logic OUT để remove player
                                    Room updatedRoom = outRoom(roomId, userId);
                                    
                                    if (updatedRoom != null) {
                                        // Broadcast update tới những người còn lại
                                        RoomUpdateManager.getInstance().broadcastUpdate(roomId, updatedRoom);
                                        System.out.println("RoomController.LISTEN: User kicked successfully");
                                    } else {
                                        // Room đã bị xóa (hết người)
                                        System.out.println("RoomController.LISTEN: Room deleted (no players left)");
                                    }
                                }
                            } else {
                                System.out.println("RoomController.LISTEN: Room is playing, keep player=" + userId + " for reconnect");
                            }
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception cleanupError) {
                    System.err.println("RoomController.LISTEN: Auto-kick failed: " + cleanupError.getMessage());
                }
            }
            
            // Không bao giờ return trong trường hợp bình thường (connection giữ mở)
            return createSuccessResponse(module, "LISTEN", "Disconnected");
            
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "LISTEN", "Du lieu khong hop le: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("RoomController.LISTEN error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(module, "LISTEN", "Loi server: " + e.getMessage());
        }
    }

    // Các phương thức hỗ trợ
    private Integer generateRoomId() {
        long timstamp = System.currentTimeMillis() + new Random().nextInt(1000);
        int roomId = (int) (timstamp % 100000000);
        return (Integer) roomId;
    }

    public List<Room> getAllRooms() {
        return roomDAO.getAllRooms();
    }

    public Room getRoomById(Integer roomId) {
        return roomDAO.getRoomById(roomId);
    }

    // Thay đổi trạng thái phòng
    public Room changeStatus(Integer roomId, boolean isPlaying) {
        // Đổi trạng thái hiện tại của phòng qua biến isPlaying
        Room room = roomDAO.getRoomById(roomId);
        if (room == null)
            return null;
        // Sửa logic: isPlaying = true → status = "playing", isPlaying = false → status = "pending"
        String status = isPlaying ? "playing" : "pending";
        roomDAO.updateRoom(roomId, null, status, null);
        room.setStatus(status);
        return room;
    }

    public Room createRoom(Integer ownerId) {
        List<Player> players = new ArrayList<>();
        Player player = new Player();
        player.setUserId(ownerId);
        players.add(player);

        Integer roomId = generateRoomId();
        Room room = new Room(roomId, ownerId, 4, Instant.now(), "pending", null, players);
        roomDAO.createRoom(room);
        return room;
    }

    public Room joinRoom(Integer roomId, Integer userId) {
        System.out.println("RoomController.joinRoom: roomId=" + roomId + ", userId=" + userId);
        Room room = roomDAO.getRoomById(roomId);
        if (room == null || userId == null || room.getMaxPlayer() == 0) {
            System.out.println("RoomController.joinRoom: room null or invalid - room=" + (room == null ? "null" : "ok")
                    + ", userId=" + userId);
            return null;
        }
        if (room.getPlayers().size() >= room.getMaxPlayer()) {
            System.out.println(
                    "RoomController.joinRoom: room full - " + room.getPlayers().size() + "/" + room.getMaxPlayer());
            return null;
        }
        List<Player> players = room.getPlayers();
        for (Player p : players) {
            if (java.util.Objects.equals(p.getUserId(), userId)) {
                System.out.println(
                        "RoomController.joinRoom: user already in room - userId=" + userId + " already exists");
                return null;
            }
        }
        Player newPlayer = new Player();
        newPlayer.setUserId(userId);
        players.add(newPlayer);
        room.setPlayers(players);
        roomDAO.addPlayerToRoom(roomId, userId);
        // Thêm vào Redis scoreboard với điểm 0 và cập nhật tên nếu có
        try {
            redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
            if (jedis != null) {
                try (jedis) {
                    String zKey = "scoreboard:" + roomId;
                    String hKey = "scoreboard:names:" + roomId;
                    jedis.zadd(zKey, 0.0, "user:" + userId);
                    // Tên có thể null ở đây; nếu cần, có thể lấy từ DB User
                    // Để đồng bộ, lưu tạm userId làm name nếu thiếu
                    jedis.hset(hKey, "user:" + userId, String.valueOf(userId));
                }
            }
        } catch (Exception ex) {
            System.err.println("RoomController.joinRoom: Failed to add user to Redis scoreboard - " + ex.getMessage());
        }
        System.out.println(
                "RoomController.joinRoom: SUCCESS - added userId=" + userId + ", now " + players.size() + " players");
        return room;
    }

    public Room editRoom(Integer roomId, Integer maxPlayer) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null)
            return null;
        if (maxPlayer != null && maxPlayer > 8) {
            maxPlayer = 8;
        }
        room.setMaxPlayer(maxPlayer);
        roomDAO.updateRoom(roomId, null, null, room.getMaxPlayer());
        return room;
    }

    public Room outRoom(Integer roomId, Integer userId) {
        System.out.println("RoomController.outRoom: roomId=" + roomId + ", userId=" + userId);
        Room room = roomDAO.getRoomById(roomId);
        if (room == null || userId == null) {
            System.out.println("RoomController.outRoom: room or userId null");
            return null;
        }
        List<Player> players = room.getPlayers();
        if (players == null) {
            System.out.println("RoomController.outRoom: players list null");
            return null;
        }
        Player toRemove = null;
        for (Player p : players) {
            if (java.util.Objects.equals(p.getUserId(), userId)) {
                toRemove = p;
                break;
            }
        }
        if (toRemove != null) {
            players.remove(toRemove);
            room.setPlayers(players);
            roomDAO.deletePlayerFromRoom(roomId, userId);
            System.out.println("RoomController.outRoom: SUCCESS - removed userId=" + userId + ", now " + players.size()
                    + " players");
            // Giữ nguyên điểm trong Redis (không ZREM). Có thể cập nhật trạng thái nếu cần.

            if (players.isEmpty()) {
                // Xóa phòng khi không còn người chơi (bất kể trạng thái pending hay playing)
                roomDAO.deleteRoom(roomId);
                System.out.println("RoomController.outRoom: Deleted empty room - roomId=" + roomId + ", status=" + room.getStatus());
            } else if (room.getOwnerId().equals(userId)) {
                room.setOwnerId(players.get(0).getUserId());
                roomDAO.updateRoom(roomId, room.getOwnerId(), null, null);
            }
        } else {
            System.out.println("RoomController.outRoom: player not found in room - userId=" + userId);
        }
        return room;
    }

    public Room kickPlayer(Integer roomId, Integer userId, Integer kickedPlayerId) {
        if (roomId == null || userId == null || kickedPlayerId == null)
            return null;
        Room room = roomDAO.getRoomById(roomId);
        if (room == null)
            return null;
        if (!java.util.Objects.equals(room.getOwnerId(), userId) || java.util.Objects.equals(userId, kickedPlayerId)
                || room.getPlayers().size() <= 1)
            return null;
        List<Player> players = room.getPlayers();
        if (players == null)
            return null;
        Player toRemove = null;
        for (Player p : players) {
            if (java.util.Objects.equals(p.getUserId(), kickedPlayerId)) {
                toRemove = p;
                break;
            }
        }
        if (toRemove != null) {
            players.remove(toRemove);
            room.setPlayers(players);
            roomDAO.deletePlayerFromRoom(roomId, kickedPlayerId);
        }
        return room;
    }
}
