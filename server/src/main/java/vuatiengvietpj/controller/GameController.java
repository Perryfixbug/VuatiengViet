package vuatiengvietpj.controller;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import vuatiengvietpj.dao.ChallengePackDAO;
import vuatiengvietpj.dao.DictionaryDAO;
import vuatiengvietpj.dao.RoomDAO;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.ScoreBoard;
import vuatiengvietpj.util.RoomManager;

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
            case "SUBSCRIBE" -> handleSubscribe(data);
            case "UNSUBSCRIBE" -> handleUnsubscribe(data);
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
            
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            
            // Lấy room từ database
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "START", "Phòng không tồn tại");
            }
            
            // Kiểm tra người start có phải owner của phòng không
            if (!java.util.Objects.equals(room.getOwnerId(), userId)) {
                return createErrorResponse(module, "START", "Chỉ chủ phòng mới được bắt đầu game");
            }
            
            // Kiểm tra room đang ở trạng thái pending
            if (!"pending".equals(room.getStatus())) {
                return createErrorResponse(module, "START", "Phòng không ở trạng thái cho phép bắt đầu");
            }
            
            // Lấy danh sách tất cả challenge pack IDs
            List<Long> challengePackIds = challengePackDAO.getAllChallengePackIds();
            if (challengePackIds.isEmpty()) {
                return createErrorResponse(module, "START", "Không có challenge pack nào trong hệ thống");
            }
            
            // Chọn ngẫu nhiên một challenge pack ID
            Random random = new Random();
            Long randomCpId = challengePackIds.get(random.nextInt(challengePackIds.size()));
            
            // Gán challenge pack vào room
            roomDAO.addChallengePackToRoom(roomId, randomCpId);
            
            // Chuyển room thành trạng thái playing
            roomDAO.updateRoom(roomId, null, "playing", null);
            
            // Lấy lại room với challenge pack đã được gán
            room = roomDAO.getRoomById(roomId);
            
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
            
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
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
            
            long challengePackId = room.getCp().getId();
            
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
            Long frequency = dictionaryDAO.getWordFrequency(matchedAnswer);
            
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
            
            Long roomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
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
            
            // Broadcast scoreboard sau khi update score
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
            Long roomId = Long.parseLong(data);
            
            // Kiểm tra room có tồn tại không
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "END", "Phòng không tồn tại");
            }
            
            // Kiểm tra room đang ở trạng thái playing
            if (!"playing".equals(room.getStatus())) {
                return createErrorResponse(module, "END", "Phòng không ở trạng thái playing");
            }
            
            // Broadcast scoreboard lần cuối trước khi kết thúc game
            broadcastScoreBoard(roomId);
            
            // Chuyển room thành trạng thái pending
            roomDAO.updateRoom(roomId, null, "pending", null);
            
            String result = "Game đã kết thúc thành công";
            return createSuccessResponse(module, "END", result);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "END", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "END", "Lỗi khi kết thúc game: " + e.getMessage());
        }
    }

    // Helper method: Kiểm tra user có trong room không
    private boolean isUserInRoom(Room room, Long userId) {
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

    // Xử lý subscribe để nhận broadcast scoreboard
    private Response handleSubscribe(String data) {
        // data format: "roomId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "SUBSCRIBE", "Du lieu khong hop le");
        }
        try {
            Long roomId = Long.parseLong(data);
            
            // Kiểm tra room tồn tại
            Room room = roomDAO.getRoomById(roomId);
            if (room == null) {
                return createErrorResponse(module, "SUBSCRIBE", "Phòng không tồn tại");
            }
            
            // Đăng ký client vào room để nhận broadcast
            RoomManager.getInstance().subscribeToRoom(roomId, clientSocket, out);
            
            return createSuccessResponse(module, "SUBSCRIBE", "Đã đăng ký nhận broadcast cho room " + roomId);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "SUBSCRIBE", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "SUBSCRIBE", "Lỗi khi subscribe: " + e.getMessage());
        }
    }

    // Xử lý unsubscribe
    private Response handleUnsubscribe(String data) {
        // data format: "roomId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "UNSUBSCRIBE", "Du lieu khong hop le");
        }
        try {
            Long roomId = Long.parseLong(data);
            
            // Hủy đăng ký client khỏi room
            RoomManager.getInstance().unsubscribeFromRoom(roomId, clientSocket);
            
            return createSuccessResponse(module, "UNSUBSCRIBE", "Đã hủy đăng ký room " + roomId);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "UNSUBSCRIBE", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "UNSUBSCRIBE", "Lỗi khi unsubscribe: " + e.getMessage());
        }
    }

    // Broadcast scoreboard đến tất cả clients trong room
    public void broadcastScoreBoard(Long roomId) {
        Room room = roomDAO.getRoomById(roomId);
        if (room == null) {
            return;
        }
        
        ScoreBoard scoreBoard = createScoreBoardFromRoom(room);
        if (scoreBoard != null) {
            RoomManager.getInstance().broadcastScoreBoard(roomId, scoreBoard);
        }
    }
}