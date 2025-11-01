package vuatiengvietpj.dao;

import vuatiengvietpj.model.*;
import java.sql.*;
import java.util.*;

public class RoomDAO extends DAO {
    public RoomDAO() {
        getDBconnection();
    }
    
    // Lấy danh sách phòng
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT r.*, p.userId, p.score FROM room r LEFT JOIN player p ON r.id = p.roomId";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<Long, Room> roomMap = new HashMap<>();
            while (rs.next()) {
                Long roomId = rs.getLong("id");
                Room room = roomMap.get(roomId);
                if (room == null) {
                    room = new Room();
                    room.setId(roomId);
                    room.setOwnerId(rs.getLong("ownerId"));
                    room.setMaxPlayer(rs.getInt("maxPlayer"));
                    Timestamp ts = rs.getTimestamp("createdAt");
                    if (ts != null) room.setCreateAt(ts.toInstant());
                    room.setStatus(rs.getString("status"));
                    Object cpObj = rs.getObject("challengePackId");
                    if (cpObj != null) {
                        Long cpId = rs.getLong("challengePackId");
                        ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                        room.setCp(cp);
                    }
                    room.setPlayers(new ArrayList<>());
                    roomMap.put(roomId, room);
                    rooms.add(room);
                }
                Object userObj = rs.getObject("userId");
                if (userObj != null) {
                    Long userId = rs.getLong("userId");
                    Player player = new Player();
                    player.setUserId(userId);
                    player.setRoomId(roomId);
                    player.setScore(rs.getInt("score"));
                    room.getPlayers().add(player);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    // Lấy thông tin phòng theo ID
    public Room getRoomById(Long roomId) {
        Room room = null;
        String sql = "SELECT r.*, p.userId, p.score FROM room r LEFT JOIN player p ON r.id = p.roomId WHERE r.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (room == null) {
                        room = new Room();
                        room.setId(rs.getLong("id"));
                        room.setOwnerId(rs.getLong("ownerId"));
                        room.setMaxPlayer(rs.getInt("maxPlayer"));
                        Timestamp ts = rs.getTimestamp("createdAt");
                        if (ts != null) room.setCreateAt(ts.toInstant());
                        room.setStatus(rs.getString("status"));
                        Object cpObj = rs.getObject("challengePackId");
                        if (cpObj != null) {
                            Long cpId = rs.getLong("challengePackId");
                            ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                            room.setCp(cp);
                        }
                        room.setPlayers(new ArrayList<>());
                    }
                    Object userObj = rs.getObject("userId");
                    if (userObj != null) {
                        Long userId = rs.getLong("userId");
                        Player player = new Player();
                        player.setUserId(userId);
                        player.setRoomId(roomId);
                        player.setScore(rs.getInt("score"));
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
            ps.setLong(1, room.getId());
            ps.setLong(2, room.getOwnerId());
            ps.setInt(3, room.getMaxPlayer());
            ps.setTimestamp(4, Timestamp.from(room.getCreateAt()));
            ps.setString(5, room.getStatus());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating room failed, no rows affected.");
            }
            
            // Insert owner into player table so DB state reflects in-memory room
            if (room.getPlayers() != null) {
                for (Player p : room.getPlayers()) {
                    if (p != null && p.getUserId() != null) {
                        addPlayerToRoom(room.getId(), p.getUserId());
                    }
                }
            } else {
                // if no players provided, still add owner
                if (room.getOwnerId() != null) {
                    addPlayerToRoom(room.getId(), room.getOwnerId());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cập nhập thông tin phòng
    public void updateRoom(Long roomId, Long ownerId, String status, Integer maxPlayer) {
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
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Tạo một challenge pack cho phòng
    public void addChallengePackToRoom(Long roomId, Long cpId) {
        String sql = "UPDATE room SET challengePackId = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cpId);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Xóa người chơi khỏi phòng
    public void deletePlayerFromRoom(Long roomId, Long userId) {
        String sql = "DELETE FROM player WHERE userId = ? AND roomId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Thêm người chơi vào phòng
    public void addPlayerToRoom(Long roomId, Long userId) {
        String checkSql = "SELECT COUNT(*) FROM player WHERE userId = ? AND roomId = ?";
        String insertSql = "INSERT INTO player (userId, roomId, score) VALUES (?, ?, 0)";
        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setLong(1, userId);
            checkPs.setLong(2, roomId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Player already exists in the room.");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
            insertPs.setLong(1, userId);
            insertPs.setLong(2, roomId);
            insertPs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Xóa phòng theo ID
    public void deleteRoom(Long roomId) {
        String deletePlayersSql = "DELETE FROM player WHERE roomId = ?";
        String deleteRoomSql = "DELETE FROM room WHERE id = ?";
        try (PreparedStatement deletePlayersPs = con.prepareStatement(deletePlayersSql);
             PreparedStatement deleteRoomPs = con.prepareStatement(deleteRoomSql)) {
            deletePlayersPs.setLong(1, roomId);
            deletePlayersPs.executeUpdate();

            deleteRoomPs.setLong(1, roomId);
            deleteRoomPs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}