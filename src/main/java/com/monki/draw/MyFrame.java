package com.monki.draw;

import javax.swing.*;
import java.awt.*;
import com.monki.draw.MyPanel;
import com.monki.draw.StartPanel;
import com.monki.draw.ConnectPanel;
import com.monki.draw.ConnectDialog;
import com.monki.util.Config;

public class MyFrame extends JFrame {

    public static JPanel myPanel;
    public static JPanel startPanel;
    public static ConnectPanel connectPanel;
    public static JPanel connectDialog;
    
    // 存储适合屏幕的棋盘面板尺寸
    private static Dimension gamePanelSize;

    public MyFrame(String title){
        initFrame(title);
    }

    private void initFrame(String title) {
        // 计算适合屏幕的尺寸
        calculateScreenAdaptiveSizes();
        
        // 设置窗口标题和初始大小
        setTitle(title);
        setBounds(0, 0, 500, 500);
        
        // 设置窗口关闭行为为退出应用程序
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 使用null布局，便于对组件进行绝对定位
        setLayout(null);
        
        // 创建各个面板
        myPanel = new MyPanel(this);
        myPanel.setBounds(0, 0, gamePanelSize.width, gamePanelSize.height); // 棋盘面板使用适合屏幕的尺寸
        
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
        
        // 居中显示窗口
        setLocationRelativeTo(null);
        
        // 显示窗口
        setVisible(true);
    }
    
    /**
     * 计算适合当前屏幕的尺寸
     */
    private void calculateScreenAdaptiveSizes() {
        // 获取屏幕尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        // 获取棋盘所需的最小宽度 = 棋盘左边距 + 棋盘宽度 + 右侧按钮区域
        // 棋盘右侧需要预留SPACE*16的空间给按钮和信息面板
        int minRequiredWidth = Config.X + Config.LENGTH + Config.SPACE * 16;
        
        // 计算高度，考虑到上下边距
        int minRequiredHeight = Config.Y * 2 + Config.LENGTH;
        
        // 计算棋盘面板尺寸，使其合理占据屏幕空间
        // 但不小于最小需求宽度
        int width = (int)(screenSize.width * 0.85);
        width = Math.max(width, minRequiredWidth);
        
        int height = (int)(screenSize.height * 0.85);
        height = Math.max(height, minRequiredHeight);
        
        // 存储计算好的尺寸
        gamePanelSize = new Dimension(width, height);
        
        System.out.println("屏幕尺寸: " + screenSize.width + "x" + screenSize.height);
        System.out.println("棋盘面板尺寸: " + width + "x" + height);
        System.out.println("最小需求宽度: " + minRequiredWidth);
    }
    
    /**
     * 获取适合屏幕的棋盘面板尺寸
     */
    public static Dimension getGamePanelSize() {
        return gamePanelSize;
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
        this.setBounds(0, 0, width, height);
        
        // 确保新面板大小与窗口一致
        newPanel.setBounds(0, 0, width, height);
        
        // 添加新面板
        this.add(newPanel);
        
        // 居中显示窗口
        this.setLocationRelativeTo(null);
        
        // 重新验证组件层次结构
        this.validate();
        
        // 重绘整个窗口
        this.repaint();
    }
}

