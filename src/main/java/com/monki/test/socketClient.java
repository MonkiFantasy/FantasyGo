package com.monki.test;

import com.monki.util.Calculator;
import com.monki.entity.Position;
import com.monki.entity.Stone;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class socketClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 8888);
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        Stone stone = new Stone(1,Color.BLACK, new Position(4,4), Calculator.getCoordinateViaIndex(4,4));
        objectOutputStream.writeObject(stone);
        System.out.println("发送棋子成功"+stone);
        socket.shutdownOutput();

        /*InputStream inputStream = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        Stone stone = (Stone) ois.readObject();
*/
    }
}
