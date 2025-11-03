package vuatiengvietpj.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vuatiengvietpj.model.ChallengePack;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.Room;

public class RoomDAO extends DAO {
    public RoomDAO() {
        getDBconnection();
    }
    
    // Lấy danh sách phòng
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT r.*, p.userId, p.score, u.fullName as playerName, o.fullName as ownerName " +
                     "FROM room r " +
                     "LEFT JOIN player p ON r.id = p.roomId " +
                     "LEFT JOIN user u ON p.userId = u.id " +
                     "LEFT JOIN user o ON r.ownerId = o.id";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, Room> roomMap = new HashMap<>();
            while (rs.next()) {
                Integer roomId = rs.getInt("id");
                Room room = roomMap.get(roomId);
                if (room == null) {
                    room = new Room();
                    room.setId(roomId);
                    room.setOwnerId(rs.getInt("ownerId"));
                    room.setOwnerName(rs.getString("ownerName"));
                    room.setMaxPlayer(rs.getInt("maxPlayer"));
                    Timestamp ts = rs.getTimestamp("createdAt");
                    if (ts != null) room.setCreateAt(ts.toInstant());
                    room.setStatus(rs.getString("status"));
                    Object cpObj = rs.getObject("challengePackId");
                    if (cpObj != null) {
                        Integer cpId = rs.getInt("challengePackId");
                        ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                        room.setCp(cp);
                    }
                    room.setPlayers(new ArrayList<>());
                    roomMap.put(roomId, room);
                    rooms.add(room);
                }
                Object userObj = rs.getObject("userId");
                if (userObj != null) {
                    Integer userId = rs.getInt("userId");
                    Player player = new Player();
                    player.setUserId(userId);
                    player.setRoomId(roomId);
                    player.setScore(rs.getInt("score"));
                    String playerName = rs.getString("playerName");
                    player.setName(playerName);
                    System.out.println("RoomDAO.getAllRooms - Player: userId=" + userId + ", name=" + playerName);
                    room.getPlayers().add(player);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    // Lấy thông tin phòng theo ID
    public Room getRoomById(Integer roomId) {
        Room room = null;
        String sql = "SELECT r.*, p.userId, p.score, u.fullName as playerName, o.fullName as ownerName " +
                     "FROM room r " +
                     "LEFT JOIN player p ON r.id = p.roomId " +
                     "LEFT JOIN user u ON p.userId = u.id " +
                     "LEFT JOIN user o ON r.ownerId = o.id " +
                     "WHERE r.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (room == null) {
                        room = new Room();
                        room.setId(rs.getInt("id"));
                        room.setOwnerId(rs.getInt("ownerId"));
                        room.setOwnerName(rs.getString("ownerName"));
                        room.setMaxPlayer(rs.getInt("maxPlayer"));
                        Timestamp ts = rs.getTimestamp("createdAt");
                        if (ts != null) room.setCreateAt(ts.toInstant());
                        room.setStatus(rs.getString("status"));
                        Object cpObj = rs.getObject("challengePackId");
                        if (cpObj != null) {
                            Integer cpId = rs.getInt("challengePackId");
                            ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                            room.setCp(cp);
                        }
                        room.setPlayers(new ArrayList<>());
                    }
                    Object userObj = rs.getObject("userId");
                    if (userObj != null) {
                        Integer userId = rs.getInt("userId");
                        Player player = new Player();
                        player.setUserId(userId);
                        player.setRoomId(roomId);
                        player.setScore(rs.getInt("score"));
                        String playerName = rs.getString("playerName");
                        player.setName(playerName);
                        System.out.println("RoomDAO.getRoomById - Player: userId=" + userId + ", name=" + playerName);
                        room.getPlayers().add(player);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return room;
    }
    
    // Lưu 1 phòng mới vào DB
    public void createRoom(Room room) {
        String sql = "INSERT INTO room (id, ownerId, maxPlayer, createdAt, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, room.getId());
            ps.setInt(2, room.getOwnerId());
            ps.setInt(3, room.getMaxPlayer());
            ps.setTimestamp(4, Timestamp.from(room.getCreateAt()));
            ps.setString(5, room.getStatus());

            Integer affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating room failed, no rows affected.");
            }
            
            // Insert players into player table so DB state reflects in-memory room
            if (room.getPlayers() != null && !room.getPlayers().isEmpty()) {
                for (Player p : room.getPlayers()) {
                    if (p != null && p.getUserId() != 0) {
                        // Preserve player's existing score when inserting
                        addPlayerToRoomWithScore(room.getId(), p.getUserId(), p.getScore() != null ? p.getScore() : 0);
                    }
                }
            } else {
                // if no players provided, still add owner with default score 0
                if (room.getOwnerId() != 0) {
                    addPlayerToRoomWithScore(room.getId(), room.getOwnerId(), 0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cập nhập thông tin phòng
    public void updateRoom(Integer roomId, Integer ownerId, String status, Integer maxPlayer) {
        StringBuilder sql = new StringBuilder("UPDATE room SET ");
        List<Object> params = new ArrayList<>();
        boolean firstField = true;

        if (ownerId != null) {
            if (!firstField) sql.append(", ");
            sql.append("ownerId = ?");
            params.add(ownerId);
            firstField = false;
        }
        if (status != null) {
            if (!firstField) sql.append(", ");
            sql.append("status = ?");
            params.add(status);
            firstField = false;
        }
        if (maxPlayer != null) {
            if (!firstField) sql.append(", ");
            sql.append("maxPlayer = ?");
            params.add(maxPlayer);
            firstField = false;
        }

        if (params.isEmpty()) {
            System.out.println("No fields to update.");
            return;
        }

        sql.append(" WHERE id = ?");
        params.add(roomId);

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (Integer i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Tạo một challenge pack cho phòng
    public void addChallengePackToRoom(Integer roomId, Integer cpId) {
        String sql = "UPDATE room SET challengePackId = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpId);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Xóa người chơi khỏi phòng
    public void deletePlayerFromRoom(Integer roomId, Integer userId) {
        String sql = "DELETE FROM player WHERE userId = ? AND roomId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Thêm người chơi vào phòng
    public void addPlayerToRoom(Integer roomId, Integer userId) {
        String checkSql = "SELECT score FROM player WHERE userId = ? AND roomId = ?";
        String insertSql = "INSERT INTO player (userId, roomId, score) VALUES (?, ?, ?)";
        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, roomId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    // Player đã tồn tại trong phòng - GIỮ NGUYÊN điểm (không làm gì)
                    System.out.println("Player already exists in the room with score: " + rs.getInt("score"));
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Player chưa tồn tại - thêm mới với score = 0
        try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
            insertPs.setInt(1, userId);
            insertPs.setInt(2, roomId);
            insertPs.setInt(3, 0); // Score mặc định = 0 cho player mới
            insertPs.executeUpdate();
            System.out.println("Added new player to room with score = 0");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Thêm người chơi với score chỉ định (dùng khi tạo phòng và cần giữ điểm hiện tại)
    public void addPlayerToRoomWithScore(Integer roomId, Integer userId, Integer score) {
        String checkSql = "SELECT score FROM player WHERE userId = ? AND roomId = ?";
        String insertSql = "INSERT INTO player (userId, roomId, score) VALUES (?, ?, ?)";
        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, roomId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    // Player đã tồn tại trong phòng - GIỮ NGUYÊN điểm (không làm gì)
                    System.out.println("Player already exists in the room with score: " + rs.getInt("score"));
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Player chưa tồn tại - thêm mới với score truyền vào
        try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
            insertPs.setInt(1, userId);
            insertPs.setInt(2, roomId);
            insertPs.setInt(3, score != null ? score : 0);
            insertPs.executeUpdate();
            System.out.println("Added new player to room with score = " + (score != null ? score : 0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Cập nhật điểm của player trong room
    public void updatePlayerScore(Integer roomId, Integer userId, Integer newScore) {
        // Lấy score hiện tại và cộng thêm
        String selectSql = "SELECT score FROM player WHERE userId = ? AND roomId = ?";
        String updateSql = "UPDATE player SET score = ? WHERE userId = ? AND roomId = ?";
        
        try (PreparedStatement selectPs = con.prepareStatement(selectSql)) {
            selectPs.setInt(1, userId);
            selectPs.setInt(2, roomId);
            try (ResultSet rs = selectPs.executeQuery()) {
                Integer currentScore = 0;
                if (rs.next()) {
                    currentScore = rs.getInt("score");
                }
                
                // Tính điểm mới (cộng thêm điểm mới vào điểm hiện tại)
                Integer updatedScore = currentScore + newScore;
                
                // Cập nhật điểm
                try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                    updatePs.setInt(1, updatedScore);
                    updatePs.setInt(2, userId);
                    updatePs.setInt(3, roomId);
                    updatePs.executeUpdate();
                    System.out.println("RoomDAO.updatePlayerScore: userId=" + userId + ", roomId=" + roomId + 
                                     ", oldScore=" + currentScore + ", addedScore=" + newScore + ", newScore=" + updatedScore);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Đặt điểm tuyệt đối cho player trong room (dùng để reset khi start game)
    public void setPlayerScore(Integer roomId, Integer userId, Integer score) {
        String updateSql = "UPDATE player SET score = ? WHERE userId = ? AND roomId = ?";
        try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
            updatePs.setInt(1, score != null ? score : 0);
            updatePs.setInt(2, userId);
            updatePs.setInt(3, roomId);
            int affected = updatePs.executeUpdate();
            if (affected > 0) {
                System.out.println("RoomDAO.setPlayerScore: Set score=" + (score != null ? score : 0) + " for userId=" + userId + ", roomId=" + roomId);
            } else {
                System.out.println("RoomDAO.setPlayerScore: No rows updated for userId=" + userId + ", roomId=" + roomId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Xóa phòng theo ID
    public void deleteRoom(Integer roomId) {
        String deletePlayersSql = "DELETE FROM player WHERE roomId = ?";
        String deleteRoomSql = "DELETE FROM room WHERE id = ?";
        try (PreparedStatement deletePlayersPs = con.prepareStatement(deletePlayersSql);
             PreparedStatement deleteRoomPs = con.prepareStatement(deleteRoomSql)) {
            deletePlayersPs.setInt(1, roomId);
            deletePlayersPs.executeUpdate();

            deleteRoomPs.setInt(1, roomId);
            deleteRoomPs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}