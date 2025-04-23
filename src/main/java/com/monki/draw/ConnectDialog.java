package com.monki.draw;

import com.monki.util.MyLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ConnectDialog extends JPanel {

    private JButton bt_connect;
    private JTextField tf_ip;
    private JTextField tf_port;
    private JFrame parentFrame;
    public static String ip;
    public static int port;

    public ConnectDialog(JFrame myFrame) {
        this.parentFrame = myFrame;
        initDialog();
        initListener(myFrame);
    }

    private void initListener(JFrame myFrame) {
        bt_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MyLogger.log("按钮被点击", this.getClass());
                try {
                    String textIp = tf_ip.getText();
                    if (textIp.isEmpty()) {
                        JOptionPane.showMessageDialog(ConnectDialog.this, "请输入有效的IP地址", "输入错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    String portText = tf_port.getText();
                    if (portText.isEmpty()) {
                        JOptionPane.showMessageDialog(ConnectDialog.this, "请输入有效的端口号", "输入错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    int textPort = Integer.parseInt(portText);
                    if (textPort <= 0 || textPort > 65535) {
                        JOptionPane.showMessageDialog(ConnectDialog.this, "端口号必须在1-65535之间", "输入错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    ip = textIp;
                    port = textPort;
                    System.out.println("ip:" + ip + " port:" + port);
                    ((MyFrame)parentFrame).switchPanel(MyFrame.connectDialog, MyFrame.connectPanel, 500, 500);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ConnectDialog.this, "端口号必须是一个有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void initDialog() {
        setLayout(new BorderLayout());
        // 不再手动设置大小，让面板适应父容器大小
        // setBounds(0, 0, 500, 500);
        
        // 创建标题面板
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        JLabel titleLabel = new JLabel("奇弈围棋 - 网络连接");
        titleLabel.setForeground(new Color(26, 180, 209));
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        
        // 创建输入面板
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        
        // IP地址标签
        JLabel lb_ip = new JLabel("IP地址:");
        lb_ip.setForeground(new Color(26, 180, 209));
        lb_ip.setFont(new Font("微软雅黑", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 5, 5, 5);
        inputPanel.add(lb_ip, gbc);
        
        // IP地址输入框
        tf_ip = new JTextField(15);
        tf_ip.setFont(new Font("Consolas", Font.PLAIN, 14));
        tf_ip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(131, 105, 195), 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        tf_ip.setBackground(new Color(240, 240, 240));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(tf_ip, gbc);
        
        // 端口标签
        JLabel lb_port = new JLabel("端口:");
        lb_port.setForeground(new Color(26, 180, 209));
        lb_port.setFont(new Font("微软雅黑", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(lb_port, gbc);
        
        // 端口输入框
        tf_port = new JTextField(15);
        tf_port.setFont(new Font("Consolas", Font.PLAIN, 14));
        tf_port.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(131, 105, 195), 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        tf_port.setBackground(new Color(240, 240, 240));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(tf_port, gbc);
        
        // 连接按钮
        bt_connect = new MyButton("连接服务器");
        bt_connect.setFont(new Font("微软雅黑", Font.BOLD, 16));
        bt_connect.setPreferredSize(new Dimension(150, 40));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 5, 5, 5);
        inputPanel.add(bt_connect, gbc);
        
        // 提示信息
        JLabel tipLabel = new JLabel("提示: 创建房间时，请使用本机IP地址和可用端口");
        tipLabel.setForeground(new Color(200, 200, 200));
        tipLabel.setFont(new Font("微软雅黑", Font.ITALIC, 12));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        inputPanel.add(tipLabel, gbc);
        
        // 返回按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setOpaque(false);
        JButton backButton = new MyButton("返回");
        backButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        backButton.addActionListener(e -> {
            ((MyFrame)parentFrame).switchPanel(MyFrame.connectDialog, MyFrame.connectPanel, 500, 500);
        });
        bottomPanel.add(backButton);
        
        // 将面板添加到主面板
        add(titlePanel, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void paintComponent(Graphics gh) {
        Graphics2D g = (Graphics2D) gh;
        // 平滑渲染
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        try {
            Image image = ImageIO.read(getClass().getResource("/img/img.png"));
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
