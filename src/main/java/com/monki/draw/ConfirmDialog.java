package com.monki.draw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConfirmDialog extends JDialog {

    private JLabel warning;
    private JButton yesButton;
    private JButton noButton;
    private boolean result = false;

    public ConfirmDialog(String text, ActionListener yesAction) {
        initDialog(text, yesAction);
    }

    private void initDialog(String text, ActionListener yesAction) {
        setContentPane(new BackgroundPanel("/img/img_button.png"));
        setBounds(500, 500, 700, 150);
        setTitle("确认");
        setModal(true);
        setLayout(new BorderLayout());

        // 创建消息面板
        JPanel messagePanel = new JPanel();
        messagePanel.setOpaque(false);
        warning = new JLabel(text);
        warning.setForeground(new Color(26, 180, 209));
        warning.setHorizontalAlignment(JLabel.CENTER);
        warning.setFont(new Font("宋体", Font.ITALIC, 30));
        messagePanel.add(warning);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        yesButton = new MyButton("是");
        noButton = new MyButton("否");
        
        // 添加按钮点击事件
        yesButton.addActionListener(e -> {
            result = true;
            if (yesAction != null) {
                yesAction.actionPerformed(e);
            }
            dispose();
        });
        
        noButton.addActionListener(e -> {
            result = false;
            dispose();
        });
        
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        // 添加组件到对话框
        add(messagePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setVisible(true);
    }

    public boolean getResult() {
        return result;
    }
} 