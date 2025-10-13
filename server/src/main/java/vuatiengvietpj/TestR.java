package vuatiengvietpj;

import vuatiengvietpj.controller.RoomController;
import vuatiengvietpj.model.Room;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestR {
    public static void main(String[] args) {
        try {
            // Khởi tạo RoomController với kết nối Socket hợp lệ
            InetAddress localhost = InetAddress.getByName("localhost");
            int mockPort = 2206; // Cổng giả lập, cần đảm bảo server đang chạy trên cổng này
            Socket mockSocket = new Socket(localhost, mockPort);

            // Đảm bảo module và action có độ dài chính xác 8 byte
            String module = String.format("%-8s", "ROOM"); // 8 byte
            String action = String.format("%-8s", "GETALL"); // 8 byte

            // Payload
            String payload = ""; // Payload
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            // Tổng độ dài gói
            int totalLength = 16 + payloadBytes.length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(totalLength); // Độ dài tổng gói
            System.err.println("Total Length: " + totalLength);
            dos.write(module.getBytes(StandardCharsets.UTF_8)); // Module
            dos.write(action.getBytes(StandardCharsets.UTF_8)); // Action
            dos.write(payloadBytes); // Payload

            dos.flush();
            byte[] mockPayload = baos.toByteArray();

            // Gửi dữ liệu đến server
            mockSocket.getOutputStream().write(mockPayload);
            mockSocket.getOutputStream().flush();
            System.out.println("Data sent to server successfully.");

            // Đợi phản hồi từ server
            DataInputStream in = new DataInputStream(mockSocket.getInputStream());
            try {
                int responseLength = in.readInt();
                byte[] response = new byte[responseLength];
                in.readFully(response);
                System.out.println("Response from server: " + new String(response, StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("Error reading response from server: " + e.getMessage());
            }

            // Đóng kết nối sau khi hoàn tất
            mockSocket.close();
            System.out.println("Connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}