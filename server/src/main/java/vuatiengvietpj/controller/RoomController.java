package vuatiengvietpj.controller;

import java.util.*;
import java.time.*;
import vuatiengvietpj.DAO.*;
import vuatiengvietpj.model.*;
public class RoomController {
    private RoomDAO roomDAO;
    // Sinh ngẫu nhiên 1 mã challenge pack từ DB
    private ChallengePack generateChallengePack() {
        ChallengePackDAO cpDAO = new ChallengePackDAO();
        int totalCP = cpDAO.getNumberCP();
        if (totalCP == 0) return null;
        Random rand = new Random();
        return cpDAO.getChallengePackById((long) (rand.nextInt(totalCP) + 1));
    }
    //Tạo 1 phòng mới theo thời gian hiện tại
    private Long generateRoomId() {
        return (long) System.currentTimeMillis();
    }

    public RoomController() {
        roomDAO = new RoomDAO();
    }
    // Danh sách phòng
    public List<vuatiengvietpj.model.Room> getAllRooms() {
        return roomDAO.getAllRooms();
    }
    // Thông tin phòng theo ID
    public vuatiengvietpj.model.Room getRoomById(Long roomId) {
        return roomDAO.getRoomById(roomId);
    }
    // Tạo phòng mới
    public Room createRoom(Long ownerId, Long challengePackId) {
        ChallengePack cp = null;
        if (challengePackId == null || ownerId == null) {
            return null;
        }
        cp = new ChallengePackDAO().getChallengePackById(challengePackId);
        List<Player> players = new ArrayList<>();
        Player player = new Player();
        player.setUserId(ownerId);
        players.add(player);
        Long roomId = generateRoomId();
        Room room = new Room(roomId, ownerId, 4, players, Instant.now(), cp, "pending");
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
        if (maxPlayer > 8) return null; // Giới hạn số người chơi tối đa là 8
        ChallengePack newCP = generateChallengePack();
        roomDAO.updateRoom(roomId, room.getOwnerId(), room.getStatus(), room.getMaxPlayer(), newCP.getId());
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
                roomDAO.updateRoom(roomId, room.getOwnerId(), null, null, null);
            }
        }
        return;
    }
}
