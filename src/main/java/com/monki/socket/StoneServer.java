package com.monki.socket;
import com.monki.draw.MyFrame;
import com.monki.draw.MyPanel;
import com.monki.entity.Position;
import com.monki.entity.Stone;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;




public class StoneServer implements Runnable{
    private int port;
    public static volatile Stone currentStone;
    public StoneServer(int listeningPort){
        this.port = listeningPort;
    }

    private static Stone receiveStone(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Stone receivedStone = (Stone) ois.readObject();
        System.out.println("从客户端接收到的棋子: " + receivedStone);
        return receivedStone;
    }
    private static void sendStone(ObjectOutputStream oos,Stone stone) throws IOException {
        oos.writeObject(stone); // 发送Stone对象到服务器
        oos.flush(); // 确保对象被发送
        System.out.println("已发送棋子到客户端"+stone);
    }

    @Override
    public void run() {
        //int port = 12345; // 服务器监听的端口
        Stone test = new Stone(0, Color.BLACK, new Position(0, 0), new Position(0, 0));
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            //等待连接建立
            System.out.println("服务器启动，等待连接...");
            Socket clientSocket = serverSocket.accept(); // 等待客户端连接
            System.out.println("客户端已连接");
            try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {
                //服务器，默认为黑方
                while (true) {
                    if (currentStone==null){
                        continue;
                    }
                    if(currentStone.getColor().equals(Color.WHITE)){
                        continue;
                    }
                    //发送棋子
                    sendStone(oos,currentStone);
                    System.out.println("等待客户端发送棋子...");
                    //接收棋子
                    currentStone = receiveStone(ois);
                    MyPanel.updateStone(currentStone);
                    MyFrame.myPanel.repaint();

                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                clientSocket.close(); // 确保在最后关闭客户端套接字
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}