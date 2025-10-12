package vuatiengvietpj.controller;

import java.io.*;
import java.net.Socket;

public abstract class ClientController {
    protected Socket socket;
    protected DataInputStream in;
    protected DataOutputStream out;

    public ClientController(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    protected void sendBytes(byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    protected byte[] receiveBytes() throws IOException {
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    protected void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
