package com.monki.draw;

import javax.swing.*;
import java.awt.*;
import com.monki.draw.MyPanel;
import com.monki.draw.StartPanel;
import com.monki.draw.ConnectPanel;
import com.monki.draw.ConnectDialog;

public class MyFrame extends JFrame {


    public static JPanel myPanel;
    public static JPanel startPanel;
    public static ConnectPanel connectPanel;
    public static JPanel connectDialog;

    public MyFrame(String title){
            initFrame(title);
        }

    private void initFrame(String title) {
        // 设置窗口标题和初始大小
        setTitle(title);
        setBounds(710, 290, 500, 500);
        
        // 设置窗口关闭行为为退出应用程序
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 使用null布局，便于对组件进行绝对定位
        setLayout(null);
        
        // 创建各个面板
        myPanel = new MyPanel(this);
        myPanel.setBounds(0, 0, 1880, 950); // 棋盘面板使用较大尺寸
        
        startPanel = new StartPanel(this);
        startPanel.setBounds(0, 0, 500, 500); // 设置为与初始窗口一致的大小
        
        connectPanel = new ConnectPanel(this);
        connectPanel.setBounds(0, 0, 500, 500); // 设置为与初始窗口一致的大小
        
        connectDialog = new ConnectDialog(this);
        connectDialog.setBounds(0, 0, 500, 500); // 设置为与初始窗口一致的大小
        
        // 添加初始面板
        add(startPanel);
        
        // 设置背景颜色
        setBackground(Color.gray);
        
        // 显示窗口
        setVisible(true);
    }
    
    /**
     * 切换面板的工具方法，确保正确移除旧面板、添加新面板并重绘界面
     * @param oldPanel 要移除的面板
     * @param newPanel 要添加的面板
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public void switchPanel(JPanel oldPanel, JPanel newPanel, int width, int height) {
        // 移除旧面板
        if (oldPanel != null) {
            this.remove(oldPanel);
        }
        
        // 调整窗口大小
        this.setBounds(this.getX(), this.getY(), width, height);
        
        // 确保新面板大小与窗口一致
        newPanel.setBounds(0, 0, width, height);
        
        // 添加新面板
        this.add(newPanel);
        
        // 重新验证组件层次结构
        this.validate();
        
        // 重绘整个窗口
        this.repaint();
    }
}

