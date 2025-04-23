package com.monki.draw;

import com.monki.util.Config;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class StartPanel extends JPanel {

    private JButton offlineButton;
    private JButton onlineButton;
    private JFrame parentFrame;

    public StartPanel(JFrame myframe) {
        this.parentFrame = myframe;
        initPanel();
        initListener();
    }

    private void initListener() {
        offlineButton.addActionListener(e -> {
            Config.MODE = 0;
            // 使用switchPanel方法切换到游戏面板
            ((MyFrame)parentFrame).switchPanel(MyFrame.startPanel, MyFrame.myPanel, 1880, 950);
        });
        
        onlineButton.addActionListener(e -> {
            Config.MODE = 1;
            // 使用switchPanel方法切换到连接面板
            ((MyFrame)parentFrame).switchPanel(MyFrame.startPanel, MyFrame.connectPanel, 500, 500);
        });
    }

    private void initPanel() {
        setLayout(null);
        
        // 创建标题面板
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setBounds(0, 30, 500, 60);
        JLabel titleLabel = new JLabel("奇弈围棋");
        titleLabel.setForeground(new Color(26, 180, 209));
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        titlePanel.add(titleLabel);
        
        // 单机模式按钮
        offlineButton = createStyledButton("单机模式", new Color(70, 130, 180), 18);
        offlineButton.setBounds(150, 100, 200, 50);
        
        // 联机模式按钮
        onlineButton = createStyledButton("联机模式", new Color(60, 179, 113), 18);
        onlineButton.setBounds(150, 200, 200, 50);
        
        // 添加到主面板
        add(titlePanel);
        add(offlineButton);
        add(onlineButton);
        
        // 添加版权信息
        JLabel versionLabel = new JLabel("© 2023 奇弈围棋 v1.0");
        versionLabel.setForeground(new Color(180, 180, 180));
        versionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        versionLabel.setBounds(190, 400, 150, 20);
        add(versionLabel);
    }
    
    // 创建样式化按钮的辅助方法
    private JButton createStyledButton(String text, Color bgColor, int fontSize) {
        MyButton button = new MyButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, fontSize));
        button.setForeground(Color.WHITE);
        return button;
    }

    @Override
    protected void paintComponent(Graphics gh) {
        Graphics2D g = (Graphics2D) gh;
        // 平滑渲染
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 背景图片
        try {
            Image image = ImageIO.read(getClass().getResource("/img/img_1.png"));
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        } catch (IOException e) {
            // 使用渐变背景作为备选
            GradientPaint gradient = new GradientPaint(0, 0, new Color(50, 50, 80), 
                getWidth(), getHeight(), new Color(120, 120, 180));
            g.setPaint(gradient);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // 添加半透明覆盖以增强文本可读性
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
