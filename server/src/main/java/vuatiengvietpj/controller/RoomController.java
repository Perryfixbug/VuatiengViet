package vuatiengvietpj.controller;

import java.util.*;
import java.time.*;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.*;

import vuatiengvietpj.dao.RoomDAO;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Player;

public class RoomController extends ServerController {
    private RoomDAO roomDAO;
    private String module = "ROOM";
    private Gson gson;

    public RoomController(Socket clientSocket) throws IOException {
        super(clientSocket);
        this.roomDAO = new RoomDAO();
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    @Override
    protected Response process(Request request) throws IOException {
        String data = request.getData();
        return switch (request.getMaLenh()) {
            case "CREATE" -> handleCreate(data);
            case "JOIN" -> handleJoin(data);
            case "EDIT" -> handleEdit(data);
            case "OUT" -> handleOut(data);
            case "GETALL" -> handleGetAll();
            default -> createErrorResponse(module, request.getMaLenh(), "Hanh dong khong hop le");
        };
    }
    // Tạo phòng mới
    private Response handleCreate(String data) {
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
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "JOIN", "Du lieu khong hop le");
        }
        try {
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            boolean success = joinRoom(roomId, userId);
            if (success) {
                return createSuccessResponse(module, "JOIN", "Ban da tham gia phong");
            } else {
                return createErrorResponse(module, "JOIN", "Tham gia phong that bai");
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "JOIN", "Du lieu khong hop le: " + e.getMessage());
        }
    }
    // Chỉnh sửa phòng
    private Response handleEdit(String data) {
        String[] parts = data.split(",");
        if (parts.length < 5) {
            return createErrorResponse(module, "EDIT", "Du lieu khong hop le");
        }
        try {
            Long roomId = Long.parseLong(parts[0]);
            Long ownerId = parts[1].isEmpty() ? null : Long.parseLong(parts[1]);
            String status = parts[2].isEmpty() ? null : parts[2];
            Integer maxPlayer = parts[3].isEmpty() ? null : Integer.parseInt(parts[3]);
            Long challengePackId = parts[4].isEmpty() ? null : Long.parseLong(parts[4]);
            Room room = editRoom(roomId, ownerId, status, maxPlayer, challengePackId);
            if (room != null) {
                return createSuccessResponse(module, "EDIT", gson.toJson(room));
            } else {
                return createErrorResponse(module, "EDIT", "LLoi chinh sua phong");
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "EDIT", "Du lieu khong hop le: " + e.getMessage());
        }
    }
    // Rời phòng
    private Response handleOut(String data) {
        String[] parts = data.split(",");
        if (parts.length < 2) {
            return createErrorResponse(module, "OUT", "Du lieu khong hop le");
        }
        try {
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            outRoom(roomId, userId);
            return createSuccessResponse(module, "OUT", "Ban da roi khoi phong");
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

    public boolean joinRoom(Long roomId, Long userId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null || userId == null || room.getMaxPlayer() == 0 || room.getPlayers().size() >= room.getMaxPlayer()) {
            return false;
        }
        List<Player> players = room.getPlayers();
        for (Player p : players) {
            if (p.getUserId().equals(userId)) {
                return false;
            }
        }
        Player newPlayer = new Player();
        newPlayer.setUserId(userId);
        players.add(newPlayer);
        room.setPlayers(players);
        roomDAO.addPlayerToRoom(roomId, userId);
        return true;
    }

    public Room editRoom(Long roomId, Long ownerId, String status, Integer maxPlayer, Long challengePackId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null) return null;
        if (ownerId != null) room.setOwnerId(ownerId);
        if (status != null) room.setStatus(status);
        if (maxPlayer != null && maxPlayer > 8) {
            maxPlayer = 8;
        }
        room.setMaxPlayer(maxPlayer);
        roomDAO.updateRoom(roomId, room.getOwnerId(), room.getStatus(), room.getMaxPlayer());
        return room;
    }

    public void outRoom(Long roomId, Long userId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null || userId == null) return;
        List<Player> players = room.getPlayers();
        if (players == null) return;
        Player toRemove = null;
        for (Player p : players) {
            if (p.getUserId().equals(userId)) {
                toRemove = p;
                break;
            }
        }
        if (toRemove != null) {
            players.remove(toRemove);
            room.setPlayers(players);
            roomDAO.deletePlayerFromRoom(roomId, userId);
            if (players.isEmpty()) {
                roomDAO.deleteRoom(roomId);
            } else if (room.getOwnerId().equals(userId)) {
                room.setOwnerId(players.get(0).getUserId());
                roomDAO.updateRoom(roomId, room.getOwnerId(), null, null);
            }
        }
    }
}
