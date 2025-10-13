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
        this.out = new ObjectOutputStream(clientSocket.getOutputStream()); // Out first
        this.in = new ObjectInputStream(clientSocket.getInputStream()); // In second
    }

    // Common handling framework
    public final void handleClient() {
        try {
            System.out.println("Client connected from: " + clientSocket.getInetAddress());

            // ✅ Receive Request from client
            Request request = receiveRequest();

            if (request != null) {
                System.out.printf("Processing request: %s - %s\n",
                        request.getModunle(), request.getMaLenh());

                // ✅ Process and create Response
                Response response = process(request);

                // ✅ Send Response to client
                sendResponse(response);
            }

        } catch (java.net.SocketException e) {
            System.err.println("[ERROR] Connection reset by client: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace(); // Added debug to print detailed stack trace

            // ✅ Send error response if there is an issue
            try {
                Response errorResponse = new Response("ERROR", "SYSTEM",
                        "Server error: " + e.getMessage(), false);
                sendResponse(errorResponse);
            } catch (IOException ignored) {
                System.err.println("Unable to send error response: " + ignored.getMessage());
            }

        } finally {
            closeConnection();
        }
    }

    // abstract: child controllers handle byte packets
    protected abstract Response process(Request request) throws IOException;

    protected Request receiveRequest() throws IOException, ClassNotFoundException {
        try {
            return (Request) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving request: " + e.getMessage());
            throw e;
        }
    }

    protected void sendResponse(Response response) throws IOException {
        try {
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
            throw e;
        }
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
            System.out.println("Closed client connection.\n");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}