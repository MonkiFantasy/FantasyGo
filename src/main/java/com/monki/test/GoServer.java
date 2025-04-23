package com.monki.test;

import com.monki.entity.Stone;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GoServer {
    private static final int PORT = 12345;
    private List<Socket> clients = new ArrayList<>();

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clients.add(clientSocket);
            new ClientHandler(clientSocket, this).start();
        }
    }

    void broadcastMove(Stone move, Socket sender) throws IOException {
        for (Socket client : clients) {
            if (client != sender) {
                try (ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream())) {
                    oos.writeObject(move);
                }
            }
        }
    }

    // 内部类，用于处理每个客户端的连接
    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private GoServer server;

        public ClientHandler(Socket clientSocket, GoServer server) {
            this.clientSocket = clientSocket;
            this.server = server;
        }

        @Override
        public void run() {
            try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                while (true) {
                    Stone move = (Stone) ois.readObject();
                    System.out.println("Received move from client: " + move);
                    server.broadcastMove(move, clientSocket);
                }
            } catch (EOFException e) {
                // Client disconnected
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                server.clients.remove(clientSocket);
            }
        }
    }

    public static void main(String[] args) {
        GoServer server = new GoServer();
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
