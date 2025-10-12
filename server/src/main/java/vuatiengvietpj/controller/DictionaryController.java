package vuatiengvietpj.controller;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class DictionaryController extends ServerController {
    private String action;
    private byte[] payload;

    public DictionaryController(Socket clientSocket, String action, byte[] payload) throws java.io.IOException {
        super(clientSocket);
        this.action = action;
        this.payload = payload;
    }

    @Override
    protected void process() throws IOException {
        
        String wordList = new String(payload, StandardCharsets.UTF_8);

        System.out.println("Nhận yêu cầu Dictionary: " + action + " | payload: " + wordList);

        if ("LOOKUP".equalsIgnoreCase(action)) {
            String[] words = wordList.split("\\s+");

            // ví dụ sắp xếp theo độ dài
            java.util.Arrays.sort(words, (a, b) -> a.length() - b.length());

            String response = String.join(", ", words);
            sendBytes(response.getBytes(StandardCharsets.UTF_8));
        } else {
            sendBytes("Dictionary: action không hợp lệ".getBytes(StandardCharsets.UTF_8));
        }
    }

}
