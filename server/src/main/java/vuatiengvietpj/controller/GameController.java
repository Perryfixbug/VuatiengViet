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
import vuatiengvietpj.dao.UserDAO;
import vuatiengvietpj.model.ChallengePack;
import vuatiengvietpj.model.Player;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.ScoreBoard;
import vuatiengvietpj.util.RoomManager;
import vuatiengvietpj.util.RedisManager;
import vuatiengvietpj.util.ConfigManager;
import java.util.HashMap;
import java.util.Map;

public class GameController extends ServerController {
    private String module = "GAME";
    private Gson gson;
    private RoomDAO roomDAO;
    private UserDAO userDAO;
    private ChallengePackDAO challengePackDAO;
    private DictionaryDAO dictionaryDAO;

    public GameController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.roomDAO = new RoomDAO();
        this.userDAO = new UserDAO();
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
            
            Integer  roomId = Integer .parseInt(parts[0]);
            Integer  userId = Integer .parseInt(parts[1]);
            
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

            // Lưu đáp án và tần suất vào Redis cho room này
            try {
                int gameTtl = ConfigManager.getInt("cache.game.ttl", 90);
                List<String> answers = challengePackDAO.getAnswersByChallengePackId(randomCpId);
                if (answers != null && !answers.isEmpty()) {
                    String answersKey = "game:room:" + roomId + ":cp:" + randomCpId + ":answers";
                    RedisManager.setCache(answersKey, answers, gameTtl);

                    Map<String, Integer> freqMap = new HashMap<>();
                    for (String a : answers) {
                        Integer f = dictionaryDAO.getWordFrequency(a);
                        freqMap.put(a, f == null ? 0 : f);
                    }
                    String freqKey = "game:room:" + roomId + ":cp:" + randomCpId + ":freq";
                    RedisManager.setCache(freqKey, freqMap, gameTtl);
                }
            } catch (Exception ex) {
                System.err.println("GameController.START: Failed to cache answers/frequencies in Redis - " + ex.getMessage());
            }
            
            // Broadcast room update để tất cả client biết game đã bắt đầu
            if (room != null) {
                vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastUpdate(roomId, room);
                System.out.println("GameController.START: Broadcasted game start to all members - roomId=" + roomId);
            }
            
