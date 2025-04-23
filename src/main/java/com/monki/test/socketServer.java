package com.monki.test;

import com.monki.entity.Stone;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class socketServer {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(8888);
        while (true) {
            Socket accept = serverSocket.accept();
            InputStream is = accept.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Stone stone = (Stone) ois.readObject();
            System.out.println(stone);
        }
    }
}
