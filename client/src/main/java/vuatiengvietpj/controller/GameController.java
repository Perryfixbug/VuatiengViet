package vuatiengvietpj.controller;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import vuatiengvietpj.model.Room;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.ScoreBoard;

public class GameController extends ClientController implements AutoCloseable {
    private String module = "GAME";
    private Gson gson;
    // Danh sách SubmitResult (chỉ đáp án đúng) cho từng user (theo userId) - chỉ lưu ở client
    private static java.util.Map<Long, java.util.List<SubmitResult>> submittedResultsByUser = new java.util.concurrent.ConcurrentHashMap<>();

    public GameController(String host, int port) throws IOException {
        super(host, port);
        this.gson = new GsonBuilder()
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

    // Gửi yêu cầu bắt đầu game lên server
    public Response startGame(Long roomId, Long userId) {
        try {
            String data = roomId + "," + userId;
            return sendAndReceive(module, "START", data);
        } catch (Exception e) {
            System.err.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Parse Room từ JSON response
    public Room parseRoom(String json) {
        try {
            return gson.fromJson(json, Room.class);
        } catch (Exception e) {
            System.err.println("Error parsing room: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Gửi yêu cầu submit answer
    public Response submitAnswer(String data) {
        try {
            return sendAndReceive(module, "SUBMIT", data);
        } catch (Exception e) {
            System.err.println("Error submitting answer: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Submit answer và parse kết quả
    public SubmitResult submitAnswerWithResult(Long roomId, Long userId, String answer) {
        try {
            // Kiểm tra answer không rỗng
            if (answer == null || answer.trim().isEmpty()) {
                return new SubmitResult(false, null, 0L, 0, "Đáp án không được rỗng");
            }
            
            // Kiểm tra xem từ đã được submit chưa bằng cách so sánh với các SubmitResult đã lưu của user
            java.util.List<SubmitResult> submittedResults = submittedResultsByUser.get(userId);
            if (submittedResults != null && !submittedResults.isEmpty()) {
                String answerLower = answer.trim().toLowerCase();
                boolean alreadySubmitted = submittedResults.stream()
                        .filter(result -> result.isSuccess()) // Chỉ kiểm tra các đáp án đúng
                        .anyMatch(result -> result.getMatchedAnswer() != null && 
                                result.getMatchedAnswer().toLowerCase().equals(answerLower));
                
                if (alreadySubmitted) {
                    // Từ này đã được submit trước đó - không gửi request lên server
                    return new SubmitResult(false, null, 0L, 0, "Bạn đã đoán từ này rồi");
                }
            }
            
            // Từ chưa được submit - gửi request lên server
            String data = roomId + "," + userId + "," + answer;
            Response response = sendAndReceive(module, "SUBMIT", data);
            
            if (response != null && response.isSuccess()) {
                // Parse kết quả: "matchedAnswer,frequency,level"
                String[] parts = response.getData().split(",");
                if (parts.length >= 3) {
                    String matchedAnswer = parts[0];
                    Long frequency = Long.parseLong(parts[1]);
                    int level = Integer.parseInt(parts[2]);
                    
                    // Tính điểm theo công thức: level * 10 * t
                    // frequency < 10: t = 3
                    // 10 <= frequency < 100: t = 2
                    // còn lại: t = 1
                    int t;
                    if (frequency < 10) {
                        t = 3;
                    } else if (10 <= frequency && frequency < 100) {
                        t = 2;
                    } else {
                        t = 1;
                    }
                    int points = level * 10 * t;
                    System.out.println("GameController: Điểm được cộng: " + points + " (level=" + level + ", frequency=" + frequency + ", t=" + t + ")");
                    
                    // Tạo SubmitResult cho đáp án đúng
                    SubmitResult successResult = new SubmitResult(true, matchedAnswer, frequency, level, points);
                    
                    // Lưu SubmitResult (chỉ đáp án đúng) vào danh sách của user (theo userId) - chỉ lưu ở client
                    submittedResultsByUser.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(successResult);
                    
                    System.out.println("GameController: Đã lưu SubmitResult cho từ '" + matchedAnswer + "' vào danh sách của user " + userId);
                    
                    // Gửi điểm lên server để cập nhật vào database
                    updateScore(roomId, userId, points);
                    
                    return successResult;
                }
            } else {
                String errorMsg = (response != null) ? response.getData() : "Không nhận được phản hồi";
                return new SubmitResult(false, null, 0L, 0, errorMsg);
            }
        } catch (Exception e) {
            System.err.println("Error submitting answer: " + e.getMessage());
            e.printStackTrace();
            return new SubmitResult(false, null, 0L, 0, "Lỗi: " + e.getMessage());
        }
        return new SubmitResult(false, null, 0L, 0, "Lỗi không xác định");
    }
    
    // Lấy danh sách SubmitResult (chỉ đáp án đúng) của một user (chỉ lưu ở client)
    public static java.util.List<SubmitResult> getUserSubmittedResults(Long userId) {
        return submittedResultsByUser.getOrDefault(userId, new java.util.ArrayList<>());
    }
    
    // Xóa danh sách SubmitResult của một user (khi logout hoặc reset)
    public static void clearUserSubmittedResults(Long userId) {
        submittedResultsByUser.remove(userId);
    }
    
    // Lấy số lượng đáp án đúng của một user
    public static int getUserSubmittedResultsCount(Long userId) {
        return getUserSubmittedResults(userId).size();
    }
    
    // Lấy danh sách từ đã đoán đúng của một user (helper method)
    public static java.util.Set<String> getUserGuessedWords(Long userId) {
        java.util.Set<String> words = new java.util.HashSet<>();
        getUserSubmittedResults(userId).stream()
                .filter(SubmitResult::isSuccess)
                .forEach(result -> {
                    if (result.getMatchedAnswer() != null) {
                        words.add(result.getMatchedAnswer());
                    }
                });
        return words;
    }

    // Inner class để chứa kết quả submit
    public static class SubmitResult {
        private boolean success;
        private String matchedAnswer;
        private Long frequency;
        private int level;
        private int points; // Điểm đã tính
        private String errorMessage;
        
        public SubmitResult(boolean success, String matchedAnswer, Long frequency, int level) {
            this.success = success;
            this.matchedAnswer = matchedAnswer;
            this.frequency = frequency;
            this.level = level;
            this.points = 0;
        }
        
        public SubmitResult(boolean success, String matchedAnswer, Long frequency, int level, int points) {
            this.success = success;
            this.matchedAnswer = matchedAnswer;
            this.frequency = frequency;
            this.level = level;
            this.points = points;
        }
        
        public SubmitResult(boolean success, String matchedAnswer, Long frequency, int level, String errorMessage) {
            this.success = success;
            this.matchedAnswer = matchedAnswer;
            this.frequency = frequency;
            this.level = level;
            this.points = 0;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMatchedAnswer() {
            return matchedAnswer;
        }
        
        public Long getFrequency() {
            return frequency;
        }
        
        public int getLevel() {
            return level;
        }
        
        public int getPoints() {
            return points;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    // Gửi điểm đã tính lên server để cập nhật vào database
    public Response updateScore(Long roomId, Long userId, int points) {
        try {
            String data = roomId + "," + userId + "," + points;
            Response response = sendAndReceive(module, "UPDATE", data);
            if (response != null && response.isSuccess()) {
                System.out.println("GameController: Đã cập nhật điểm " + points + " cho user " + userId + " trong room " + roomId);
            } else {
                System.err.println("GameController: Lỗi khi cập nhật điểm: " + (response != null ? response.getData() : "Không nhận được phản hồi"));
            }
            return response;
        } catch (Exception e) {
            System.err.println("Error updating score: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Parse ScoreBoard từ JSON response
    public ScoreBoard parseScoreBoard(String json) {
        try {
            return gson.fromJson(json, ScoreBoard.class);
        } catch (Exception e) {
            System.err.println("Error parsing scoreboard: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Subscribe để nhận broadcast scoreboard
    public Response subscribe(Long roomId) {
        try {
            return sendAndReceive(module, "SUBSCRIBE", roomId.toString());
        } catch (Exception e) {
            System.err.println("Error subscribing: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Unsubscribe khỏi room
    public Response unsubscribe(Long roomId) {
        try {
            return sendAndReceive(module, "UNSUBSCRIBE", roomId.toString());
        } catch (Exception e) {
            System.err.println("Error unsubscribing: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Bắt đầu listener thread để nhận broadcast messages
    // Lưu ý: Cần giữ connection mở để nhận broadcast
    private Thread broadcastListenerThread;
    
    public void startBroadcastListener(java.util.function.Consumer<Response> onBroadcast) {
        if (socket == null || socket.isClosed()) {
            System.err.println("Cannot start listener: socket is closed");
            return;
        }
        
        // Stop existing listener if any
        stopBroadcastListener();
        
        // Remove socket timeout for broadcast listening (or set longer timeout)
        try {
            if (!socket.isClosed()) {
                socket.setSoTimeout(0); // No timeout for broadcast listening
            }
        } catch (Exception e) {
            System.err.println("Error setting socket timeout: " + e.getMessage());
        }
        
        broadcastListenerThread = new Thread(() -> {
            try {
                System.out.println("GameController: Broadcast listener started for socket: " + socket);
                while (!socket.isClosed() && socket.isConnected() && !Thread.currentThread().isInterrupted()) {
                    try {
                        // Check if socket is still valid before attempting read
                        if (socket.isClosed() || !socket.isConnected()) {
                            System.out.println("GameController: Socket closed, stopping listener");
                            break;
                        }
                        
                        Response response = receiveResponse();
                        if (response != null) {
                            // Kiểm tra nếu là broadcast message
                            if ("SCOREBOARD".equals(response.getMaLenh())) {
                                System.out.println("GameController: Received broadcast SCOREBOARD");
                                onBroadcast.accept(response);
                            } else {
                                System.out.println("GameController: Received non-broadcast message: " + response.getMaLenh() + " (module: " + response.getmodule() + ")");
                            }
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout is OK for keep-alive checking, continue listening
                        // Check if we should continue
                        if (socket.isClosed() || !socket.isConnected()) {
                            break;
                        }
                        continue;
                    } catch (java.io.EOFException e) {
                        // Connection closed by server
                        System.out.println("GameController: Broadcast listener - EOF (connection closed by server)");
                        break;
                    } catch (java.net.SocketException e) {
                        // Socket error
                        System.out.println("GameController: Broadcast listener - SocketException: " + e.getMessage());
                        break;
                    } catch (java.io.InterruptedIOException e) {
                        // Thread was interrupted
                        System.out.println("GameController: Broadcast listener - Interrupted");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Broadcast listener error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.out.println("GameController: Broadcast listener stopped");
            }
        });
        broadcastListenerThread.setDaemon(true);
        broadcastListenerThread.setName("GameController-broadcast-listener");
        broadcastListenerThread.start();
    }
    
    public void stopBroadcastListener() {
        if (broadcastListenerThread != null && broadcastListenerThread.isAlive()) {
            // Interrupt the thread
            broadcastListenerThread.interrupt();
            broadcastListenerThread = null;
        }
    }

    // Gửi yêu cầu kết thúc game
    public Response endGame(String data) {
        try {
            return sendAndReceive(module, "END", data);
        } catch (Exception e) {
            System.err.println("Error ending game: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        stopBroadcastListener();
        try {
            super.close();
        } catch (Exception ignored) {
        }
    }
}
