package vuatiengvietpj.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Response;
import java.util.List;

public class RoomController extends ClientController implements AutoCloseable {
    private String module = "ROOM";
    private Gson gson;

    public RoomController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(java.time.Instant.class, new com.google.gson.JsonSerializer<java.time.Instant>() {
                    @Override
                    public com.google.gson.JsonElement serialize(java.time.Instant src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
                        return new com.google.gson.JsonPrimitive(src.toString());
                    }
                })
                .registerTypeAdapter(java.time.Instant.class, new com.google.gson.JsonDeserializer<java.time.Instant>() {
                    @Override
                    public java.time.Instant deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                        return java.time.Instant.parse(json.getAsString());
                    }
                })
                .create();
    }
    
    // Gửi yêu cầu tạo phòng lên server
    public Response createRoom(Integer ownerId) {
        try {
            return sendAndReceive(module, "CREATE", ownerId.toString());
        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu tham gia phòng lên server
    public Response joinRoom(Integer roomId, Integer userId) {
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
    public Response editRoom(Integer roomId, Integer maxPlayer) {
        try {
            String data = roomId + "," + maxPlayer;
            System.out.println("RoomController.editRoom: sending EDIT for room=" + roomId + ", max=" + maxPlayer);
            Response r = sendAndReceive(module, "EDIT", data);
            System.out.println("RoomController.editRoom: response=" + (r == null ? "null" : r.getData()));
            return r;
        } catch (Exception e) {
            System.err.println("Error editing room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    // Gửi yêu cầu rời phòng lên server
    public Response outRoom(Integer roomId, Integer userId) {
        try {
            String data = roomId + "," + userId;
            return sendAndReceive(module, "OUT", data);
        } catch (Exception e) {
            System.err.println("Error leaving room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Gửi yêu cầu lấy danh sách phòng lên server
    public Response getAllRooms() {
        try {
            Response response = sendAndReceive(module, "GETALL", "");
            if (response != null && response.isSuccess()) {
                List<Room> rooms = parseRooms(response.getData());
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
    public Room getRoomById(Integer roomId) {
        try {
            Response response = sendAndReceive(module, "GETBYID", roomId.toString());
            if (response != null && response.isSuccess()) {
                // server may return a single Room or a JSON array with one element; try to parse either
                Room room = parseRoom(response.getData());
                return room;
            }
        } catch (Exception e) {
            System.err.println("Error fetching room by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Helper to parse list of rooms using configured gson (handles Instant)
    public List<Room> parseRooms(String json) {
        try {
            return gson.fromJson(json, new com.google.gson.reflect.TypeToken<List<Room>>() {}.getType());
        } catch (Exception e) {
            // fallback: try parse single room into list
            try {
                Room r = gson.fromJson(json, Room.class);
                return java.util.Collections.singletonList(r);
            } catch (Exception ex) {
                System.err.println("Error parsing rooms: " + ex.getMessage());
                return java.util.Collections.emptyList();
            }
        }
    }

    // Helper to parse a single room; also handles when server returns an array with one element
    public Room parseRoom(String json) {
        try {
            return gson.fromJson(json, Room.class);
        } catch (Exception e) {
            try {
                List<Room> list = gson.fromJson(json, new com.google.gson.reflect.TypeToken<List<Room>>() {}.getType());
                if (list != null && !list.isEmpty()) return list.get(0);
            } catch (Exception ex) {
                System.err.println("Error parsing room: " + ex.getMessage());
            }
        }
        return null;
    }
    // Gửi yêu cầu làm mới trạng thái phòng lên server
    public Response refreshRoom(Integer roomId, boolean isPlaying) {
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
    public Response kickPlayer(Integer roomId, Integer userId, Integer kickedPlayerId) {
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
    public void checkAlive(Integer roomId, List<Integer> playerIds) {
        for (Integer playerId : playerIds) {
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
