package vuatiengvietpj;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class DictionaryClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 2206);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        byte[] header = new byte[16];
        byte[] moduleBytes = String.format("%-8s", "DICT").getBytes(StandardCharsets.UTF_8);
        byte[] actionBytes = String.format("%-8s", "LOOKUP").getBytes(StandardCharsets.UTF_8);
        System.arraycopy(moduleBytes, 0, header, 0, 8);
        System.arraycopy(actionBytes, 0, header, 8, 8);


        String payload = "hello world this is a test example";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        byte[] packet = new byte[16 + payloadBytes.length];
        System.arraycopy(header, 0, packet, 0, 16);
        System.arraycopy(payloadBytes, 0, packet, 16, payloadBytes.length);

        System.out.println("Packet length: " + packet.length);
        System.out.println("Sending packet: " + new String(packet, StandardCharsets.UTF_8));
        // gửi gói
        out.writeInt(packet.length);
        out.write(packet);
        out.flush();

        // nhận phản hồi
        int len = in.readInt();
        byte[] resp = new byte[len];
        in.readFully(resp);
        System.out.println("Server trả về: " + new String(resp, StandardCharsets.UTF_8));

        socket.close();
    }
}
