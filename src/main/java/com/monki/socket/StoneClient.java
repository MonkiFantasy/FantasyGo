package com.monki.socket;

import com.monki.draw.MyFrame;
import com.monki.draw.MyPanel;
import com.monki.entity.Stone;
import com.monki.util.Config;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class StoneClient implements Runnable{
    public static volatile Stone currentStone;
    public static Boolean isCurrentTurn;
    private int port;
    private String ip;
    public StoneClient(String ip, int port){
        this.ip=ip;
        this.port=port;
    }

    private static Stone receiveStone(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Stone receivedStone = (Stone) ois.readObject();
        System.out.println("从服务器接收到的棋子: " + receivedStone);
        return receivedStone;
    }

    private static void sendStone(ObjectOutputStream oos,Stone stone) throws IOException {
        oos.writeObject(stone); // 发送Stone对象到服务器
        oos.flush(); // 确保对象被发送
        System.out.println("发送棋子成功"+stone);
    }

    @Override
    public void run() {
        String host = ip; // 服务器地址
         // 服务器监听端口
        //Stone test = new Stone(0, Color.BLACK, new Position(0, 0), new Position(0, 0));

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            //客户端，默认为白方
            while (true) {
                //接收棋子
                Stone stone = receiveStone(ois);
                //System.out.println("收到"+stone);
                //break;
                currentStone = stone;
                MyPanel.updateStone(stone);
                MyFrame.myPanel.repaint();


                //发送棋子
                //TODO:在这里要改变currentStone
                // 的值，虽然点击时已经被改变，但是下次循环执行时又被还原成收到的棋子

                while (currentStone.getColor().equals(Color.BLACK)){
                    //wait();
                }
                sendStone(oos,currentStone);
            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}