package vuatiengvietpj.util;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.ScoreBoard;

import java.io.ObjectOutputStream;
import java.time.Instant;

/**
 * RoomManager: Quản lý các clients đang trong room và broadcast scoreboard
 */
public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final ConcurrentMap<Integer, List<ClientConnection>> roomClients = new ConcurrentHashMap<>();
    private final Gson gson;

    private RoomManager() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .create();
    }

    public static RoomManager getInstance() {
        return instance;
    }

    /**
     * Đăng ký client vào room để nhận broadcast
     */
    public synchronized void subscribeToRoom(Integer roomId, Socket socket, ObjectOutputStream out) {
        roomClients.putIfAbsent(roomId, new ArrayList<>());
        ClientConnection conn = new ClientConnection(socket, out);
        roomClients.get(roomId).add(conn);
        System.out.println("RoomManager: Client subscribed to room " + roomId + ", total: " + roomClients.get(roomId).size());
    }

    /**
     * Hủy đăng ký client khỏi room
     */
    public synchronized void unsubscribeFromRoom(Integer roomId, Socket socket) {
        List<ClientConnection> clients = roomClients.get(roomId);
        if (clients != null) {
            clients.removeIf(conn -> conn.socket.equals(socket));
            if (clients.isEmpty()) {
                roomClients.remove(roomId);
            }
            System.out.println("RoomManager: Client unsubscribed from room " + roomId);
        }
    }

    /**
     * Broadcast scoreboard đến tất cả clients trong room
     */
    public void broadcastScoreBoard(Integer roomId, ScoreBoard scoreBoard) {
        List<ClientConnection> clients = roomClients.get(roomId);
        if (clients == null || clients.isEmpty()) {
            System.out.println("RoomManager: No clients in room " + roomId);
            return;
        }

        Response response = new Response("GAME", "SCOREBOARD", gson.toJson(scoreBoard), true);
        List<ClientConnection> toRemove = new ArrayList<>();

        for (ClientConnection conn : clients) {
            try {
                if (conn.socket.isClosed() || !conn.socket.isConnected()) {
                    toRemove.add(conn);
                    continue;
                }
                conn.out.writeObject(response);
                conn.out.flush();
                System.out.println("RoomManager: Broadcasted scoreboard to client in room " + roomId);
            } catch (IOException e) {
                System.err.println("RoomManager: Error broadcasting to client: " + e.getMessage());
                toRemove.add(conn);
            }
        }

        // Remove disconnected clients
        if (!toRemove.isEmpty()) {
            synchronized (this) {
                clients.removeAll(toRemove);
            }
        }
    }

    /**
     * Lấy số lượng clients trong room
     */
    public int getClientCount(Integer roomId) {
        List<ClientConnection> clients = roomClients.get(roomId);
        return clients != null ? clients.size() : 0;
    }

    /**
     * Inner class để lưu thông tin connection của client
     */
    private static class ClientConnection {
        final Socket socket;
        final ObjectOutputStream out;

        ClientConnection(Socket socket, ObjectOutputStream out) {
            this.socket = socket;
            this.out = out;
        }
    }
}

