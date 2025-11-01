package vuatiengvietpj.controller;

import java.io.*;
import java.net.*;

import vuatiengvietpj.model.Response;
import vuatiengvietpj.model.Request;

public abstract class ServerController {
    protected Socket clientSocket;
    protected ObjectInputStream in;
    protected ObjectOutputStream out;

    public ServerController() {

    }

    public ServerController(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.out = new ObjectOutputStream(clientSocket.getOutputStream()); // Out trước
        this.in = new ObjectInputStream(clientSocket.getInputStream()); // In sau
    }

    // khung xử lý chung
    public final void handleClient(String ip) // chỗ này em chỉnh truyền Ip của client vào để xác minh thiết bị
    {
        try {
            while (true) { // loop để xử lý nhiều request
                Request request = receiveRequest();
                if (request == null) {
                    break; // client đóng kết nối
                }
                System.out.printf("Xu ly requestttttt: %s - %s\n",
                        request.getModule(), request.getMaLenh());
                request.setIp(ip);
                Response response = process(request);
                sendResponse(response);
                out.flush();

                // Nếu là lệnh LOGOUT, thoát loop
                if ("LOGOUT".equals(request.getMaLenh())) {
                    break;
                }
            }
        } catch (java.io.EOFException | java.net.SocketException closed) {
            // client đóng kết nối
        } catch (Exception e) {
            System.err.println("Loi xu ly client: " + e.getMessage());
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
        System.out.println(new Response(module, command, data, true));
        return new Response(module, command, data, true);
    }

    protected Response createErrorResponse(String module, String command, String errorMessage) {
        System.out.println(new Response(module, command, errorMessage, false));
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