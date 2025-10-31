package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Response;

public class RoomController extends ClientController implements AutoCloseable {
    private String module = "ROOM";
    private Gson gson;

    public RoomController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new Gson();
    }
    
    // Gửi yêu cầu tạo phòng lên server
    public Response createRoom(Long ownerId) {
        try {
            return sendAndReceive(module, "CREATE", ownerId.toString());
        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu tham gia phòng lên server
    public Response joinRoom(Long roomId, Long userId) {
        try {
            String data = roomId + "," + userId;
            return sendAndReceive(module, "JOIN", data);
        } catch (Exception e) {
            System.err.println("Error joining room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu chỉnh sửa phòng lên server
    public Response editRoom(Long roomId, Integer maxPlayer) {
        try {
            String data = roomId + "," + maxPlayer;
            return sendAndReceive(module, "EDIT", data);
        } catch (Exception e) {
            System.err.println("Error editing room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu rời phòng lên server
    public Response outRoom(Long roomId, Long userId) {
        try {
            String data = roomId + "," + userId;
            return sendAndReceive(module, "OUT", data);
        } catch (Exception e) {
            System.err.println("Error leaving room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Gửi yêu cầu rời phòng lên server mà server tự lấy user từ session (chỉ gửi roomId)
    public Response outRoom(Long roomId) {
        try {
            String data = roomId.toString();
            return sendAndReceive(module, "OUT", data);
        } catch (Exception e) {
            System.err.println("Error leaving room (session): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu lấy danh sách phòng lên server
    public Response getAllRooms() {
        try {
            Response response = sendAndReceive(module, "GETALL", "");
            if (response != null && response.isSuccess()) {
                List<Room> rooms = gson.fromJson(response.getData(), new TypeToken<List<Room>>() {}.getType());
                System.out.println("Rooms: " + rooms);
            }
            return response;
        } catch (Exception e) {
            System.err.println("Error fetching all rooms: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Lấy phòng theo id (trả về Room hoặc null)
    public Room getRoomById(Long roomId) {
        try {
            Response response = sendAndReceive(module, "GETBYID", roomId.toString());
            if (response != null && response.isSuccess()) {
                Room room = gson.fromJson(response.getData(), Room.class);
                return room;
            }
        } catch (Exception e) {
            System.err.println("Error fetching room by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu làm mới trạng thái phòng lên server
    public Response refreshRoom(Long roomId, boolean isPlaying) {
        try {
            String data = roomId + "," + isPlaying;
            return sendAndReceive(module, "REFRESH", data);
        } catch (Exception e) {
            System.err.println("Error refreshing room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu đá người chơi khỏi phòng lên server
    public Response kickPlayer(Long roomId, Long userId, Long kickedPlayerId) {
        try {
            String data = roomId + "," + userId + "," + kickedPlayerId;
            return sendAndReceive(module, "KICK", data);
        } catch (Exception e) {
            System.err.println("Error kicking player: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Kiểm tra trạng thái alive của từng player trong phòng
    public void checkAlive(Long roomId, List<Long> playerIds) {
        for (Long playerId : playerIds) {
            try {
                Response response = sendAndReceive("USER", "ALIVE", playerId.toString());
                if (response == null || !response.isSuccess()) {
                    System.out.println("Player " + playerId + " khong phan hoi...");
                    outRoom(roomId, playerId);
                }
            } catch (Exception e) {
                System.err.println("Errorr check alive player " + playerId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Expose a public disconnect so callers can close the underlying socket
    public void disconnect() {
        close();
    }

    @Override
    public void close() {
        // delegate to existing close helper in ClientController
        try {
            super.close();
        } catch (Exception ignored) {
        }
    }
}
