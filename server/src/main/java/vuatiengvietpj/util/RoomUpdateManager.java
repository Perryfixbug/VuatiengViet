package vuatiengvietpj.util;

import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;

/**
 * Quản lý các listener connections cho room updates
 * Sử dụng pattern Singleton để đảm bảo chỉ có 1 instance duy nhất
 */
public class RoomUpdateManager {
    private static RoomUpdateManager instance;
    
    // roomId -> userId -> ObjectOutputStream
    // ConcurrentHashMap để thread-safe
    private Map<Integer, ConcurrentHashMap<Integer, ObjectOutputStream>> listeners = new ConcurrentHashMap<>();
    
    private Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class,
            (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
        .create();
    
    private RoomUpdateManager() {
        // Private constructor cho Singleton
    }
    
    public static RoomUpdateManager getInstance() {
        if (instance == null) {
            synchronized (RoomUpdateManager.class) {
                if (instance == null) {
                    instance = new RoomUpdateManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Đăng ký một listener cho room
     * @param roomId ID của phòng
     * @param userId ID của user
     * @param out ObjectOutputStream để gửi updates
     */
    public void addListener(Integer roomId, Integer userId, ObjectOutputStream out) {
        listeners.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, out);
        System.out.println("RoomUpdateManager: Listener added - room=" + roomId + ", user=" + userId + 
                         ", total in room=" + listeners.get(roomId).size());
    }
    
    /**
     * Xóa listener khi user disconnect hoặc rời phòng
     */
    public void removeListener(Integer roomId, Integer userId) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        if (roomListeners != null) {
            roomListeners.remove(userId);
            System.out.println("RoomUpdateManager: Listener removed - room=" + roomId + ", user=" + userId);
            
            if (roomListeners.isEmpty()) {
                listeners.remove(roomId);
                System.out.println("RoomUpdateManager: Room " + roomId + " has no more listeners, removed from map");
            }
        }
    }
    
    /**
     * Broadcast room update tới tất cả listeners trong phòng
     * @param roomId ID của phòng
     * @param room Object Room đã được cập nhật
     */
    public void broadcastUpdate(Integer roomId, Room room) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        
        if (roomListeners == null || roomListeners.isEmpty()) {
            System.out.println("RoomUpdateManager: No listeners for room " + roomId);
            return;
        }
        
        Response update = new Response("ROOM", "UPDATE", gson.toJson(room), true);
        
        System.out.println("RoomUpdateManager: Broadcasting to " + roomListeners.size() + 
                         " listeners in room " + roomId);
        
        // Duyệt qua tất cả listeners
        roomListeners.forEach((userId, out) -> {
            try {
                synchronized (out) { // Thread-safe write
                    out.writeObject(update);
                    out.flush();
                }
                System.out.println("  Sent update to user=" + userId);
            } catch (Exception e) {
                System.err.println("  Failed to send to user=" + userId + ": " + e.getMessage());
                // Xóa listener lỗi để tránh gửi lại
                roomListeners.remove(userId);
            }
        });
    }
    
    /**
     * Gửi message tới một user cụ thể (dùng cho KICK)
     * @param roomId ID của phòng
     * @param userId ID của user cần nhận message
     * @param command Command name (vd: "KICKED")
     * @param data Data của message
     */
    public void sendToUser(Integer roomId, Integer userId, String command, String data) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        if (roomListeners == null) {
            System.out.println("RoomUpdateManager: No listeners for room " + roomId);
            return;
        }
        
        ObjectOutputStream out = roomListeners.get(userId);
        if (out != null) {
            try {
                synchronized (out) {
                    Response response = new Response("ROOM", command, data, true);
                    out.writeObject(response);
                    out.flush();
                }
                System.out.println("RoomUpdateManager: Sent " + command + " to user=" + userId);
            } catch (Exception e) {
                System.err.println("RoomUpdateManager: Failed to send " + command + " to user=" + userId);
                roomListeners.remove(userId);
            }
        } else {
            System.out.println("RoomUpdateManager: User " + userId + " not found in room " + roomId);
        }
    }
    
    /**
     * Lấy số lượng listeners trong một phòng
     */
    public int getListenerCount(Integer roomId) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        return (roomListeners == null) ? 0 : roomListeners.size();
    }
    
    /**
     * Kiểm tra xem user có listener active trong phòng không
     * @param roomId ID của phòng
     * @param userId ID của user
     * @return true nếu user có listener active
     */
    public boolean hasListener(Integer roomId, Integer userId) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        return roomListeners != null && roomListeners.containsKey(userId);
    }
    
    /**
     * Broadcast scoreboard update tới tất cả listeners trong phòng
     * @param roomId ID của phòng
     * @param scoreBoard Object ScoreBoard đã được cập nhật
     */
    public void broadcastScoreBoard(Integer roomId, vuatiengvietpj.model.ScoreBoard scoreBoard) {
        ConcurrentHashMap<Integer, ObjectOutputStream> roomListeners = listeners.get(roomId);
        
        if (roomListeners == null || roomListeners.isEmpty()) {
            System.out.println("RoomUpdateManager: No listeners for scoreboard in room " + roomId);
            return;
        }
        
        Response update = new Response("GAME", "SCOREBOARD", gson.toJson(scoreBoard), true);
        
        System.out.println("RoomUpdateManager: Broadcasting SCOREBOARD to " + roomListeners.size() + 
                         " listeners in room " + roomId);
        
        // Duyệt qua tất cả listeners
        roomListeners.forEach((userId, out) -> {
            try {
                synchronized (out) { // Thread-safe write
                    out.writeObject(update);
                    out.flush();
                }
                System.out.println("  Sent SCOREBOARD to user=" + userId);
            } catch (Exception e) {
                System.err.println("  Failed to send SCOREBOARD to user=" + userId + ": " + e.getMessage());
                // Xóa listener lỗi để tránh gửi lại
                roomListeners.remove(userId);
            }
        });
    }
}