            // Khởi tạo scoreboard Redis: set điểm 0 cho tất cả người chơi hiện có
            try {
                redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
                if (jedis != null) {
                    try (jedis) {
                        String zKey = "scoreboard:" + roomId;
                        String hKey = "scoreboard:names:" + roomId;
                        int gameTtl = ConfigManager.getInt("cache.game.ttl", 90);
                        if (room != null && room.getPlayers() != null) {
                            for (Player p : room.getPlayers()) {
                                jedis.zadd(zKey, 0.0, "user:" + p.getUserId());
                                if (p.getName() != null) {
                                    jedis.hset(hKey, "user:" + p.getUserId(), p.getName());
                                }
                            }
                        }
                        jedis.expire(zKey, gameTtl);
                        jedis.expire(hKey, gameTtl);
                    }
                }
            } catch (Exception ex) {
                System.err.println("GameController.START: Failed to init Redis scoreboard - " + ex.getMessage());
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

            // Lấy đáp án từ Redis thay vì DB
            String answersKey = "game:room:" + roomId + ":cp:" + challengePackId + ":answers";
            List<?> cachedAnswers = RedisManager.getCache(answersKey, List.class);
            if (cachedAnswers == null || cachedAnswers.isEmpty()) {
                return createErrorResponse(module, "SUBMIT", "Dữ liệu game không sẵn sàng. Vui lòng bắt đầu lại.");
            }
            List<String> validAnswers = new java.util.ArrayList<>();
            for (Object o : cachedAnswers) {
                if (o != null) validAnswers.add(o.toString());
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
            
            // Tìm thấy đáp án đúng - Lấy frequency từ Redis thay vì DB
            String freqKey = "game:room:" + roomId + ":cp:" + challengePackId + ":freq";
            java.util.Map<?,?> freqMapRaw = RedisManager.getCache(freqKey, java.util.Map.class);
            int frequency = 0;
            if (freqMapRaw != null) {
                Object v = freqMapRaw.get(matchedAnswer);
                if (v instanceof Number) {
                    frequency = ((Number) v).intValue();
                } else if (v instanceof String) {
                    try { frequency = Integer.parseInt((String) v); } catch (Exception ignore) {}
                }
            }
            
        
            
            // Trả về frequency để client tự tính điểm
            // Format: "matchedAnswer,frequency"
            String result = matchedAnswer + "," + frequency;
            
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
            
            // Cập nhật điểm trong Redis ZSET (không ghi DB)
            try {
                redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
                if (jedis != null) {
                    try (jedis) {
                        String zKey = "scoreboard:" + roomId;
                        jedis.zincrby(zKey, points, "user:" + userId);
                    }
                }
            } catch (Exception ex) {
                System.err.println("GameController.UPDATE: Redis zincrby failed - " + ex.getMessage());
            }
            
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
            
            // GHI điểm cuối từ Redis vào DB player trước khi kết thúc game
            try {
                redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
                if (jedis != null) {
                    try (jedis) {
                        String zKey = "scoreboard:" + roomId;
                        java.util.List<redis.clients.jedis.resps.Tuple> tuples = jedis.zrevrangeWithScores(zKey, 0, -1);
                        if (tuples != null) {
                            for (redis.clients.jedis.resps.Tuple t : tuples) {
                                String member = t.getElement();
                                double score = t.getScore();
                                Integer uid = null;
                                if (member != null && member.startsWith("user:")) {
                                    try { uid = Integer.parseInt(member.substring(5)); } catch (Exception ignore) {}
                                }
                                if (uid == null) continue;
                                roomDAO.setPlayerScore(roomId, uid, (int) Math.round(score));
                            }
                        }
                        // Sau khi lưu DB, xóa luôn cache Redis (ZSET + HASH)
                        try {
                            String hKey = "scoreboard:names:" + roomId;
                            jedis.del(zKey);
                            jedis.del(hKey);
                            System.out.println("GameController.END: Deleted Redis scoreboard keys for room " + roomId);
                        } catch (Exception delEx) {
                            System.err.println("GameController.END: Failed to delete Redis keys - " + delEx.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("GameController.END: Persist final scores to DB failed - " + ex.getMessage());
            }

            // Broadcast scoreboard lần cuối để client thấy điểm (đọc từ DB player)
            try {
                Room roomForBoard = roomDAO.getRoomById(roomId);
                ScoreBoard finalBoard = createScoreBoardFromRoom(roomForBoard);
                if (finalBoard != null) {
                    vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastScoreBoard(roomId, finalBoard);
                }
            } catch (Exception bEx) {
                System.err.println("GameController.END: Failed DB-based scoreboard broadcast - " + bEx.getMessage());
            }
            
            // CHỜ để đảm bảo broadcast đã được gửi và nhận đủ
            try {
                Thread.sleep(500); // Delay 500ms để broadcast kịp đến tất cả client
                System.out.println("GameController.END: Waited 500ms for final scoreboard broadcast");
            } catch (InterruptedException e) {
                System.err.println("GameController.END: Sleep interrupted - " + e.getMessage());
            }

            // CỘNG điểm trong phòng vào tổng điểm người chơi và lưu DB
            try {
                redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
                if (jedis != null) {
                    try (jedis) {
                        String zKey = "scoreboard:" + roomId;
                        java.util.List<redis.clients.jedis.resps.Tuple> tuples = jedis.zrevrangeWithScores(zKey, 0, -1);
                        if (tuples != null) {
                            for (redis.clients.jedis.resps.Tuple t : tuples) {
                                String member = t.getElement(); // e.g. user:123
                                double scoreD = t.getScore();
                                Integer uid = null;
                                if (member != null && member.startsWith("user:")) {
                                    try { uid = Integer.parseInt(member.substring(5)); } catch (Exception ignore) {}
                                }
                                if (uid == null) continue;

                                int addScore = (int) Math.round(scoreD);
                                try {
                                    vuatiengvietpj.model.User u = userDAO.findById(uid);
                                    int current = (u != null && u.getTotalScore() != null) ? u.getTotalScore() : 0;
                                    int updated = current + addScore;
                                    boolean ok = userDAO.updateScore(uid, updated);
                                    if (!ok) {
                                        System.err.println("GameController.END: Failed to update totalScore for user " + uid);
                                    }
                                } catch (Exception exu) {
                                    System.err.println("GameController.END: Error updating user totalScore: userId=" + uid + ", err=" + exu.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("GameController.END: Failed to persist total scores - " + ex.getMessage());
            }
            
            // LƯU thông tin phòng cũ (trước khi xóa)
            Room oldRoom = roomDAO.getRoomById(roomId);
            if (oldRoom == null) {
                return createErrorResponse(module, "END", "Không tìm thấy phòng");
            }
            
            Integer  ownerId = oldRoom.getOwnerId();
            int maxPlayer = oldRoom.getMaxPlayer();
            ChallengePack cp = oldRoom.getCp();
            List<Player> players = new ArrayList<>(oldRoom.getPlayers()); // Copy để tránh reference
            
            System.out.println("GameController.END: Saved room info - owner=" + ownerId + 
                             ", maxPlayer=" + maxPlayer + ", players=" + players.size());
            
            // XÓA phòng cũ
            roomDAO.deleteRoom(roomId);
            System.out.println("GameController.END: Deleted old room " + roomId);
            
            // TẠO phòng mới với thông tin đã lưu
            // Generate new ID (chỉ lấy 8 chữ số)
            long timestamp = System.currentTimeMillis() + new java.util.Random().nextInt(1000);
            Integer newRoomId = (Integer) ((int) (timestamp % 100000000));
            Room newRoom = new Room(newRoomId, ownerId, maxPlayer, Instant.now(), "pending", cp, new ArrayList<>());
            
            // THÊM lại players vào phòng mới (GIỮ NGUYÊN điểm để hiển thị kết quả)
            for (Player p : players) {
                Player newPlayer = new Player();
                newPlayer.setUserId(p.getUserId());
                newPlayer.setName(p.getName());
                newPlayer.setRoomId(newRoomId);
                newPlayer.setScore(p.getScore()); // GIỮ NGUYÊN điểm (không reset)
                newRoom.getPlayers().add(newPlayer);
            }
            
            // Lưu phòng mới vào DB
            roomDAO.createRoom(newRoom);
            
            System.out.println("GameController.END: Created new room " + newRoom.getId() + 
                             " with " + newRoom.getPlayers().size() + " players");
            
            // CHỜ thêm để đảm bảo room mới đã được tạo hoàn toàn
            try {
                Thread.sleep(300); // Delay 300ms
                System.out.println("GameController.END: Waited 300ms after creating new room");
            } catch (InterruptedException e) {
                System.err.println("GameController.END: Sleep interrupted - " + e.getMessage());
            }
            
            // BROADCAST phòng MỚI đến listeners của phòng CŨ (TRƯỚC KHI remove listeners)
            Room finalRoom = roomDAO.getRoomById(newRoom.getId());
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
            
            // Redis scoreboard đã bị xóa ở trên sau khi lưu DB

            // Trả về ID phòng mới cho client (CHỦ PHÒNG)
            String result = newRoom.getId().toString();
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

    // Xử lý subscribe để nhận broadcast scoreboard
    private Response handleSubscribe(String data) {
        // data format: "roomId"
        if (data == null || data.isEmpty()) {
            return createErrorResponse(module, "SUBSCRIBE", "Du lieu khong hop le");
        }
        try {
            Integer  roomId = Integer .parseInt(data);
            
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
            Integer roomId = Integer.parseInt(data);
            
            // Hủy đăng ký client khỏi room
            RoomManager.getInstance().unsubscribeFromRoom(roomId, clientSocket);
            
            return createSuccessResponse(module, "UNSUBSCRIBE", "Đã hủy đăng ký room " + roomId);
        } catch (NumberFormatException e) {
            return createErrorResponse(module, "UNSUBSCRIBE", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(module, "UNSUBSCRIBE", "Lỗi khi unsubscribe: " + e.getMessage());
        }
    }

    // Broadcast scoreboard đến tất cả clients trong room - lấy dữ liệu từ Redis
    // Ưu tiên Redis ZSET (điểm) + HASH (tên); fallback tên từ Room nếu cần
    public void broadcastScoreBoard(Integer  roomId) {
        ScoreBoard scoreBoard = new ScoreBoard();
        scoreBoard.setRoomId(roomId);
        scoreBoard.setUpdateAt(java.time.Instant.now());

        java.util.List<Player> players = new java.util.ArrayList<>();

        try {
            redis.clients.jedis.Jedis jedis = vuatiengvietpj.util.RedisManager.getResource();
            if (jedis != null) {
                try (jedis) {
                    String zKey = "scoreboard:" + roomId;
                    String hKey = "scoreboard:names:" + roomId;
                    java.util.List<redis.clients.jedis.resps.Tuple> tuples = jedis.zrevrangeWithScores(zKey, 0, -1);

                    // Optional fallback room to fetch names if missing
                    Room roomForNames = roomDAO.getRoomById(roomId);

                    for (redis.clients.jedis.resps.Tuple t : tuples) {
                        String member = t.getElement(); // e.g., user:123
                        double score = t.getScore();
                        Integer uid = null;
                        if (member != null && member.startsWith("user:")) {
                            try { uid = Integer.parseInt(member.substring(5)); } catch (Exception ignore) {}
                        }
                        if (uid == null) continue;

                        String name = jedis.hget(hKey, member);
                        if (name == null && roomForNames != null && roomForNames.getPlayers() != null) {
                            for (Player p : roomForNames.getPlayers()) {
                                if (java.util.Objects.equals(p.getUserId(), uid)) { name = p.getName(); break; }
                            }
                        }

                        Player p = new Player();
                        p.setUserId(uid);
                        p.setName(name);
                        p.setScore((int) Math.round(score));
                        players.add(p);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("GameController.broadcastScoreBoard: Redis read failed - " + ex.getMessage());
        }

        scoreBoard.setPlayer(players);
        vuatiengvietpj.util.RoomUpdateManager.getInstance().broadcastScoreBoard(roomId, scoreBoard);
    }
}