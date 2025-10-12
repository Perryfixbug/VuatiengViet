package vuatiengvietpj.controller;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class GameController extends ServerController {
  private String action;
  private byte[] remainingData;

    public GameController(Socket clientSocket, String action, byte[] remaining) throws java.io.IOException {
        super(clientSocket);
        this.action = action;
        this.remainingData = remaining;
    }

    @Override
    protected void process() throws java.io.IOException {
        // Xử lý các yêu cầu liên quan đến game ở đây
        sendBytes("GAME;response;Chức năng game chưa được triển khai.".getBytes(StandardCharsets.UTF_8));
    }
}
