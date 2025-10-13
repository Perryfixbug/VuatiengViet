package vuatiengvietpj.controller;

import java.io.*;
import java.net.Socket;

import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;

public abstract class ClientController {
    protected Socket socket;
    protected ObjectInputStream in;
    protected ObjectOutputStream out;

    public ClientController(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    protected void sendRequest(Request request) throws IOException {
        out.writeObject(request);
        out.flush();
    }

    protected Response receiveResponse() throws IOException, ClassNotFoundException {
        return (Response) in.readObject();
    }

    protected Response sendAndReceive(String module, String command, String data)
            throws IOException, ClassNotFoundException {
        Request request = new Request(module, command, data);
        sendRequest(request);
        return receiveResponse();
    }

    protected void close() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }
}
