package vuatiengvietpj;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import vuatiengvietpj.controller.ServerController; // giữ import
import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.controller.GameController;
import vuatiengvietpj.controller.RoomController;
import vuatiengvietpj.controller.DictionaryController;
import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Room;

public class ServerApp {
    // Inner class extends ServerController, override process() để delegate
    static class DelegatingController extends ServerController {
        public DelegatingController(Socket client) throws IOException {
            super(client);
        }

        @Override
        protected Response process(Request request) throws IOException {
            String module = request.getModule();
            System.out.println("Processing module: " + module);
            switch (module.toUpperCase()) {
                case "USER":
                    try {
                        System.out.println("Creating UserController");
                        UserController uc = new UserController(clientSocket);
                        System.out.println("UserController created, calling process");
                        Response resp = uc.process(request);
                        System.out.println("UserController process done: " + resp);
                        return resp;
                    } catch (Exception e) {
                        System.err.println("Error in UserController: " + e.getMessage());
                        e.printStackTrace();
                        return createErrorResponse(module, request.getMaLenh(), "Lỗi server: " + e.getMessage());
                    }
                case "ROOM":
                    try {
                        System.out.println("Creating RoomController");
                        RoomController rc = new RoomController(clientSocket);
                        System.out.println("RoomController created, calling process");
                        Response resp = rc.process(request);
                        System.out.println("RoomController process done: " + resp);
                        return resp;
                    } catch (Exception e) {
                        System.err.println("Error in RoomController: " + e.getMessage());
                        e.printStackTrace();
                        return createErrorResponse(module, request.getMaLenh(), "Lỗi server: " + e.getMessage());
                    }
                case "GAME":
                    // ...existing code...
                case "DICT":
                    // ...existing code...
                default:
                    return createErrorResponse(module, request.getMaLenh(), "Module không hợp lệ");
            }
        }
    }

    public static void main(String[] args) {

        final ConcurrentHashMap<String, Long> lastConnect = new ConcurrentHashMap<>();
        final long MIN_MS_BETWEEN_CONNECT = 500; // 500ms between accepts from same IP

        try (ServerSocket serverSocket = new ServerSocket(2208)) {
            System.out.println("Server started on port 2208...");
            System.out.println("Đợi client kết nối...\n");

            while (true) {
                Socket client = serverSocket.accept();
                String ip = client.getInetAddress().toString();
                // long now = Instant.now().toEpochMilli();
                // Long prev = lastConnect.get(ip);
                // if (prev != null && now - prev < MIN_MS_BETWEEN_CONNECT) {
                // System.out.println("Throttling connection from " + ip + " (" + (now - prev) +
                // "ms since last)");
                // try {
                // client.close();
                // } catch (Exception ignored) {
                // }
                // continue;
                // }
                // lastConnect.put(ip, now);

                new Thread(() -> {
                    try {
                        System.out.println("IP client: " + client.getInetAddress());
                        // Dùng DelegatingController để delegate dựa trên request
                        DelegatingController controller = new DelegatingController(client);
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