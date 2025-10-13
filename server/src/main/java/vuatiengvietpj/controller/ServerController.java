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
            Request request = receiveRequest();
            if (request == null)
                return; // client đóng trước khi gửi gì -> thoát êm

            System.out.printf("Xu ly request: %s - %s\n",
                    request.getModunle(), request.getMaLenh());

            Response response = process(request);
            sendResponse(response); // gửi đúng 1 lần
            out.flush();
        } catch (java.io.EOFException | java.net.SocketException closed) {
            // client đóng kết nối trong/ sau khi nhận response -> coi như bình thường
        } catch (Exception e) {
            System.err.println("Loi xu ly client: " + e.getMessage());
            // Không cố gửi error response vì socket có thể đã đóng
        } finally {
            closeConnection();
        }
    }

    // abstract: controller con xử lý gói byte
    protected abstract Response process(Request request) throws IOException;

    protected Request receiveRequest() {
        try {
            return (Request) in.readObject();
        } catch (java.io.EOFException | java.net.SocketException closed) {
            return null; // client đóng kết nối
        } catch (Exception e) {
            System.err.println("Loi nhan request: " + e.getMessage());
            return null;
        }
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
            System.out.println("Dong ket noi client.\n");
        } catch (IOException ignored) {
        }
    }
}