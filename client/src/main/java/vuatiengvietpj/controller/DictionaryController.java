package vuatiengvietpj.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DictionaryController extends ClientController {
    public DictionaryController(String host, int port) throws IOException {
        super(host, port);
    }

    public void lookupWord(String word) throws IOException {
        // 16 byte đầu: module + action
        byte[] moduleAction = new byte[16];
        byte[] mod = "DICT;LOOKUP".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(mod, 0, moduleAction, 0, mod.length);

        byte[] body = word.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(16 + body.length);
        buffer.put(moduleAction);
        buffer.put(body);

        sendBytes(buffer.array());
        System.out.println("Đã gửi yêu cầu tra từ: " + word);

        // nhận phản hồi
        byte[] response = receiveBytes();
        String meaning = new String(response, StandardCharsets.UTF_8);
        System.out.println("Nghĩa từ: " + meaning);
    }
}