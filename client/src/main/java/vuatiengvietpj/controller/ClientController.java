package vuatiengvietpj.controller;

import java.io.*;
import java.net.Socket;

import vuatiengvietpj.model.Request;
import vuatiengvietpj.model.Response;

public abstract class ClientController {
    protected Socket socket;
    protected ObjectInputStream in;
    protected ObjectOutputStream out;
    private final String host;
    private final int port;

    public ClientController(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connectNewSocket();
    }

    private void connectNewSocket() throws IOException {
        // close any previous resources
        try {
            if (this.socket != null && !this.socket.isClosed())
                this.socket.close();
        } catch (Exception ignored) {
        }
        System.out.println("ClientController.connectNewSocket: connecting to " + host + ":" + port);
        socket = new Socket(host, port);
        // enable keep-alive to help with intermediate NAT/firewall cases
        try {
            socket.setKeepAlive(true);
            // set a read timeout so reads won't block forever (milliseconds)
            socket.setSoTimeout(30000);
        } catch (Exception ignored) {
        }
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private synchronized void reconnect() throws IOException {
        // reconnect only when needed
        connectNewSocket();
    }

    protected void sendRequest(Request request) throws IOException {
        // If socket is not usable, try to reconnect once before sending
        try {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                reconnect();
            }
        } catch (IOException e) {
            // failed to reconnect
            close();
            throw e;
        }

        try {
            out.writeObject(request);
            out.flush();
        } catch (java.net.SocketException se) {
            // Try one reconnect attempt on socket-level failure
            try {
                reconnect();
                out.writeObject(request);
                out.flush();
            } catch (IOException retryEx) {
                close();
                throw retryEx;
            }
        } catch (IOException e) {
            // Ensure resources are closed on other IO failures
            try {
                close();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    protected Response receiveResponse() throws IOException, ClassNotFoundException {
        try {
            return (Response) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Close on read failure as well
            try {
                close();
            } catch (Exception ignored) {
            }
            throw e;
        }
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
            out = null;
            in = null;
            socket = null;
        } catch (IOException ignored) {
        }
    }
}