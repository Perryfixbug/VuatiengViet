package vuatiengvietpj;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import vuatiengvietpj.controller.RoomController;

public class ServerApp {
    public static void main(String[] args) {
        // NOTE: Session/Redis subscriber removed for Room-only testing

        // simple per-IP rate limiter to avoid abusive reconnect storms
        final ConcurrentHashMap<String, Long> lastConnect = new ConcurrentHashMap<>();
        final long MIN_MS_BETWEEN_CONNECT = 500; // 500ms between accepts from same IP

        try (ServerSocket serverSocket = new ServerSocket(2208)) {
            System.out.println("Server started on port 2208...");
            System.out.println("Đợi client kết nối...\n");

            while (true) {
                Socket client = serverSocket.accept();
                String ip = client.getInetAddress().toString();
                long now = Instant.now().toEpochMilli();
                Long prev = lastConnect.get(ip);
                if (prev != null && now - prev < MIN_MS_BETWEEN_CONNECT) {
                    System.out.println("Throttling connection from " + ip + " (" + (now - prev) + "ms since last)");
                    try { client.close(); } catch (Exception ignored) {}
                    continue;
                }
                lastConnect.put(ip, now);

                new Thread(() -> {
                    try {
                        System.out.println("IP client: " + client.getInetAddress());
                        // Temporarily use RoomController to test Room features
                        RoomController controller = new RoomController(client);
                        controller.handleClient(client.getInetAddress().toString());
                    } catch (Exception e) {
                        System.err.println("Lỗi tạo controller: " + e.getMessage());
                        try {
                            client.close();
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Loi server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}