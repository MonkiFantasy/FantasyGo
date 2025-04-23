package com.monki.test;

import com.monki.entity.Position;
import com.monki.entity.Stone;

import java.awt.*;
import java.io.*;
import java.net.Socket;

public class GoClient {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public void connectToServer(String host, int port) throws IOException {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public void sendMove(Stone move) throws IOException {
        oos.writeObject(move);
        oos.flush();
    }

    public Stone receiveMove() throws IOException, ClassNotFoundException {
        return (Stone) ois.readObject();
    }

    // 关闭连接
    public void close() throws IOException {
        oos.close();
        ois.close();
        socket.close();
    }

    public static void main(String[] args) {
        GoClient client = new GoClient();
        try {
            client.connectToServer("localhost", 12345);

            // 假设这里有一个UI循环，用于发送和接收棋子
            // 例如：
            // while (gameNotOver) {
            //     Stone myMove = getMyMoveFromUI();
            //     client.sendMove(myMove);
            //     Stone opponentsMove = client.receiveMove();
            //     updateUIWithOpponentsMove(opponentsMove);
            // }

            // 示例：发送一个假定的棋子
            Stone move = new Stone(1, Color.BLACK, new Position(3, 4), new Position(3, 4));
            client.sendMove(move);
            Stone opponentsMove = client.receiveMove();
            System.out.println("Received opponent's move: " + opponentsMove);

            client.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
