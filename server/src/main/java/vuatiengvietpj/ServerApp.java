package vuatiengvietpj;

import java.net.ServerSocket;
import java.net.Socket;
import vuatiengvietpj.controller.RoomController;

public class ServerApp {
    public static void main(String[] args) {
        // NOTE: Session/Redis subscriber removed for Room-only testing

        try (ServerSocket serverSocket = new ServerSocket(2208)) {
            System.out.println("Server started on port 2208...");
            System.out.println("Đợi client kết nối...\n");

            while (true) {
                Socket client = serverSocket.accept();
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