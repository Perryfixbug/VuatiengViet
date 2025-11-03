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
            Long ownerId = Long.parseLong(data);
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
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            Room room = joinRoom(roomId, userId);
            if (room != null) {
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
            Long roomId = Long.parseLong(parts[0]);
            int maxPlayer = parts[1].isEmpty() ? 0 : Integer.parseInt(parts[1]);
            Room room = editRoom(roomId, maxPlayer);
            if (room != null) {
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
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            Room room = outRoom(roomId, userId);
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
            Long roomId = Long.parseLong(data);
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
            Long roomId = Long.parseLong(parts[0]);
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
        Long roomId = Long.parseLong(parts[0]);
        Long userId = Long.parseLong(parts[1]);
        Long kickedPlayerId = Long.parseLong(parts[2]);
        Room room = kickPlayer(roomId, userId, kickedPlayerId);
        if (room != null) {
            return createSuccessResponse(module, "KICK", gson.toJson(room));
        } else {
            return createErrorResponse(module, "KICK", "Du lieu khong hop le hoac khong the kick");
        }
    }

    // Các phương thức hỗ trợ
    private Long generateRoomId() {
        return System.currentTimeMillis() + new Random().nextInt(1000);
    }

    public List<Room> getAllRooms() {
        return roomDAO.getAllRooms();
    }

    public Room getRoomById(Long roomId) {
        return roomDAO.getRoomById(roomId);
    }

    // Thay đổi trạng thái phòng
    public Room changeStatus(Long roomId, boolean isPlaying) {
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

    public Room createRoom(Long ownerId) {
        List<Player> players = new ArrayList<>();
        Player player = new Player();
        player.setUserId(ownerId);
        players.add(player);

        Long roomId = generateRoomId();
        Room room = new Room(roomId, ownerId, 4, Instant.now(), "pending", null, players);
        roomDAO.createRoom(room);
        return room;
    }

    public Room joinRoom(Long roomId, Long userId) {
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
        System.out.println(
                "RoomController.joinRoom: SUCCESS - added userId=" + userId + ", now " + players.size() + " players");
        return room;
    }

    public Room editRoom(Long roomId, Integer maxPlayer) {
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

    public Room outRoom(Long roomId, Long userId) {
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

            if (players.isEmpty()) {
                roomDAO.deleteRoom(roomId);
            } else if (room.getOwnerId().equals(userId)) {
                room.setOwnerId(players.get(0).getUserId());
                roomDAO.updateRoom(roomId, room.getOwnerId(), null, null);
            }
        } else {
            System.out.println("RoomController.outRoom: player not found in room - userId=" + userId);
        }
        return room;
    }

    public Room kickPlayer(Long roomId, Long userId, Long kickedPlayerId) {
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
