package vuatiengvietpj.controller;

import java.io.*;
import java.net.*;

import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Request;

public abstract class ServerController {
    protected Socket clientSocket;
    protected ObjectInputStream in;
    protected ObjectOutputStream out;

    public ServerController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.out = new ObjectOutputStream(clientSocket.getOutputStream()); // Out trước
        this.in = new ObjectInputStream(clientSocket.getInputStream()); // In sau
    }

    // khung xử lý chung
    public final void handleClient() {
        try {
            System.out.println("Client kết nối từ: " + clientSocket.getInetAddress());

            // ✅ Nhận Request từ client
            Request request = receiveRequest();

            if (request != null) {
                System.out.printf("Xử lý request: %s - %s\n",
                        request.getModunle(), request.getMaLenh());

                // ✅ Xử lý và tạo Response
                Response response = process(request);

                // ✅ Gửi Response về client
                sendResponse(response);
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý client: " + e.getMessage());
            e.printStackTrace();

            // ✅ Gửi error response nếu có lỗi
            try {
                Response errorResponse = new Response("ERROR", "SYSTEM",
                        "Lỗi server: " + e.getMessage(), false);
                sendResponse(errorResponse);
            } catch (IOException ignored) {
            }

        } finally {
            closeConnection();
        }
    }

    // abstract: controller con xử lý gói byte
    protected abstract Response process(Request request) throws IOException;

    protected Request receiveRequest() throws IOException, ClassNotFoundException {
        return (Request) in.readObject();
    }

    protected void sendResponse(Response response) throws IOException {
        out.writeObject(response);
        out.flush();
    }

    protected Response createSuccessResponse(String module, String command, String data) {
        return new Response(module, command, data, true);
    }

    protected Response createErrorResponse(String module, String command, String errorMessage) {
        return new Response(module, command, errorMessage, false);
    }

    protected void closeConnection() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (clientSocket != null)
                clientSocket.close();
            System.out.println("Đóng kết nối client.\n");
        } catch (IOException ignored) {
        }
    }
}