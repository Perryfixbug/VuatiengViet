package vuatiengvietpj.controller;

import java.io.*;
import java.net.*;

public abstract class ServerController {
    protected Socket clientSocket;
    protected DataInputStream in;
    protected DataOutputStream out;

    public ServerController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new DataInputStream(clientSocket.getInputStream());
        this.out = new DataOutputStream(clientSocket.getOutputStream());
    }

    // khung xử lý chung
    public final void handleClient() {
        try {
            System.out.println("Client kết nối từ: " + clientSocket.getInetAddress());

            // xử lý riêng từng controller
            process();

        } catch (Exception e) {
            System.err.println("Lỗi xử lý client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
            System.out.println("Đóng kết nối.\n");
        }
    }

    // abstract: controller con xử lý gói byte
    protected abstract void process() throws IOException;

    protected void sendBytes(byte[] msg) throws IOException {
        out.writeInt(msg.length);
        out.write(msg);
        out.flush();
    }
}
