package vuatiengvietpj;

import java.net.ServerSocket;
import java.net.Socket;
import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.util.SessionManager;
import vuatiengvietpj.util.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class ServerApp {
    public static void main(String[] args) {
        // Thread lắng nghe log session

        new Thread(() -> {
            try (Jedis jedis = RedisManager.getResource()) {
                if (jedis == null) {
                    System.err.println("[SESSION] Không kết nối được Redis để subscribe.");
                    return;
                }
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        System.out.println("[SESSION] " + message);
                    }
                }, SessionManager.CH_EVENTS);
            } catch (Exception e) {
                System.err.println("[SESSION] Subscriber error: " + e.getMessage());
            }
        }, "session-subscriber").start();

        try (ServerSocket serverSocket = new ServerSocket(2208)) {
            System.out.println("Server started on port 2208...");
            System.out.println("Đợi client kết nối...\n");

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> {
                    try {
                        System.out.println("IP client: " + client.getInetAddress());
                        UserController controller = new UserController(client);
                        controller.handleClient();
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