package com.monki.draw;

import com.monki.socket.GoClient;
import com.monki.util.Config;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

import static com.monki.draw.MyFrame.connectDialog;

public class ConnectPanel extends JPanel {

    private JButton createRoomBtn;
    private JButton joinRoomBtn;
    private JButton backHomeBtn;
    private JFrame parentFrame;
    private GoClient goClient;

    public ConnectPanel(JFrame myframe) {
        this.parentFrame = myframe;
        initPanel();
        initListener();
        
        goClient = new GoClient();
        new Thread(goClient).start();
    }

    private void initListener() {
        createRoomBtn.addActionListener(e -> {
            Config.SERVER = true;

            goClient.createRoom();
            
            Dimension gamePanelSize = MyFrame.getGamePanelSize();
            ((MyFrame)parentFrame).switchPanel(MyFrame.connectPanel, MyFrame.myPanel, 
                                               gamePanelSize.width, gamePanelSize.height);
        });
        
        joinRoomBtn.addActionListener(e -> {
            Config.SERVER = false;

            String roomId = JOptionPane.showInputDialog(this, "请输入房间ID:", "加入房间", JOptionPane.QUESTION_MESSAGE);
            
            if (roomId != null && !roomId.trim().isEmpty()) {
                goClient.joinRoom(roomId);
                
                Dimension gamePanelSize = MyFrame.getGamePanelSize();
                ((MyFrame)parentFrame).switchPanel(MyFrame.connectPanel, MyFrame.myPanel, 
                                                   gamePanelSize.width, gamePanelSize.height);
            }
        });
        
        backHomeBtn.addActionListener(e -> {
            ((MyFrame)parentFrame).switchPanel(MyFrame.connectPanel, MyFrame.startPanel, 500, 500);
        });
    }

    private void initPanel() {
        setLayout(null);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setBounds(0, 30, 500, 50);
        JLabel titleLabel = new JLabel("奇弈围棋 - 网络对弈");
        titleLabel.setForeground(new Color(26, 180, 209));
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        titlePanel.add(titleLabel);
        
        createRoomBtn = createStyledButton("创建房间", new Color(70, 130, 180), 18);
        createRoomBtn.setBounds(150, 100, 200, 50);
        
        joinRoomBtn = createStyledButton("加入房间", new Color(60, 179, 113), 18);
        joinRoomBtn.setBounds(150, 200, 200, 50);
        
        JLabel descLabel1 = createInfoLabel("创建房间: 系统将为您创建一个房间并生成房间ID");
        descLabel1.setBounds(50, 270, 400, 20);
        
        JLabel descLabel2 = createInfoLabel("加入房间: 您需要输入房主的房间ID来连接");
        descLabel2.setBounds(50, 300, 400, 20);
        
        backHomeBtn = new MyButton("返回首页");
        backHomeBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        backHomeBtn.setBounds(20, 400, 120, 40);
        
        add(titlePanel);
        add(createRoomBtn);
        add(joinRoomBtn);
        add(descLabel1);
        add(descLabel2);
        add(backHomeBtn);
    }
    
    private JButton createStyledButton(String text, Color bgColor, int fontSize) {
        MyButton button = new MyButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, fontSize));
        button.setForeground(Color.WHITE);
        return button;
    }
    
    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("微软雅黑", Font.ITALIC, 14));
        return label;
    }

    @Override
    protected void paintComponent(Graphics gh) {
        Graphics2D g = (Graphics2D) gh;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        try {
            Image image = ImageIO.read(getClass().getResource("/img/img_1.png"));
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        } catch (IOException e) {
            GradientPaint gradient = new GradientPaint(0, 0, new Color(50, 50, 80), 
                getWidth(), getHeight(), new Color(120, 120, 180));
            g.setPaint(gradient);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public GoClient getGoClient() {
        return goClient;
    }
}
