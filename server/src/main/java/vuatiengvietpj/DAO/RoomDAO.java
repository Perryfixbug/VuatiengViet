package vuatiengvietpj.DAO;

import vuatiengvietpj.model.*;
import java.sql.*;
import java.util.*;

public class RoomDAO extends DAO {
    public RoomDAO() {
        getDBconnection();
    }
    
    // Lấy danh sách người chơi theo mã phòng
    private List<Player> getPlayersByRoomId(Long roomId){
        List<Player> players = new ArrayList<>();
        String sql ="SELECT * FROM player WHERE roomId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Player player = new Player();
                    player.setUserId(rs.getLong("user_id"));
                    player.setRoomId(rs.getLong("room_id"));
                    player.setScore(rs.getInt("score"));
                    players.add(player);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
    // Lấy danh sách phòng
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM Room";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room room = new Room();
                room.setId(rs.getLong("id"));
                room.setOwnerId(rs.getLong("ownerId"));
                room.setMaxPlayer(rs.getInt("maxPlayer"));
                room.setCreateAt(rs.getTimestamp("createAt").toInstant());
                room.setStatus(rs.getString("status"));

                Long cpId = rs.getLong("challengePackId");
                if (cpId != null) {
                    ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                    room.setCp(cp);
                }

                List<Player> players = getPlayersByRoomId(room.getId());
                room.setPlayers(players);

                rooms.add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
    // Lấy thông tin phòng theo ID
    public Room getRoomById(Long roomId) {
        Room room = new Room();
        String sql = "SELECT * FROM room WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    room = new Room();
                    room.setId(rs.getLong("id"));
                    room.setOwnerId(rs.getLong("owner_id"));
                    room.setMaxPlayer(rs.getInt("max_player"));
                    room.setCreateAt(rs.getTimestamp("create_at").toInstant());
                    room.setStatus(rs.getString("status"));

                    Long cpId = rs.getLong("challenge_pack_id");
                    if (cpId != null) {
                        ChallengePack cp = new ChallengePackDAO().getChallengePackById(cpId);
                        room.setCp(cp);
                    }

                    List<Player> players = getPlayersByRoomId(room.getId());
                    room.setPlayers(players);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return room;
    }
    // Lưu 1 phòng mới vào DB
    public void createRoom(Room room) {

        String sql = "INSERT INTO room (id, ownerId, maxPlayer, createAt, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, room.getId());
            ps.setLong(2, room.getOwnerId());
            ps.setInt(3, room.getMaxPlayer());
            ps.setTimestamp(4, Timestamp.from(room.getCreateAt()));
            ps.setString(5, room.getStatus());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating room failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    room.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating room failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Cập nhập thông tin phòng
    public void updateRoom(Long roomId, Long ownerId, String status, Integer maxPlayer, Long challengePackId) {
        StringBuilder sql = new StringBuilder("UPDATE room SET ");
        List<Object> params = new ArrayList<>();
        if (ownerId != null) {
            sql.append("owner_id = ?");
            params.add(ownerId);
        }
        if (challengePackId != null) {
            sql.append("challenge_pack_id = ?");
            params.add(challengePackId);
        }
        if (status != null) {
            sql.append("status = ?");
            params.add(status);
        }
        if (maxPlayer != null) {
            if (!params.isEmpty()) sql.append(", ");
            sql.append("max_player = ?");
            params.add(maxPlayer);
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
    public void deletePlayerFromRoom(Long roomId, Long userId) {
        String sql = "DELETE FROM player WHERE user_id = ? AND room_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void addPlayerToRoom(Long roomId, Long userId) {
        String sql = "INSERT INTO player (user_id, room_id, score) VALUES (?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Xóa phòng theo ID
    public void deleteRoom(Long roomId) {
        String sql = "DELETE FROM room WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
