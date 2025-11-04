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

import vuatiengvietpj.dao.ChallengePackDAO;
import vuatiengvietpj.dao.DictionaryDAO;
import vuatiengvietpj.dao.RoomDAO;
import vuatiengvietpj.model.ChallengePack;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.ScoreBoard;

public class GameController extends ServerController {
    private String module = "GAME";
    private Gson gson;
    private RoomDAO roomDAO;
    private ChallengePackDAO challengePackDAO;
    private DictionaryDAO dictionaryDAO;

    public GameController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.roomDAO = new RoomDAO();
        this.challengePackDAO = new ChallengePackDAO();
        this.dictionaryDAO = new DictionaryDAO();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    @Override
    public Response process(Request request) throws IOException {
        String data = request.getData();
        return switch (request.getMaLenh()) {
            case "START" -> handleStart(data);
            case "SUBMIT" -> handleSubmit(data);
            case "UPDATE" -> handleUpdate(data);
            case "END" -> handleEnd(data);
            default -> createErrorResponse(module, request.getMaLenh(), "Hành động không hợp lệ");
        };
    }

    // Xử lý bắt đầu game
    private Response handleStart(String data) {
        // data format: "roomId,userId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "START", "Dữ liệu không hợp lệ");
        }
        try {
            String[] parts = data.split(",");
            if (parts.length < 2) {
                return createErrorResponse(module, "START", "Dữ liệu không hợp lệ");
            }
            
            Integer  roomId = Integer .parseInt(parts[0]);
            Integer  userId = Integer .parseInt(parts[1]);
            
            System.out.println("GameController.START: roomId=" + roomId + ", userId=" + userId);
            
            // Lấy room từ database
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                System.err.println("GameController.START: Room " + roomId + " NOT FOUND in DB!");
                return createErrorResponse(module, "START", "Phòng không tồn tại");
            }
            
            System.out.println("GameController.START: Found room " + roomId + ", status=" + room.getStatus() + 
                             ", owner=" + room.getOwnerId() + ", players=" + room.getPlayers().size());
            
            // Kiểm tra người start có phải owner của phòng không
            if (!java.util.Objects.equals(room.getOwnerId(), userId)) {
                return createErrorResponse(module, "START", "Chỉ chủ phòng mới được bắt đầu game");
            }
            
            // Kiểm tra room đang ở trạng thái pending
            if (!"pending".equals(room.getStatus())) {
                return createErrorResponse(module, "START", "Phòng không ở trạng thái cho phép bắt đầu");
            }
            
            // Lấy danh sách tất cả challenge pack IDs
            List<Integer > challengePackIds = challengePackDAO.getAllChallengePackIds();
            if (challengePackIds.isEmpty()) {
                return createErrorResponse(module, "START", "Không có challenge pack nào trong hệ thống");
            }
            
            // Chọn ngẫu nhiên một challenge pack ID
            Random random = new Random();
            Integer  randomCpId = challengePackIds.get(random.nextInt(challengePackIds.size()));
            
            // Gán challenge pack vào room
            roomDAO.addChallengePackToRoom(roomId, randomCpId);
            
            // RESET điểm về 0 cho tất cả players khi BẮT ĐẦU game
            Room tempRoom = roomDAO.getRoomById(roomId);
            if (tempRoom != null && tempRoom.getPlayers() != null) {
                for (Player p : tempRoom.getPlayers()) {
                    // Use setPlayerScore to set absolute value 0 (updatePlayerScore adds)
                    roomDAO.setPlayerScore(roomId, p.getUserId(), 0);
                }
                System.out.println("GameController.START: Reset all players' scores to 0 in room " + roomId);
            }
            
            // Chuyển room thành trạng thái playing
            roomDAO.updateRoom(roomId, null, "playing", null);
            
            // Lấy lại room với challenge pack đã được gán
            room = roomDAO.getRoomById(roomId);
            
            // Broadcast room update để tất cả client biết game đã bắt đầu
            if (room != null) {
                vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
                System.out.println("GameController.START: Broadcasted game start to all members - roomId=" + roomId);
            }
            
