package vuatiengvietpj.controller;

import java.net.*;


public class UserController extends ServerController {
    private String action;
    private byte[] payload;

    public UserController(Socket clientSocket, String action, byte[] payload) throws java.io.IOException {
        super(clientSocket);
        this.action = action;
        this.payload = payload;
    }

    @Override
    protected void process() throws java.io.IOException {
        // String[] parts = request.split(";");
        // String action = parts[1];

        switch (action) {
            // case "login" -> handleLogin(parts);
            // case "signup" -> handleSignup(parts);
            // default -> sendLine("USER;error;Hành động không hợp lệ.");
        }
    }
}
