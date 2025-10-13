package vuatiengvietpj;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import vuatiengvietpj.controller.ServerController;
import vuatiengvietpj.controller.GameController;
import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.controller.DictionaryController;
import vuatiengvietpj.controller.RoomController;


public class ServerApp {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(2206)) {
            System.out.println("Server started on port 2206...");

            while (true) {
                Socket client = serverSocket.accept();
                DataInputStream in = new DataInputStream(client.getInputStream());

                // đọc tổng gói
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);

                // tách header
                String module = new String(data, 0, 8, StandardCharsets.UTF_8).trim();
                String action = new String(data, 8, 8, StandardCharsets.UTF_8).trim();

                // phần còn lại là payload
                byte[] remaining = new byte[length - 16];
                System.arraycopy(data, 16, remaining, 0, remaining.length);

                System.out.printf("Nhận kết nối từ module: %s | action: %s\n", module, action);

                ServerController controller;

                // --- mapping module cứng ---
                switch (module.toUpperCase()) {
                    case "USER" -> controller = new UserController(client, action, remaining);
                    case "GAME" -> controller = new GameController(client, action, remaining);
                    case "DICT" -> controller = new DictionaryController(client, action, remaining);
                    case "ROOM" -> controller = new RoomController(client, action, remaining);
                    default -> {
                        System.err.println("Module không hợp lệ: " + module);
                        client.close();
                        continue;
                    }
                }

                controller.handleClient();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
