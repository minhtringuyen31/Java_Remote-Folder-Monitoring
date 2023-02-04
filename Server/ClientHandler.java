package Server;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    String userDeviceName;
    private Boolean isConnection;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        try {
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        this.userDeviceName = this.is.readUTF();
        this.isConnection = true;
    }

    public void receivedDirectoryTree() throws IOException, ClassNotFoundException {
        // ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        // dirTree =
    }

    @Override
    public void run() {
        while (isConnection) {
            try {
                String message = null;
                message = is.readUTF();

                if (message.equals("DISCONNECT")) {
                    if (this.socket != null) {
                        try {
                            isConnection = false;
                            this.socket.close();
                            this.socket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }

            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

}
