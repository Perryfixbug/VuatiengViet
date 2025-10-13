package vuatiengvietpj;

import java.net.ServerSocket;
import java.net.Socket;
import vuatiengvietpj.controller.UserController;

public class ServerApp {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2208)) {
            System.out.println("🚀 Server started on port 2208...");
            System.out.println("Đợi client kết nối...\n");

            while (true) {
                Socket client = serverSocket.accept();

                // ✅ Tạo thread xử lý từng client
                new Thread(() -> {
                    try {
                        System.out.println("📞 Client kết nối từ: " + client.getInetAddress());

                        // ✅ Tạo UserController - nó sẽ tự động xử lý Request/Response
                        UserController controller = new UserController(client);
                        controller.handleClient();

                    } catch (Exception e) {
                        System.err.println("❌ Lỗi tạo controller: " + e.getMessage());
                        try {
                            client.close();
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}