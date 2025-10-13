package vuatiengvietpj;

import java.net.ServerSocket;
import java.net.Socket;
import vuatiengvietpj.controller.RoomController;

public class ServerApp {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2206)) {
            System.out.println("🚀 Server started on port 2206...");
            System.out.println("Waiting for client connections...\n");

            while (true) {
                Socket client = serverSocket.accept();

                // ✅ Create a thread to handle each client
                new Thread(() -> {
                    try {
                        System.out.println("📞 Client connected from: " + client.getInetAddress());

                        // ✅ Create UserController - it will automatically handle Request/Response
                        RoomController controller = new RoomController(client);
                        
                        controller.handleClient();

                    } catch (Exception e) {
                        System.err.println("❌ Error creating controller: " + e.getMessage());
                        e.printStackTrace(); // Added detailed logging for debugging
                        try {
                            client.close();
                        } catch (Exception ignored) {
                            System.err.println("Failed to close client socket: " + ignored.getMessage());
                        }
                    }
                }).start();
            }

        } catch (Exception e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace(); // Added detailed logging for debugging
        }
    }
}