            // Trả về room với challenge pack để gửi cho tất cả client
            return createSuccessResponse(module, "START", gson.toJson(room));
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "START", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "START", "Lỗi khi bắt đầu game: " + e.getMessage());
        }
    }

    // Xử lý submit câu trả lời
    private Response handleSubmit(String data) {
        // data format: "roomId,userId,answer"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "SUBMIT", "Du lieu khong hop le");
        }
        try {
            String[] parts = data.split(",");
            if (parts.length < 3) {
                return createErrorResponse(module, "SUBMIT", "Du lieu khong hop le");
            }
            
            Integer  roomId = Integer.parseInt(parts[0]);
            Integer  userId = Integer.parseInt(parts[1]);
            String answer = parts[2]; // Chưa trim để check empty
            
            // Kiểm tra answer không rỗng
            if (answer == null || answer.trim().isEmpty()) {
                return createErrorResponse(module, "SUBMIT", "Đáp án không được rỗng");
            }
            
            answer = answer.trim(); // Trim whitespace sau khi check
            
            // Lấy room từ database
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "SUBMIT", "Phòng không tồn tại");
            }
            
            // Kiểm tra user có trong room không
            if (!isUserInRoom(room, userId)) {
                return createErrorResponse(module, "SUBMIT", "Bạn không trong phòng này");
            }
            
            // Kiểm tra room đang ở trạng thái playing
            if (!"playing".equals(room.getStatus())) {
                return createErrorResponse(module, "SUBMIT", "Phòng không ở trạng thái cho phép submit");
            }
            
            // Kiểm tra ChallengePack có tồn tại không
            if (room.getCp() == null || room.getCp().getId() == 0L) {
                return createErrorResponse(module, "SUBMIT", "Phòng chưa có challenge pack");
            }
            
            Integer  challengePackId = room.getCp().getId();
            
            // Lấy danh sách đáp án của ChallengePack
            List<String> validAnswers = challengePackDAO.getAnswersByChallengePackId(challengePackId);
            if (validAnswers.isEmpty()) {
                return createErrorResponse(module, "SUBMIT", "Challenge pack không có đáp án");
            }
            
            // So sánh câu trả lời với danh sách đáp án (case-insensitive, normalize)
            String matchedAnswer = null;
            for (String validAnswer : validAnswers) {
                // So sánh không phân biệt hoa thường và normalize (loại bỏ khoảng trắng)
                if (validAnswer.trim().equalsIgnoreCase(answer)) {
                    matchedAnswer = validAnswer;
                    break;
                }
            }
            
            if (matchedAnswer == null) {
                // Câu trả lời không đúng
                return createErrorResponse(module, "SUBMIT", "Đáp án không đúng");
            }
            
            // Tìm thấy đáp án đúng - Lấy frequency từ Dictionary
            Integer  frequency = dictionaryDAO.getWordFrequency(matchedAnswer);
            
            // Lấy level từ ChallengePack
            int level = room.getCp().getLevel();
            
            // Trả về frequency và level để client tự tính điểm
            // Format: "matchedAnswer,frequency,level"
            String result = matchedAnswer + "," + frequency + "," + level;
            
            return createSuccessResponse(module, "SUBMIT", result);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "SUBMIT", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "SUBMIT", "Lỗi khi submit: " + e.getMessage());
        }
    }

    // Xử lý cập nhật điểm của player (từ client gửi lên)
    private Response handleUpdate(String data) {
        // data format: "roomId,userId,points"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "UPDATE", "Du lieu khong hop le");
        }
        try {
            String[] parts = data.split(",");
            if (parts.length < 3) {
                return createErrorResponse(module, "UPDATE", "Du lieu khong hop le");
            }
            
            Integer  roomId = Integer.parseInt(parts[0]);
            Integer  userId = Integer.parseInt(parts[1]);
            int points = Integer.parseInt(parts[2]);
            
            // Kiểm tra room có tồn tại không
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "UPDATE", "Phòng không tồn tại");
            }
            
            // Kiểm tra user có trong room không
            if (!isUserInRoom(room, userId)) {
                return createErrorResponse(module, "UPDATE", "Bạn không trong phòng này");
            }
            
            // Cập nhật điểm của player trong database
            roomDAO.updatePlayerScore(roomId, userId, points);
            
            // Broadcast scoreboard qua listener socket (không xung đột với request-response)
            broadcastScoreBoard(roomId);
            
            return createSuccessResponse(module, "UPDATE", "Đã cập nhật điểm thành công");
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "UPDATE", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "UPDATE", "Lỗi khi cập nhật điểm: " + e.getMessage());
        }
    }

    // Xử lý kết thúc game
    private Response handleEnd(String data) {
        // data format: "roomId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "END", "Du lieu khong hop le");
        }
        try {
            Integer  roomId = Integer.parseInt(data);
            
            // Kiểm tra room có tồn tại không
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                // Phòng đã bị xóa (có thể do client khác đã END trước)
                // Không coi là lỗi, trả về success
                System.out.println("GameController.END: Room already deleted - roomId=" + roomId);
                return createSuccessResponse(module, "END", "Game đã kết thúc");
            }
            
            // Nếu phòng đã pending rồi (client khác đã END trước), vẫn cho phép
            if ("pending".equals(room.getStatus())) {
                System.out.println("GameController.END: Room already ended - roomId=" + roomId);
                return createSuccessResponse(module, "END", "Game đã kết thúc");
            }
            
            // Chỉ xử lý nếu phòng đang "playing"
            if (!"playing".equals(room.getStatus())) {
                return createErrorResponse(module, "END", "Phòng không ở trạng thái playing");
            }
            
            // Broadcast scoreboard lần cuối trước khi kết thúc game
            broadcastScoreBoard(roomId);
            
            // LƯU thông tin phòng cũ vào TEMP (trước khi xóa)
            Room oldRoom = roomDAO.getRoomById(roomId);
            if (oldRoom == null) {
                return createErrorResponse(module, "END", "Không tìm thấy phòng");
            }
            
            //Integer newRoomId = oldRoom.getId();
            long timstamp = System.currentTimeMillis() + new Random().nextInt(1000);
            Integer newRoomId = (int) (timstamp % 100000000);
            Integer ownerId = oldRoom.getOwnerId();
            int maxPlayer = oldRoom.getMaxPlayer();
            List<Player> players = new ArrayList<>(oldRoom.getPlayers()); // Copy để tránh reference (giữ điểm)
            
            System.out.println("GameController.END: Saved room info to TEMP - oldId=" + roomId + 
                             ", newId=" + newRoomId + ", owner=" + ownerId + 
                             ", maxPlayer=" + maxPlayer + ", players=" + players.size());
            
            // XÓA phòng cũ
            roomDAO.deleteRoom(roomId);
            System.out.println("GameController.END: Deleted old room " + roomId);
            
            // TẠO phòng mới với newRoomId
            Room newRoom = new Room();
            newRoom.setId(newRoomId);
            newRoom.setOwnerId(ownerId);
            newRoom.setMaxPlayer(maxPlayer);
            newRoom.setCreateAt(Instant.now());
            newRoom.setStatus("pending");
            newRoom.setPlayers(players); // Gán danh sách players với điểm đã được giữ
            
            roomDAO.createRoom(newRoom);
            System.out.println("GameController.END: Created new room " + newRoomId + " with " + players.size() + " players");
            
            // VERIFY: Kiểm tra phòng mới có trong DB không
            Room verifyRoom = roomDAO.getRoomById(newRoomId);
            if (verifyRoom == null) {
                System.err.println("GameController.END: ERROR - Room " + newRoomId + " NOT FOUND after createRoom()!");
            } else {
                System.out.println("GameController.END: VERIFIED - Room " + newRoomId + " exists in DB, status=" + 
                                 verifyRoom.getStatus() + ", players=" + verifyRoom.getPlayers().size());
            }
            
            // CHỜ thêm để đảm bảo room mới đã được tạo hoàn toàn
            try {
                Thread.sleep(300); // Delay 300ms
                System.out.println("GameController.END: Waited 300ms after creating new room");
            } catch (InterruptedException e) {
                System.err.println("GameController.END: Sleep interrupted - " + e.getMessage());
            }
            
            // BROADCAST phòng MỚI đến listeners của phòng CŨ (TRƯỚC KHI remove listeners)
            Room finalRoom = roomDAO.getRoomById(newRoomId);
            vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastUpdate(roomId, finalRoom);
            System.out.println("GameController.END: Broadcasted new room to old room listeners");
            
            // CHỜ để broadcast phòng mới kịp đến tất cả client
            try {
                Thread.sleep(500); // Delay 500ms để broadcast kịp
                System.out.println("GameController.END: Waited 500ms for new room broadcast");
            } catch (InterruptedException e) {
                System.err.println("GameController.END: Sleep interrupted - " + e.getMessage());
            }
            
            // SAU ĐÓ mới stop listening cho phòng cũ - remove tất cả listeners
            for (Player p : players) {
                vuatiengvietpj.util.RoomUpdateManager.getInstance().removeListener(roomId, p.getUserId());
            }
            
            // Trả về ID phòng mới cho client (CHỦ PHÒNG)
            String result = newRoomId.toString();
            return createSuccessResponse(module, "END", result);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "END", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "END", "Lỗi khi kết thúc game: " + e.getMessage());
        }
    }

    // Helper method: Kiểm tra user có trong room không
    private boolean isUserInRoom(Room room, Integer  userId) {
        if (room.getPlayers() == null || userId == null) {
            return false;
        }
        for (Player player : room.getPlayers()) {
            if (player.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    // Helper method: Tạo ScoreBoard từ Room với điểm của người chơi
    private ScoreBoard createScoreBoardFromRoom(Room room) {
        if (room == null) {
            return null;
        }
        
        ScoreBoard scoreBoard = new ScoreBoard();
        scoreBoard.setRoomId(room.getId());
        scoreBoard.setUpdateAt(Instant.now());
        
        // Lấy danh sách Player từ Room (đã có score từ database)
        if (room.getPlayers() != null && !room.getPlayers().isEmpty()) {
            List<Player> players = new java.util.ArrayList<>(room.getPlayers());
            
            // Sắp xếp Player theo điểm từ cao xuống thấp
            players.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
            
            scoreBoard.setPlayer(players);
        }
        
        return scoreBoard;
    }

    // Broadcast scoreboard đến tất cả clients trong room
    // Dùng RoomUpdateManager để broadcast qua listener socket
    public void broadcastScoreBoard(Integer  roomId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null) {
            return;
        }
        
        ScoreBoard scoreBoard = createScoreBoardFromRoom(room);
        if (scoreBoard != null) {
            // Broadcast ScoreBoard qua listener socket (không xung đột với request-response)
            vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastScoreBoard(roomId, scoreBoard);
        }
    }
}