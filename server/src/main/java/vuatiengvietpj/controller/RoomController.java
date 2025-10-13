package vuatiengvietpj.controller;

import java.util.*;
import java.time.*;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;

import vuatiengvietpj.dao.*;
import vuatiengvietpj.model.*;
public class RoomController extends ServerController {
    private String action;
    private byte[] payload;
    private RoomDAO roomDAO;

    public RoomController(Socket clientSocket, String action, byte[] payload) throws IOException {
        super(clientSocket);
        this.action = action;
        this.payload = payload;
        roomDAO = new RoomDAO();
    }

    @Override
    protected void process() throws IOException {
        String requestData = new String(payload, StandardCharsets.UTF_8);

        System.out.println("Nhận yêu cầu Room: " + action + " | payload: " + requestData);

        if ("CREATE".equalsIgnoreCase(action)) {
            // Parse payload and create room
            Long ownerId = Long.parseLong(requestData);
            Room room = createRoom(ownerId);
            if (room != null) {
                sendBytes(("Room created: " + room.getId()).getBytes(StandardCharsets.UTF_8));
            } else {
                sendBytes("Failed to create room".getBytes(StandardCharsets.UTF_8));
            }
        } else if ("JOIN".equalsIgnoreCase(action)) {
            // Parse payload and join room
            String[] data = requestData.split(",");
            Long roomId = Long.parseLong(data[0]);
            Long userId = Long.parseLong(data[1]);
            boolean success = joinRoom(roomId, userId);
            if (success) {
                sendBytes("Joined room successfully".getBytes(StandardCharsets.UTF_8));
            } else {
                sendBytes("Failed to join room".getBytes(StandardCharsets.UTF_8));
            }
        } else if ("EDIT".equalsIgnoreCase(action)) {
            // Parse payload and edit room
            String[] data = requestData.split(",");
            Long roomId = Long.parseLong(data[0]);
            Long ownerId = data[1].isEmpty() ? null : Long.parseLong(data[1]);
            String status = data[2].isEmpty() ? null : data[2];
            Integer maxPlayer = data[3].isEmpty() ? null : Integer.parseInt(data[3]);
            Long challengePackId = data[4].isEmpty() ? null : Long.parseLong(data[4]);
            Room room = editRoom(roomId, ownerId, status, maxPlayer, challengePackId);
            if (room != null) {
                sendBytes(("Room edited: " + room.getId()).getBytes(StandardCharsets.UTF_8));
            } else {
                sendBytes("Failed to edit room".getBytes(StandardCharsets.UTF_8));
            }
        } else if ("OUT".equalsIgnoreCase(action)) {
            // Parse payload and leave room
            String[] data = requestData.split(",");
            Long roomId = Long.parseLong(data[0]);
            Long userId = Long.parseLong(data[1]);
            outRoom(roomId, userId);
            sendBytes("Left room successfully".getBytes(StandardCharsets.UTF_8));
        } else if ("GETALL".equalsIgnoreCase(action)) {
            // Get all rooms
            List<Room> rooms = getAllRooms();
            Gson gson = new Gson();
            sendBytes(gson.toJson(rooms).getBytes(StandardCharsets.UTF_8));
        } else {
            sendBytes("Room: action không hợp lệ".getBytes(StandardCharsets.UTF_8));
        }

    }

    
    // Sinh ngẫu nhiên 1 mã challenge pack từ DB
    public ChallengePack generateChallengePack() {
        ChallengePackDAO cpDAO = new ChallengePackDAO();
        int totalCP = cpDAO.getNumberCP();
        if (totalCP == 0) return null;
        Random rand = new Random();
        return cpDAO.getChallengePackById((long) (rand.nextInt(totalCP) + 1));
    }
    //Tạo 1 mã phòng mới theo thời gian hiện tại
    private Long generateRoomId() {
        return (long) System.currentTimeMillis();
    }
    // Danh sách phòng
    public List<Room> getAllRooms() {
        return roomDAO.getAllRooms();
    }
    // Thông tin phòng theo ID
    public Room getRoomById(Long roomId) {
        return roomDAO.getRoomById(roomId);
    }
    // Tạo phòng mới
    public Room createRoom(Long ownerId) {
        List<Player> players = new ArrayList<>();
        Player player = new Player();
        player.setUserId(ownerId);
        players.add(player);

        // Tạo ID thủ công bằng generateRoomId
        Long roomId = generateRoomId();
        Room room = new Room(roomId, ownerId, 4, Instant.now(), "pending", null, players);
        roomDAO.createRoom(room);
        return room;
    }
    // Vào phòng
    public boolean joinRoom(Long roomId, Long userId) {
        Room room = roomDAO.getRoomById(roomId);
        // Nếu phòng đầy thì không cho vào
        if (room == null || userId == null || room.getPlayers().size() >= room.getMaxPlayer()) return false;
        List<Player> players = room.getPlayers();
        for (Player p : players) {
            if (p.getUserId().equals(userId)) return false; 
        }
        Player newPlayer = new Player();
        newPlayer.setUserId(userId);
        players.add(newPlayer);
        room.setPlayers(players);
        roomDAO.addPlayerToRoom(roomId, userId);
        return true;
    }
    // Sửa thông tin phòng
    public Room editRoom(Long roomId, Long ownerId, String status, Integer maxPlayer, Long challengePackId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null) return null;
        if (ownerId != null) room.setOwnerId(ownerId);
        if (status != null) room.setStatus(status);
        if (maxPlayer != null) room.setMaxPlayer(maxPlayer);
        if (maxPlayer > 8) maxPlayer = 8;
        roomDAO.updateRoom(roomId, room.getOwnerId(), room.getStatus(), room.getMaxPlayer());
        return room;
    }
    // Rời phòng
    public void outRoom(Long roomId, Long userId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null || userId == null) return;
        List<Player> players = room.getPlayers();
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
            // Nếu phòng không còn người chơi thì xóa phòng
            if (players.isEmpty()) {
                roomDAO.deleteRoom(roomId);
            // Nếu người rời là chủ phòng thì đổi chủ phòng mới
            } else if (room.getOwnerId().equals(userId)) {
                room.setOwnerId(players.get(0).getUserId());
                roomDAO.updateRoom(roomId, room.getOwnerId(), null, null);
            }
        }
        return;
    }
}
